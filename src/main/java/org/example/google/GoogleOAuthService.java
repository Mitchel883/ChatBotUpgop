package org.example.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.TokenResponse;
import org.example.model.UserToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Service
public class GoogleOAuthService {

    private final Map<String, UserToken> tokens = new HashMap<>();

    // ✅ Leer desde application.properties
    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    private static final List<String> SCOPES = Collections.singletonList(
            "https://www.googleapis.com/auth/calendar.events"
    );

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
     * Intercambia el código de autorización por tokens de acceso
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

            tokens.put(phone, token);

            System.out.println("✅ OAuth completado exitosamente para " + phone);
            System.out.println("   Access token: " + tokenResponse.getAccessToken().substring(0, 20) + "...");
            System.out.println("   Refresh token: " + (tokenResponse.getRefreshToken() != null ? "Sí" : "No"));

        } catch (Exception e) {
            System.err.println("❌ Error intercambiando OAuth code: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error intercambiando OAuth code", e);
        }
    }

    /**
     * Obtiene el token de un usuario
     */
    public UserToken getUserToken(String phone) {
        return tokens.get(phone);
    }

    /**
     * Verifica si un usuario tiene token válido
     */
    public boolean isAuthenticated(String phone) {
        return tokens.containsKey(phone);
    }

    /**
     * Remueve el token de un usuario (logout)
     */
    public void revokeToken(String phone) {
        tokens.remove(phone);
        System.out.println("🔓 Token revocado para: " + phone);
    }

    /**
     * Debug: Muestra configuración actual
     */
    public void printConfig() {
        System.out.println("📋 Configuración OAuth:");
        System.out.println("   Client ID: " + clientId);
        System.out.println("   Redirect URI: " + redirectUri);
        System.out.println("   Usuarios autenticados: " + tokens.size());
    }
}