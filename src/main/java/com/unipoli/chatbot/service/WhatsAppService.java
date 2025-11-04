package com.unipoli.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

    @Service
    public class WhatsAppService {

        @Value("${meta.access.token}")
        private String accessToken;

        public void sendMessage(String to, String text) {
            try {
                WebClient.create("https://graph.facebook.com/v17.0/me/messages")
                        .post()
                        .header("Authorization", "Bearer " + accessToken)
                        .bodyValue("{\"messaging_product\": \"whatsapp\", \"to\": \"" + to + "\", \"type\": \"text\", \"text\": {\"body\": \"" + text + "\"}}")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } catch (Exception e) {
                System.out.println("Error al enviar mensaje: " + e.getMessage());
            }
        }
    }
