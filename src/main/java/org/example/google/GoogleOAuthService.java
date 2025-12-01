package org.example.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.TokenResponse;
import org.example.firebase.OAuthTokenRepository;
import org.example.model.UserToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Service
public class GoogleOAuthService {

    // ⭐ Memoria caché (se recarga desde Firebase al iniciar)
    private final Map<String, UserToken> tokensCache = new HashMap<>();

    // ⭐ Repositorio para persistencia
    private final OAuthTokenRepository tokenRepository;

    // Configuración desde application.properties
    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private static final List<String> SCOPES = Collections.singletonList(
            "https://www.googleapis.com/auth/calendar.events"
    );

    public GoogleOAuthService(OAuthTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * ⭐ Carga todos los tokens desde Firebase al iniciar la aplicación
     */
    @PostConstruct
    public void loadTokensFromFirebase() {
        System.out.println("🔄 Cargando tokens OAuth desde Firebase...");
        Map<String, UserToken> loadedTokens = tokenRepository.loadAllTokens();
        tokensCache.putAll(loadedTokens);
        System.out.println("✅ " + loadedTokens.size() + " tokens OAuth cargados en caché");
    }

    /**
     * Genera el link de autorización OAuth de Google
     */
    public String generateAuthLink(String phone) {
        String scope = "https://www.googleapis.com/auth/calendar.events";

        try {
            return "https://accounts.google.com/o/oauth2/v2/auth?"
                    + "client_id=" + URLEncoder.encode(clientId, "UTF-8")
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8")
                    + "&response_type=code"
                    + "&scope=" + URLEncoder.encode(scope, "UTF-8")
                    + "&access_type=offline"
                    + "&prompt=consent"
                    + "&state=" + phone;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error generando URL OAuth", e);
        }
    }

    /**
     * Método alternativo usando GoogleAuthorizationCodeFlow
     */
    public String buildAuthUrl(String phone) {
        try {
            GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret);

            GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                    .setInstalled(details);

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientSecrets,
                    SCOPES
            )
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();

            return flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setState(phone)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error construyendo URL OAuth", e);
        }
    }

    /**
     * ⭐ Intercambia el código de autorización por tokens y los GUARDA en Firebase
     */
    public void exchangeCode(String phone, String code) {
        try {
            System.out.println("🔄 Intercambiando código OAuth para: " + phone);
            System.out.println("   Redirect URI usado: " + redirectUri);

            GoogleAuthorizationCodeTokenRequest request =
                    new GoogleAuthorizationCodeTokenRequest(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            GsonFactory.getDefaultInstance(),
                            clientId,
                            clientSecret,
                            code,
                            redirectUri
                    );

            TokenResponse tokenResponse = request.execute();

            UserToken token = new UserToken(
                    phone,
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken()
            );

            // ⭐ Guardar en caché
            tokensCache.put(phone, token);

            // ⭐ Guardar en Firebase (PERSISTENTE)
            tokenRepository.saveToken(phone, token);

            System.out.println("✅ OAuth completado exitosamente para " + phone);
            System.out.println("   Access token: " + tokenResponse.getAccessToken().substring(0, 20) + "...");
            System.out.println("   Refresh token: " + (tokenResponse.getRefreshToken() != null ? "Sí" : "No"));
            System.out.println("   💾 Token guardado en Firebase (permanente)");

        } catch (Exception e) {
            System.err.println("❌ Error intercambiando OAuth code: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error intercambiando OAuth code", e);
        }
    }

    /**
     * ⭐ Obtiene el token de un usuario (primero de caché, si no de Firebase)
     */
    public UserToken getUserToken(String phone) {
        // Intentar desde caché
        UserToken token = tokensCache.get(phone);

        if (token != null) {
            return token;
        }

        // Si no está en caché, intentar desde Firebase
        System.out.println("ℹ️ Token no en caché, cargando desde Firebase: " + phone);
        token = tokenRepository.getToken(phone);

        if (token != null) {
            // Agregar a caché para próximas llamadas
            tokensCache.put(phone, token);
        }

        return token;
    }

    /**
     * Verifica si un usuario tiene token válido
     */
    public boolean isAuthenticated(String phone) {
        UserToken token = getUserToken(phone);
        return token != null;
    }

    /**
     * ⭐ Remueve el token de un usuario (caché y Firebase)
     */
    public void revokeToken(String phone) {
        tokensCache.remove(phone);
        tokenRepository.deleteToken(phone);
        System.out.println("🔓 Token revocado para: " + phone);
    }

    /**
     * Debug: Muestra configuración actual
     */
    public void printConfig() {
        System.out.println("📋 Configuración OAuth:");
        System.out.println("   Client ID: " + clientId);
        System.out.println("   Redirect URI: " + redirectUri);
        System.out.println("   Usuarios en caché: " + tokensCache.size());
        System.out.println("   Usuarios autenticados: " + getAuthenticatedUsersCount());
    }

    /**
     * Obtiene el número de usuarios autenticados
     */
    public int getAuthenticatedUsersCount() {
        return tokensCache.size();
    }

    /**
     * Refresca todos los tokens desde Firebase (útil para debug)
     */
    public void refreshTokensFromFirebase() {
        loadTokensFromFirebase();
    }
}