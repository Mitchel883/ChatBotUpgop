package org.example.bot;

import org.example.contacts.ContactBook;
import org.example.info.InfoService;
import org.example.reminders.NLP;
import org.example.reminders.Reminder;
import org.example.reminders.ReminderService;
import org.example.CloudApiSender;
import org.example.google.GoogleCalendarService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class ChatBotService {

    private CloudApiSender sender = null;
    private InfoService info = null;
    private ContactBook contacts = null;
    private ReminderService reminders = null;
    private GoogleCalendarService calendarService = null;

    private static final String ADVISOR = "521111111111";

    public ChatBotService(
            CloudApiSender sender,
            InfoService info,
            ContactBook contacts,
            ReminderService reminders,
            GoogleCalendarService calendarService
    ) {
        this.sender = sender;
        this.info = info;
        this.contacts = contacts;
        this.reminders = reminders;
        this.calendarService = calendarService;
    }

    public void handle(String from, String bodyRaw) {

        String body = bodyRaw.trim();


        // 0) EVENTO PRUEBA

        if (body.equalsIgnoreCase("evento prueba")) {
            try {
                String result = calendarService.crearEventoPrueba(from);
//a
                // Si el servicio regresa mensaje de token faltante
                if (result.contains("NO_OAUTH_TOKEN") ||
                        result.contains("no ha hecho login") ||
                        result.contains("NO TOKEN")) {

                    String authUrl = calendarService.generateAuthLink(from);

                    sender.sendText(
                            from,
                            "🔐 Necesito que autorices tu Google Calendar.\n" +
                                    "Haz clic aquí para activar tu cuenta:\n" +
                                    authUrl +
                                    "\n\nCuando termines, escribe *evento prueba* otra vez. 😊"
                    );
                    return;
                }

                // Evento creado
                sender.sendText(from, "📅 Evento creado exitosamente:\n" + result);

            } catch (Exception e) {

                if (e.getMessage() != null && e.getMessage().contains("NO_OAUTH_TOKEN")) {

                    String authUrl = calendarService.generateAuthLink(from);

                    sender.sendText(
                            from,
                            "🔐 Necesito que autorices tu Google Calendar.\n" +
                                    "Haz clic aquí:\n" + authUrl +
                                    "\n\nLuego vuelve a escribir *evento prueba*. 😊"
                    );
                    return;
                }

                sender.sendText(from, "⚠️ Error creando evento: " + e.getMessage());
            }
            return;
        }


        // 1) MENÚ
        if (isMenu(body)) {
            sendMenu(from);
            return;
        }


        // 2) ASESOR
        if (body.equalsIgnoreCase("asesor")) {
            sender.sendText(from,
                    "Te conecto con un asesor al +" + ADVISOR +
                            ". Escribe tu duda y te responde.");
            return;
        }

        // 3) RECORDATORIO + GOOGLE CALENDAR
        NLP.RemindParse r = NLP.parseReminder(body);
        if (r != null) {

            LocalDateTime when = r.when;
            if (when == null)
                when = LocalDateTime.of(LocalDate.now(), LocalTime.now().plusMinutes(1));

            reminders.add(new Reminder(from, r.text, when));

            try {
                calendarService.createEvent(from, r.text, when);

            } catch (Exception e) {

                // FALTÓ TOKEN → Mandar link OAuth
                if (e.getMessage() != null && e.getMessage().contains("NO_OAUTH_TOKEN")) {

                    String authUrl = calendarService.generateAuthLink(from);
                    sender.sendText(
                            from,
                            "🔐 Para agregar esto a Google Calendar necesito autorización.\n" +
                                    "Haz clic aquí:\n" + authUrl +
                                    "\n\nLuego vuelve a enviar tu recordatorio. 😊"
                    );

                } else {
                    sender.sendText(
                            from,
                            "⚠️ No pude agregar el evento a Google Calendar.\n" +
                                    "Pero tu recordatorio interno sí fue guardado."
                    );
                }
            }

            sender.sendText(from,
                    "✅ Listo, te recuerdo *" + r.text +
                            "* el " + human(when) + ".");
            return;
        }


        // 4) ENVIAR MENSAJE A CONTACTO
        NLP.SendParse s = NLP.parseSendTo(body);
        if (s != null) {
            String dest = contacts.find(s.contact);
            if (dest == null) {
                sender.sendText(from,
                        "No encontré el contacto *" + s.contact +
                                "*. Puedes darme su número así:\n'agregar juan 521234567890'.");
                return;
            }

            if (s.hour != null) {
                int h = s.hour;

                if (s.ampm != null) {
                    boolean pm = s.ampm.equalsIgnoreCase("pm");
                    if (pm && h < 12) h += 12;
                    if (!pm && h == 12) h = 0;
                }

                LocalDateTime when = LocalDateTime.of(
                        LocalDate.now(),
                        LocalTime.of(h, s.minute != null ? s.minute : 0)
                );

                reminders.add(new Reminder(dest, s.text, when));
                sender.sendText(from,
                        "📩 Enviaré a *" + capitalize(s.contact) +
                                "*: \"" + s.text + "\" a las " + hhmm(when) + ".");

            } else {
                sender.sendText(dest, s.text);
                sender.sendText(from,
                        "📨 Mensaje enviado a *" + capitalize(s.contact) + "*.");
            }
            return;
        }


        // 5) AGREGAR CONTACTO
        if (body.toLowerCase().startsWith("agregar ")) {
            String[] parts = body.split("\\s+");
            if (parts.length >= 3) {
                contacts.put(parts[1], parts[2]);
                sender.sendText(
                        from,
                        "👤 Contacto *" + capitalize(parts[1]) +
                                "* guardado con número " + parts[2] + "."
                );
            } else {
                sender.sendText(
                        from,
                        "Uso correcto:\n" +
                                "agregar <nombre> <e164_sin_+>\n" +
                                "Ej: agregar juan 521234567890"
                );
            }
            return;
        }


        // 6) FAQ o fallback
        String answer = info.lookup(body, ADVISOR);
        sender.sendText(from, answer + "\n\n" + quickMenuHint());
    }


    // Helpers

    private boolean isMenu(String s) {
        s = s.toLowerCase();
        return s.equals("menu") || s.equals("menú") || s.equals("opciones") || s.equals("ayuda");
    }

    private void sendMenu(String to) {
        sender.sendText(
                to,
                "📋 *Menú Unipoli Bot*\n" +
                        "1) Información: escribe palabras como *constancia*, *ficha*, *horario*.\n" +
                        "2) Recordatorios: *Recuérdame mañana a las 4pm que debo enviar la tarea*.\n" +
                        "3) Mensajes a contactos: *Envía mensaje a Juan que debe enviar la tarea a las 3pm*.\n" +
                        "4) Asesor humano: escribe *asesor*.\n" +
                        "5) Contactos: *agregar juan 521234567890*.\n"
        );
    }

    private String quickMenuHint() {return "Escribe *menú* para ver opciones.";}

    private String human(LocalDateTime dt) {return dt.toLocalDate() + " " + hhmm(dt);}
    private String hhmm(LocalDateTime dt) {return String.format("%02d:%02d", dt.getHour(), dt.getMinute());}
    private String capitalize(String s) {return s.substring(0,1).toUpperCase() + s.substring(1);}
}
