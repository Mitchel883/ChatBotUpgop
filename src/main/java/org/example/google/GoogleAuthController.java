package org.example.google;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google")
public class GoogleAuthController {

    private final GoogleOAuthService oauth;

    public GoogleAuthController(GoogleOAuthService oauth) {
        this.oauth = oauth;
    }

    /**
     * Paso 1: Redirige al usuario a la página de autorización de Google
     * URL: /google/authorize?phone=5218711234567
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(@RequestParam String phone) {
        System.out.println("🔐 Iniciando flujo OAuth para: " + phone);

        String authUrl = oauth.buildAuthUrl(phone);

        System.out.println("   Redirigiendo a: " + authUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", authUrl);

        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect
    }

    /**
     * Paso 2: Google redirige aquí después de que el usuario autoriza
     * URL: /google/oauth/callback?code=xxx&state=5218711234567
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        // Manejar error de OAuth
        if (error != null) {
            System.err.println("❌ Error en OAuth: " + error);
            return ResponseEntity.status(400)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorPage(error));
        }

        // Validar parámetros
        if (code == null || state == null) {
            System.err.println("❌ Parámetros faltantes en callback");
            return ResponseEntity.status(400)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorPage("Parámetros faltantes"));
        }

        String phone = state;

        try {
            System.out.println("✅ Callback recibido para: " + phone);
            System.out.println("   Code: " + code.substring(0, 20) + "...");

            // Intercambiar código por tokens
            oauth.exchangeCode(phone, code);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildSuccessPage(phone));

        } catch (Exception e) {
            System.err.println("❌ Error en callback OAuth: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorPage(e.getMessage()));
        }
    }

    /**
     * Endpoint de prueba para verificar configuración
     */
    @GetMapping("/test-config")
    public ResponseEntity<String> testConfig() {
        oauth.printConfig();
        return ResponseEntity.ok("Configuración impresa en logs");
    }

    /**
     * Revoca autorización de un usuario
     */
    @PostMapping("/revoke")
    public ResponseEntity<String> revoke(@RequestParam String phone) {
        oauth.revokeToken(phone);
        return ResponseEntity.ok("Autorización revocada para: " + phone);
    }

    // ==================== HTML RESPONSES ====================

    private String buildSuccessPage(String phone) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>Autorización Exitosa</title>" +
                "    <style>" +
                "        body {" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;" +
                "            display: flex;" +
                "            justify-content: center;" +
                "            align-items: center;" +
                "            min-height: 100vh;" +
                "            margin: 0;" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);" +
                "        }" +
                "        .container {" +
                "            background: white;" +
                "            padding: 40px;" +
                "            border-radius: 20px;" +
                "            box-shadow: 0 20px 60px rgba(0,0,0,0.3);" +
                "            text-align: center;" +
                "            max-width: 400px;" +
                "        }" +
                "        .success-icon {" +
                "            font-size: 64px;" +
                "            margin-bottom: 20px;" +
                "        }" +
                "        h1 {" +
                "            color: #10b981;" +
                "            margin: 0 0 10px 0;" +
                "            font-size: 24px;" +
                "        }" +
                "        p {" +
                "            color: #6b7280;" +
                "            line-height: 1.6;" +
                "            margin: 10px 0;" +
                "        }" +
                "        .phone {" +
                "            background: #f3f4f6;" +
                "            padding: 10px 20px;" +
                "            border-radius: 10px;" +
                "            font-family: 'Courier New', monospace;" +
                "            margin: 20px 0;" +
                "            font-weight: bold;" +
                "        }" +
                "        .next-steps {" +
                "            text-align: left;" +
                "            margin-top: 30px;" +
                "            padding: 20px;" +
                "            background: #f9fafb;" +
                "            border-radius: 10px;" +
                "        }" +
                "        .next-steps h3 {" +
                "            margin-top: 0;" +
                "            color: #374151;" +
                "            font-size: 16px;" +
                "        }" +
                "        .next-steps li {" +
                "            margin: 10px 0;" +
                "            color: #6b7280;" +
                "        }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"success-icon\">✅</div>" +
                "        <h1>¡Autorización Exitosa!</h1>" +
                "        <p>Tu cuenta de Google Calendar ha sido conectada correctamente.</p>" +
                "        <div class=\"phone\">" + phone + "</div>" +
                "        <div class=\"next-steps\">" +
                "            <h3>📋 Siguientes pasos:</h3>" +
                "            <ol style=\"padding-left: 20px;\">" +
                "                <li>Puedes cerrar esta ventana</li>" +
                "                <li>Vuelve a WhatsApp</li>" +
                "                <li>Crea tu recordatorio de nuevo</li>" +
                "                <li>Se guardará automáticamente en Google Calendar</li>" +
                "            </ol>" +
                "        </div>" +
                "        <p style=\"margin-top: 30px; font-size: 14px; color: #9ca3af;\">" +
                "            💡 Tus recordatorios ahora se sincronizarán con Google Calendar" +
                "        </p>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    private String buildErrorPage(String errorMessage) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>Error de Autorización</title>" +
                "    <style>" +
                "        body {" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;" +
                "            display: flex;" +
                "            justify-content: center;" +
                "            align-items: center;" +
                "            min-height: 100vh;" +
                "            margin: 0;" +
                "            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);" +
                "        }" +
                "        .container {" +
                "            background: white;" +
                "            padding: 40px;" +
                "            border-radius: 20px;" +
                "            box-shadow: 0 20px 60px rgba(0,0,0,0.3);" +
                "            text-align: center;" +
                "            max-width: 400px;" +
                "        }" +
                "        .error-icon {" +
                "            font-size: 64px;" +
                "            margin-bottom: 20px;" +
                "        }" +
                "        h1 {" +
                "            color: #ef4444;" +
                "            margin: 0 0 10px 0;" +
                "            font-size: 24px;" +
                "        }" +
                "        .error-message {" +
                "            background: #fef2f2;" +
                "            border: 2px solid #fee2e2;" +
                "            color: #991b1b;" +
                "            padding: 15px;" +
                "            border-radius: 10px;" +
                "            margin: 20px 0;" +
                "            font-family: 'Courier New', monospace;" +
                "            font-size: 14px;" +
                "            word-break: break-word;" +
                "        }" +
                "        .help {" +
                "            text-align: left;" +
                "            margin-top: 30px;" +
                "            padding: 20px;" +
                "            background: #f9fafb;" +
                "            border-radius: 10px;" +
                "        }" +
                "        .help h3 {" +
                "            margin-top: 0;" +
                "            color: #374151;" +
                "            font-size: 16px;" +
                "        }" +
                "        .help li {" +
                "            margin: 10px 0;" +
                "            color: #6b7280;" +
                "        }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"error-icon\">❌</div>" +
                "        <h1>Error de Autorización</h1>" +
                "        <p>No se pudo completar la autorización de Google Calendar.</p>" +
                "        <div class=\"error-message\">" + errorMessage + "</div>" +
                "        <div class=\"help\">" +
                "            <h3>💡 Posibles soluciones:</h3>" +
                "            <ol style=\"padding-left: 20px;\">" +
                "                <li>Intenta autorizar de nuevo</li>" +
                "                <li>Verifica que tu email esté en usuarios de prueba</li>" +
                "                <li>Contacta al administrador del bot</li>" +
                "            </ol>" +
                "        </div>" +
                "        <p style=\"margin-top: 30px; font-size: 14px; color: #9ca3af;\">" +
                "            📱 Vuelve a WhatsApp e intenta de nuevo" +
                "        </p>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}