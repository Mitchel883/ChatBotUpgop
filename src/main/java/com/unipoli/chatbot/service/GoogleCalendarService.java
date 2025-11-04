package com.unipoli.chatbot.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;

    @Service
    public class GoogleCalendarService {

        @Value("${google.client.id}")
        private String clientId;

        @Value("${google.client.secret}")
        private String clientSecret;

        @Value("${google.refresh.token}")
        private String refreshToken;

        @Value("${google.redirect.uri}")
        private String redirectUri;

        private Calendar getCalendarService() throws IOException {
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setClientSecrets(clientId, clientSecret)
                    .build()
                    .setRefreshToken(refreshToken);

            return new Calendar.Builder(
                    com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport(),
                    com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("ChatBotUnipoli").build();
        }

        public void createEvent(String summary, String startTime, String endTime) {
            try {
                Calendar service = getCalendarService();
                Event event = new Event().setSummary(summary);

                Date startDate = new Date();
                EventDateTime start = new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(startTime)).setTimeZone("America/Mexico_City");
                EventDateTime end = new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(endTime)).setTimeZone("America/Mexico_City");

                event.setStart(start);
                event.setEnd(end);

                service.events().insert("primary", event).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String listEvents() {
            StringBuilder sb = new StringBuilder();
            try {
                Calendar service = getCalendarService();
                Events events = service.events().list("primary")
                        .setMaxResults(5)
                        .setTimeMin(new com.google.api.client.util.DateTime(new Date()))
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute();

                if (events.getItems().isEmpty()) return "No hay próximos eventos.";
                for (Event event : events.getItems()) {
                    sb.append("• ").append(event.getSummary())
                            .append(" — ").append(event.getStart().getDateTime())
                            .append("\n");
                }
            } catch (Exception e) {
                sb.append("Error al listar eventos.");
            }
            return sb.toString();
        }
    }


