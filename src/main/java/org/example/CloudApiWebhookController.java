package org.example;

import org.example.CloudApiSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/whatsapp")
public class CloudApiWebhookController {
    @Autowired
    private org.example.bot.ChatBotService chatbot;


    private static final String VERIFY_TOKEN = System.getenv().getOrDefault(
            "WA_VERIFY_TOKEN", "unipoli-verify-token");

    private final CloudApiSender cloudApiSender;

    // Inyección por constructor
    public CloudApiWebhookController(CloudApiSender cloudApiSender) {
        this.cloudApiSender = cloudApiSender;
    }

    // 1) Verificación
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name="hub.mode", required=false) String mode,
            @RequestParam(name="hub.verify_token", required=false) String token,
            @RequestParam(name="hub.challenge", required=false) String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Verification failed");
    }



    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload) {
        if (!"whatsapp_business_account".equals(payload.get("object"))) {
            return ResponseEntity.ok().build();
        }

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> entries =
                (java.util.List<Map<String, Object>>) payload.get("entry");
        if (entries == null) return ResponseEntity.ok().build();

        for (Map<String, Object> e : entries) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> changes =
                    (java.util.List<Map<String, Object>>) e.get("changes");
            if (changes == null) continue;

            for (Map<String, Object> ch : changes) {
                @SuppressWarnings("unchecked")
                Map<String, Object> value = (Map<String, Object>) ch.get("value");
                if (value == null) continue;

                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> messages =
                        (java.util.List<Map<String, Object>>) value.get("messages");
                if (messages == null) continue;

                for (Map<String, Object> m : messages) {
                    String from = (String) m.get("from");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> text = (Map<String, Object>) m.get("text");
                    String body = text != null ? (String) text.get("body") : "";

                    // Llama a tu servicio/bot aquí
                    chatbot.handle(from, body);
                }
            }
        }
        return ResponseEntity.ok().build();
    }


}