package org.example.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.TokenResponse;   // <-- FALTABA ESTO
import org.example.model.UserToken;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class GoogleOAuthService {

    private final Map<String, UserToken> tokens = new HashMap<>();

    private GoogleClientSecrets clientSecrets;

    private static final List<String> SCOPES = Collections.singletonList(
            "https://www.googleapis.com/auth/calendar.events"
    );

    public GoogleOAuthService() throws Exception {
        loadClientSecrets();
    }

    private void loadClientSecrets() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream("credentials_oauth.json");
        if (in == null) {
            throw new IllegalStateException("No se encontró credentials_oauth.json en resources/");
        }

        this.clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(in)
        );
    }

    public String generateAuthLink(String phone) {
        String scope = "https://www.googleapis.com/auth/calendar.events";

        String clientId = clientSecrets.getDetails().getClientId();
        String redirectUri = clientSecrets.getDetails().getRedirectUris().get(0);

        return "https://accounts.google.com/o/oauth2/v2/auth?"
                + "client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + scope
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + phone;
    }

    public String buildAuthUrl(String phone) {
        try {
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
                    .setRedirectUri(clientSecrets.getDetails().getRedirectUris().get(0))
                    .setState(phone)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Error construyendo URL OAuth", e);
        }
    }

    public void exchangeCode(String phone, String code) {
        try {
            GoogleAuthorizationCodeTokenRequest request =
                    new GoogleAuthorizationCodeTokenRequest(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            GsonFactory.getDefaultInstance(),
                            clientSecrets.getDetails().getClientId(),
                            clientSecrets.getDetails().getClientSecret(),
                            code,
                            clientSecrets.getDetails().getRedirectUris().get(0)
                    );

            TokenResponse tokenResponse = request.execute();

            UserToken token = new UserToken(
                    phone,
                    tokenResponse.getAccessToken(),
                    tokenResponse.getRefreshToken()
            );

            tokens.put(phone, token);

            System.out.println("✔ OAuth completado para " + phone);

        } catch (Exception e) {
            throw new RuntimeException("Error intercambiando OAuth code", e);
        }
    }

    public UserToken getUserToken(String phone) {
        return tokens.get(phone);
    }
}
