package org.example.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.example.model.UserToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;

@Service
public class GoogleCalendarService {

    private final GoogleOAuthService oauth;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.timezone:America/Mexico_City}")
    private String timeZone;

    @Value("${app.base-url-full}")
    private String baseUrl;

    public GoogleCalendarService(GoogleOAuthService oauth) {
        this.oauth = oauth;
    }

    /**
     * Construye el cliente de Google Calendar con las credenciales del usuario
     */
    private Calendar buildCalendarClient(UserToken token) throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setAccessToken(token.getAccessToken())
                .setRefreshToken(token.getRefreshToken());

        credential = credential.createScoped(
                Collections.singletonList("https://www.googleapis.com/auth/calendar.events")
        );

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("ChatBotUnipoli").build();
    }

    /**
     * Crea un evento de prueba (para verificar que OAuth funciona)
     */
    public String crearEventoPrueba(String phone) throws Exception {
        UserToken token = oauth.getUserToken(phone);
        if (token == null) {
            throw new Exception("NO_OAUTH_TOKEN");
        }

        Calendar client = buildCalendarClient(token);

        Event event = new Event()
                .setSummary("🧪 Evento de prueba - ChatBotUnipoli")
                .setDescription("Evento creado automáticamente desde el bot de WhatsApp");

        ZoneId zona = ZoneId.of(timeZone);
        ZonedDateTime startTime = ZonedDateTime.now(zona).plusMinutes(10);
        ZonedDateTime endTime = startTime.plusMinutes(30);

        DateTime start = new DateTime(startTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        DateTime end = new DateTime(endTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        event.setStart(new EventDateTime()
                .setDateTime(start)
                .setTimeZone(timeZone));

        event.setEnd(new EventDateTime()
                .setDateTime(end)
                .setTimeZone(timeZone));

        event = client.events().insert("primary", event).execute();

        System.out.println("✅ Evento de prueba creado: " + event.getHtmlLink());
        return event.getHtmlLink();
    }

    /**
     * Genera el link para que el usuario autorice OAuth
     */
    public String generateAuthLink(String phone) {
        return oauth.generateAuthLink(phone);
    }

    /**
     * Obtiene la URL de login (redirige al flujo OAuth)
     */
    public String getLoginUrl(String phone) {
        return baseUrl + "/google/authorize?phone=" + phone;
    }

    /**
     * Crea un evento en Google Calendar desde un recordatorio
     * Usado por el sistema de recordatorios de WhatsApp
     */
    public String createEvent(String phone, String text, LocalDateTime when) throws Exception {
        UserToken token = oauth.getUserToken(phone);
        if (token == null) {
            throw new Exception("NO_OAUTH_TOKEN");
        }

        Event event = new Event()
                .setSummary("⏰ " + text)
                .setDescription("Recordatorio creado desde ChatBotUnipoli");

        // Convertir LocalDateTime a Date
        Date startDate = Date.from(when.atZone(ZoneId.of(timeZone)).toInstant());
        Date endDate = Date.from(when.plusMinutes(30).atZone(ZoneId.of(timeZone)).toInstant());

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startDate))
                .setTimeZone(timeZone);

        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endDate))
                .setTimeZone(timeZone);

        event.setStart(start);
        event.setEnd(end);

        Calendar client = buildCalendarClient(token);
        Event created = client.events().insert("primary", event).execute();

        System.out.println("✅ Evento creado en Google Calendar para " + phone);
        System.out.println("   Título: " + text);
        System.out.println("   Fecha: " + when);
        System.out.println("   Link: " + created.getHtmlLink());

        return created.getId(); // Retorna el ID del evento
    }

    /**
     * Crea un evento desde un recordatorio con ZonedDateTime
     */
    public String crearEventoDesdeRecordatorio(
            String phone,
            String title,
            ZonedDateTime start,
            ZonedDateTime end
    ) throws Exception {
        UserToken token = oauth.getUserToken(phone);
        if (token == null) {
            throw new Exception("NO_OAUTH_TOKEN");
        }

        Calendar client = buildCalendarClient(token);

        Event event = new Event()
                .setSummary(title)
                .setDescription("Recordatorio generado automáticamente por ChatBotUnipoli");

        DateTime startDate = new DateTime(start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        DateTime endDate = new DateTime(end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        event.setStart(new EventDateTime()
                .setDateTime(startDate)
                .setTimeZone(timeZone));

        event.setEnd(new EventDateTime()
                .setDateTime(endDate)
                .setTimeZone(timeZone));

        Event created = client.events()
                .insert("primary", event)
                .execute();

        System.out.println("✅ Evento creado: " + created.getHtmlLink());
        return created.getHtmlLink();
    }

    /**
     * Verifica si un usuario tiene autorización
     */
    public boolean isAuthorized(String phone) {
        return oauth.isAuthenticated(phone);
    }

    /**
     * Revoca la autorización de un usuario
     */
    public void revokeAuthorization(String phone) {
        oauth.revokeToken(phone);
    }
}