package org.example.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.example.model.UserToken;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class OAuthTokenRepository {

    private final Firestore firestore;
    private static final String COLLECTION = "oauth_tokens";

    public OAuthTokenRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Guarda o actualiza el token de un usuario en Firebase
     */
    public void saveToken(String phoneNumber, UserToken token) {
        if (firestore == null || phoneNumber == null || token == null) {
            System.out.println("⚠️ No se puede guardar token: Firestore no disponible");
            return;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("phoneNumber", phoneNumber);
            data.put("accessToken", token.getAccessToken());
            data.put("refreshToken", token.getRefreshToken());
            data.put("updatedAt", Timestamp.now());

            // Usar phoneNumber como ID del documento para evitar duplicados
            firestore.collection(COLLECTION)
                    .document(phoneNumber)
                    .set(data)
                    .get();

            System.out.println("✅ Token guardado en Firebase para: " + phoneNumber);

        } catch (Exception e) {
            System.err.println("❌ Error guardando token: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carga el token de un usuario desde Firebase
     */
    public UserToken getToken(String phoneNumber) {
        if (firestore == null || phoneNumber == null) {
            return null;
        }

        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION)
                    .document(phoneNumber)
                    .get()
                    .get();

            if (!doc.exists()) {
                System.out.println("ℹ️ No hay token guardado para: " + phoneNumber);
                return null;
            }

            String accessToken = doc.getString("accessToken");
            String refreshToken = doc.getString("refreshToken");

            if (accessToken == null) {
                System.out.println("⚠️ Token inválido para: " + phoneNumber);
                return null;
            }

            UserToken token = new UserToken(phoneNumber, accessToken, refreshToken);
            System.out.println("✅ Token cargado desde Firebase para: " + phoneNumber);
            return token;

        } catch (Exception e) {
            System.err.println("❌ Error cargando token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Elimina el token de un usuario
     */
    public void deleteToken(String phoneNumber) {
        if (firestore == null || phoneNumber == null) {
            return;
        }

        try {
            firestore.collection(COLLECTION)
                    .document(phoneNumber)
                    .delete()
                    .get();

            System.out.println("🗑️ Token eliminado para: " + phoneNumber);

        } catch (Exception e) {
            System.err.println("❌ Error eliminando token: " + e.getMessage());
        }
    }

    /**
     * Verifica si un usuario tiene token guardado
     */
    public boolean hasToken(String phoneNumber) {
        if (firestore == null || phoneNumber == null) {
            return false;
        }

        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION)
                    .document(phoneNumber)
                    .get()
                    .get();

            return doc.exists();

        } catch (Exception e) {
            System.err.println("❌ Error verificando token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Carga TODOS los tokens al iniciar la aplicación
     * Retorna un mapa: phoneNumber -> UserToken
     */
    public Map<String, UserToken> loadAllTokens() {
        Map<String, UserToken> tokens = new HashMap<>();

        if (firestore == null) {
            return tokens;
        }

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION).get();
            QuerySnapshot snapshot = future.get();

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String phoneNumber = doc.getString("phoneNumber");
                String accessToken = doc.getString("accessToken");
                String refreshToken = doc.getString("refreshToken");

                if (phoneNumber != null && accessToken != null) {
                    UserToken token = new UserToken(phoneNumber, accessToken, refreshToken);
                    tokens.put(phoneNumber, token);
                }
            }

            System.out.println("✅ Cargados " + tokens.size() + " tokens OAuth desde Firebase");

        } catch (Exception e) {
            System.err.println("❌ Error cargando todos los tokens: " + e.getMessage());
        }

        return tokens;
    }
}
