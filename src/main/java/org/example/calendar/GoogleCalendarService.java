package org.example.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;

@Service
public class GoogleCalendarService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TIMEZONE = "America/Mexico_City";

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.refresh.token}")
    private String refreshToken;

    @Value("${google.calendar.id:primary}")
    private String calendarId;

    @Value("${google.calendar.application.name:Unipoli ChatBot}")
    private String applicationName;

    private Calendar calendarService;

    @PostConstruct
    public void init() throws Exception {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // Crear credenciales OAuth2 usando refresh token
            GoogleCredentials credentials = GoogleCredentials.create(
                    new AccessToken(null, null) // El token se refrescará automáticamente
            );

            // Configurar las credenciales con client_id, client_secret y refresh_token
            credentials = com.google.auth.oauth2.UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            calendarService = new Calendar.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(applicationName)
                    .build();

            System.out.println("✅ Google Calendar inicializado correctamente con OAuth2");
            System.out.println("📅 Calendar ID: " + calendarId);

        } catch (Exception e) {
            System.err.println("❌ Error al inicializar Google Calendar: " + e.getMessage());
            e.printStackTrace();
            System.err.println("⚠️ La aplicación continuará sin sincronización de calendario");
        }
    }

    /**
     * Crea un evento en Google Calendar
     * @param summary Título del evento
     * @param description Descripción opcional
     * @param startDateTime Fecha y hora de inicio
     * @param durationMinutes Duración en minutos
     * @return ID del evento creado
     */
    public String createEvent(String summary, String description, LocalDateTime startDateTime, int durationMinutes) {
        if (calendarService == null) {
            System.out.println("⚠️ [MODO SIN CALENDAR] Evento: " + summary + " - " + startDateTime);
            return "local-only-" + System.currentTimeMillis();
        }

        try {
            // Convertir LocalDateTime a DateTime de Google
            ZonedDateTime startZoned = startDateTime.atZone(ZoneId.of(TIMEZONE));
            ZonedDateTime endZoned = startZoned.plusMinutes(durationMinutes);

            Event event = new Event()
                    .setSummary(summary)
                    .setDescription(description);

            // Configurar fecha/hora de inicio
            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                    .setTimeZone(TIMEZONE);
            event.setStart(start);

            // Configurar fecha/hora de fin
            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                    .setTimeZone(TIMEZONE);
            event.setEnd(end);

            // Insertar el evento en el calendario
            Event createdEvent = calendarService.events()
                    .insert(calendarId, event)
                    .execute();

            System.out.println("✅ Evento creado en Google Calendar: " + createdEvent.getHtmlLink());
            return createdEvent.getId();

        } catch (IOException e) {
            System.err.println("❌ Error al crear evento en Google Calendar: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Elimina un evento de Google Calendar
     * @param eventId ID del evento a eliminar
     * @return true si se eliminó exitosamente
     */
    public boolean deleteEvent(String eventId) {
        if (calendarService == null) {
            System.out.println("⚠️ [MODO SIN CALENDAR] Eliminando evento local: " + eventId);
            return true;
        }

        try {
            calendarService.events().delete(calendarId, eventId).execute();
            System.out.println("🗑️ Evento eliminado de Google Calendar: " + eventId);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Error al eliminar evento: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza un evento existente
     * @param eventId ID del evento
     * @param newSummary Nuevo título
     * @param newDateTime Nueva fecha/hora
     * @return true si se actualizó exitosamente
     */
    public boolean updateEvent(String eventId, String newSummary, LocalDateTime newDateTime) {
        if (calendarService == null) {
            System.out.println("⚠️ [MODO SIN CALENDAR] Actualizando evento local: " + eventId);
            return true;
        }

        try {
            Event event = calendarService.events().get(calendarId, eventId).execute();

            if (newSummary != null) {
                event.setSummary(newSummary);
            }

            if (newDateTime != null) {
                ZonedDateTime startZoned = newDateTime.atZone(ZoneId.of(TIMEZONE));
                EventDateTime start = new EventDateTime()
                        .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                        .setTimeZone(TIMEZONE);
                event.setStart(start);

                // Mantener la misma duración
                ZonedDateTime endZoned = startZoned.plusMinutes(30);
                EventDateTime end = new EventDateTime()
                        .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                        .setTimeZone(TIMEZONE);
                event.setEnd(end);
            }

            calendarService.events().update(calendarId, eventId, event).execute();
            System.out.println("✏️ Evento actualizado en Google Calendar: " + eventId);
            return true;

        } catch (IOException e) {
            System.err.println("❌ Error al actualizar evento: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene el enlace público del calendario
     * @return URL del calendario
     */
    public String getCalendarLink() {
        // Si es el calendario primario, usar URL diferente
        if ("primary".equals(calendarId)) {
            return "https://calendar.google.com/calendar/u/0/r";
        }
        return "https://calendar.google.com/calendar/embed?src=" + calendarId;
    }
}