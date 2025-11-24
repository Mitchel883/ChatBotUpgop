package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class CloudApiSender {

    private static final Logger log = LoggerFactory.getLogger(CloudApiSender.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate http = new RestTemplate();

    @Value("${wa.phone_number_id}")
    private String phoneNumberId;

    @Value("${wa.access_token}")
    private String accessToken;

    public void sendText(String toE164, String text) {
        // Validaciones básicas
        if (phoneNumberId == null || phoneNumberId.trim().isEmpty()) {
            throw new IllegalStateException("wa.phone_number_id vacío o nulo");
        }
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("wa.access_token vacío o nulo");
        }

        final String token = accessToken;
        final String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";
        final String to = normalizeMxE164(toE164);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", to);
        body.put("type", "text");
        Map<String, String> textObj = new HashMap<>();
        textObj.put("body", text);
        body.put("text", textObj);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // ===== LOGS DE ENTRADA =====
        log.info("POST {}", url);
        log.info("PhoneNumberId: {}", phoneNumberId);
        log.info("To (E164 normalizado): {}", to);
        log.info("AccessToken: {}", maskToken(token));
        log.info("Request headers: {}", headers);
        log.info("Request body:\n{}", toJson(body));

        long t0 = System.currentTimeMillis();
        try {
            ResponseEntity<String> resp = http.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            long ms = System.currentTimeMillis() - t0;

            // ===== LOGS DE RESPUESTA (200/201/etc) =====
            log.info("Response status: {} ({} ms)", resp.getStatusCodeValue(), ms);
            log.info("Response headers: {}", resp.getHeaders());
            log.info("Response body:\n{}", resp.getBody());
        } catch (HttpStatusCodeException e) {
            long ms = System.currentTimeMillis() - t0;

            // ===== LOGS DE ERROR (4xx/5xx) =====
            log.warn("HTTP {} {} ({} ms)", e.getRawStatusCode(), e.getStatusText(), ms);
            log.warn("Error headers: {}", e.getResponseHeaders());
            String bodyErr = e.getResponseBodyAsString();
            log.warn("Error body:\n{}", (bodyErr == null || bodyErr.isEmpty()) ? "<no body>" : bodyErr);

            // Re-lanzar si quieres que falle la petición hacia arriba
            throw e;
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            log.error("Error inesperado ({} ms): {}", ms, e.toString(), e);
            throw e;
        }
    }

    private static String toJson(Object o) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "<no se pudo serializar a JSON: " + e.getMessage() + ">";
        }
    }

    private static String maskToken(String token) {
        if (token == null) return "<null>";
        String t = token.trim();
        if (t.length() <= 10) return "********";
        return t.substring(0, 6) + "..." + t.substring(t.length() - 4);
        // ejemplo: EAAGxx...9d2f
    }

    private String normalizeMxE164(String raw) {
        String d = raw.replaceAll("\\D", "");
        if (d.startsWith("521") && d.length() == 13) return "52" + d.substring(3);
        if (d.startsWith("52") && d.length() == 12) return d;
        if (d.length() == 10) return "52" + d;
        return d;
    }
}
