package org.example;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Component
public class CloudApiSender {

    private final RestTemplate http = new RestTemplate();

    @org.springframework.beans.factory.annotation.Value("${wa.phone_number_id}")
    private String phoneNumberId;

    @org.springframework.beans.factory.annotation.Value("${wa.access_token}")
    private String accessToken;

    public void sendText(String toE164, String text) {
        String url = "https://graph.facebook.com/v20.0/" + phoneNumberId + "/messages";
        System.out.println("<< POST " + url);
        String to = normalizeMxE164(toE164);
        Map<String,Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");

        body.put("to", to);
        body.put("type", "text");
        Map<String,String> textObj = new HashMap<>();
        textObj.put("body", text);
        body.put("text", textObj);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

            http.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private String normalizeMxE164(String raw) {
        String d = raw.replaceAll("\\D", ""); // solo dígitos
        // Si viene como 521 + 10 dígitos (wa_id del webhook), quita el 1
        if (d.startsWith("521") && d.length() == 13) return "52" + d.substring(3);
        // Si ya viene correcto 52 + 10
        if (d.startsWith("52") && d.length() == 12) return d;
        // Si viene local de 10 dígitos, antepone 52
        if (d.length() == 10) return "52" + d;
        return d; // fallback para otros países o formatos ya válidos
    }

}