package com.unipoli.chatbot.controller;

import com.unipoli.chatbot.controller.WhatsAppService;
import com.unipoli.chatbot.controller.GoogleCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

    @RestController
    @RequestMapping("/webhook")
    public class WebhookController {

        @Autowired
        private WhatsAppService whatsappService;

        @Autowired
        private GoogleCalendarService calendarService;

        @GetMapping
        public String verifyWebhook(@RequestParam("hub.mode") String mode,
                                    @RequestParam("hub.verify_token") String token,
                                    @RequestParam("hub.challenge") String challenge) {
            if ("subscribe".equals(mode) && token.equals(System.getenv("VERIFY_TOKEN"))) {
                return challenge;
            }
            return "Verification failed";
        }

        @PostMapping
        public void receiveMessage(@RequestBody Map<String, Object> payload) {
            try {
                Map<?, ?> entry = (Map<?, ?>) ((java.util.List<?>) payload.get("entry")).get(0);
                Map<?, ?> changes = (Map<?, ?>) ((java.util.List<?>) entry.get("changes")).get(0);
                Map<?, ?> value = (Map<?, ?>) changes.get("value");
                java.util.List<?> messages = (java.util.List<?>) value.get("messages");

                if (messages != null) {
                    Map<?, ?> message = (Map<?, ?>) messages.get(0);
                    String from = message.get("from").toString();
                    String text = ((Map<?, ?>) message.get("text")).get("body").toString().toLowerCase();

                    if (text.contains("crear evento")) {
                        calendarService.createEvent("Reunión con cliente", "2025-10-30T15:00:00Z", "2025-10-30T16:00:00Z");
                        whatsappService.sendMessage(from, "📅 Evento creado en Google Calendar");
                    } else if (text.contains("ver eventos")) {
                        String events = calendarService.listEvents();
                        whatsappService.sendMessage(from, " Próximos eventos:\n" + events);
                    } else if (text.contains("recordatorio")) {
                        whatsappService.sendMessage(from, "Recordatorio activado (simulado)");
                    } else {
                        whatsappService.sendMessage(from, "Hola soy tu asistente UNIPOLI.\nOpciones:\n- Crear evento\n- Ver eventos\n- Recordatorio");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
