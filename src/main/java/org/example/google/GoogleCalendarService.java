package org.example.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.example.model.UserToken;
import org.example.reminders.ReminderParseResult;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class GoogleCalendarService {

    private final GoogleOAuthService oauth;
    private static final String TIME_ZONE_ID = "America/Mexico_City";


    public GoogleCalendarService(GoogleOAuthService oauth) {
        this.oauth = oauth;
    }


    //   CLIENTE CALENDAR
    private Calendar buildCalendarClient(UserToken token) throws Exception {

        InputStream in = getClass().getClassLoader().getResourceAsStream("credentials_oauth.json");
        if (in == null) {
            throw new IllegalStateException("No se encontró credentials_oauth.json en src/main/resources.");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(in)
        );

        String clientId = clientSecrets.getDetails().getClientId();
        String clientSecret = clientSecrets.getDetails().getClientSecret();

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
        ).setApplicationName("UnipoliBot").build();
    }


    //   EVENTO DE PRUEBA (PARA WHATSAPP → "evento prueba")
    public String crearEventoPrueba(String phone) throws Exception {

        UserToken token = oauth.getUserToken(phone);
        if (token == null) {
            throw new Exception("NO_OAUTH_TOKEN");
        }

        Calendar client = buildCalendarClient(token);

        Event event = new Event()
                .setSummary("Evento de prueba")
                .setDescription("Evento creado automáticamente desde el bot");

        ZoneId zona = ZoneId.of("America/Mexico_City");

        ZonedDateTime startTime = ZonedDateTime.now(zona).plusMinutes(10);
        ZonedDateTime endTime = startTime.plusMinutes(30);

        DateTime start = new DateTime(startTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        DateTime end = new DateTime(endTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        event.setStart(new EventDateTime()
                .setDateTime(start)
                .setTimeZone("America/Mexico_City"));

        event.setEnd(new EventDateTime()
                .setDateTime(end)
                .setTimeZone("America/Mexico_City"));

        event = client.events().insert("primary", event).execute();

        return event.getHtmlLink();
    }


    public String getLoginUrl(String phone) {
        try {
            String baseUrl = "https://unluxuriant-uninducted-marlena.ngrok-free.dev/google/auth/login";
            return baseUrl + "?phone=" + phone;
        } catch (Exception e) {
            return null;
        }
    }


    //   GENERAR LINK PARA AUTORIZAR

    public String generateAuthLink(String phone) {
        return oauth.generateAuthLink(phone);
    }

//a
    //   CREAR EVENTO REAL (USADO POR "recordatorios")

    public void createEvent(String phone, String text, LocalDateTime when) throws Exception {

        UserToken token = oauth.getUserToken(phone);
        if (token == null) {
            throw new Exception("NO_OAUTH_TOKEN");
        }

        Event event = new Event()
                .setSummary(text)
                .setDescription("Recordatorio creado desde UnipoliBot");

        Date startDate = Date.from(when.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(when.plusMinutes(30).atZone(ZoneId.systemDefault()).toInstant());

        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startDate))
                .setTimeZone(ZoneId.systemDefault().toString());

        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endDate))
                .setTimeZone(ZoneId.systemDefault().toString());

        event.setStart(start);
        event.setEnd(end);

        Calendar client = buildCalendarClient(token);
        client.events().insert("primary", event).execute();

        System.out.println("✔ Evento insertado en Google Calendar para " + phone);
    }

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
                .setSummary(title) // 🟢 Título correcto
                .setDescription("Recordatorio generado automáticamente por el bot");

        DateTime startDate = new DateTime(start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        DateTime endDate   = new DateTime(end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        event.setStart(new EventDateTime()
                .setDateTime(startDate)
                .setTimeZone("America/Mexico_City"));

        event.setEnd(new EventDateTime()
                .setDateTime(endDate)
                .setTimeZone("America/Mexico_City"));

        Event created = client.events()
                .insert("primary", event)
                .execute();

        return created.getHtmlLink(); // 🔗 link directo al Calendar
    }



    // ...resto de tu clase (buildCalendarClient, oauth, etc.)


}
