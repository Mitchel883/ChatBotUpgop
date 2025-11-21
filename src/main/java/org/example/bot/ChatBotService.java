package org.example.bot;

import org.example.contacts.ContactBook;
import org.example.info.InfoService;
import org.example.reminders.NLP;
import org.example.reminders.Reminder;
import org.example.reminders.ReminderParser;
import org.example.reminders.ReminderService;
import org.example.CloudApiSender;
import org.example.google.GoogleCalendarService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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





    //O) BIENVENIDA
        if (isBienvenido(body)) {
            sendBienvenido(from);
            return;
        }

        // 1) MENÚ
        if (isMenu(body)) {
            sendMenu(from);
            return;
        }
        // 2) FAQ o fallback
        String answer = info.lookup(body, ADVISOR);

        if (answer != null) {
            sender.sendText(
                    from,
                    answer + "\n\n" + quickMenuHint()
            );
            // 👇 aquí cortas para que NO siga evaluando las demás opciones
            return;
        }



        // 0) EVENTO PRUEBA

    if(isReminder(body)) {
            try {
                // 1) Parsear el mensaje del usuario (fecha/hora + título)
                ReminderParser.ReminderData data = ReminderParser.parse(body);

                ZonedDateTime start = data.getStart();
                ZonedDateTime end = start.plusMinutes(30); // duración por defecto

                // 2) Crear evento en Google Calendar
                String result = calendarService.crearEventoDesdeRecordatorio(
                        from,
                        data.getTitle(), // título ya limpio: "examen de física", etc.
                        start,
                        end
                );

                // 3) Manejo de falta de token (por si tu servicio regresa el texto en vez de lanzar excepción)
                if (result != null && (
                        result.contains("NO_OAUTH_TOKEN") ||
                                result.contains("no ha hecho login") ||
                                result.contains("NO TOKEN"))) {

                    String authUrl = calendarService.generateAuthLink(from);

                    sender.sendText(
                            from,
                            "🔐 Necesito que autorices tu Google Calendar.\n" +
                                    "Haz clic aquí para activar tu cuenta:\n" +
                                    authUrl +
                                    "\n\nCuando termines, vuelve a escribirme tu recordatorio. 😊"
                    );
                    return;
                }

                // 4) Formatear fecha/hora bonito en español
                DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern(
                        "EEEE d 'de' MMMM yyyy", new Locale("es", "MX"));
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern(
                        "hh:mm a", new Locale("es", "MX"));

                String fechaStr = start.format(dateFmt);
                String horaStr = start.format(timeFmt);

                // 5) Respuesta OK al usuario, incluyendo link al evento
                sender.sendText(
                        from,
                        "📅 Evento creado:\n" +
                                "📝 *" + data.getTitle() + "*\n" +
                                "📆 " + fechaStr + "\n" +
                                "⏰ " + horaStr + "\n" +
                                "🔗 Abre tu evento en Google Calendar:\n" +
                                result
                );
                return;

            } catch (IllegalArgumentException e) {
                // errores de parseo (no entendió fecha/hora)
                sender.sendText(
                        from,
                        "🕒 No pude entender bien la fecha/hora del recordatorio.\n" +
                                "Ejemplos que sí entiendo:\n" +
                                "• Recuérdame mañana a las 4pm que debo enviar la tarea\n" +
                                "• Recuérdame el viernes a las 10 que tengo junta\n" +
                                "• Recuérdame que a las 9am tengo un evento\n"
                );
                return;

            } catch (Exception e) {

                // Por si tu servicio lanza la excepción con el mensaje NO_OAUTH_TOKEN
                if (e.getMessage() != null && e.getMessage().contains("NO_OAUTH_TOKEN")) {

                    String authUrl = calendarService.generateAuthLink(from);

                    sender.sendText(
                            from,
                            "🔐 Necesito que autorices tu Google Calendar.\n" +
                                    "Haz clic aquí:\n" + authUrl +
                                    "\n\nLuego vuelve a mandarme tu recordatorio. 😊"
                    );
                    return;
                }

                sender.sendText(from, "⚠️ Error creando evento: " + e.getMessage());
            }
        }
        //AQUI TERMINA LOGICA DE GCALENDAR



        // 3) ASESOR
        if (body.equalsIgnoreCase("asesor")) {
            sender.sendText(from,
                    "Te conecto con un asesor al +" + ADVISOR +
                            ". Escribe tu duda y te responde.");
            return;
        }else{

            sender.sendText(from,
                    "No encontré esa información 🤔. ¿Quieres hablar con un asesor? Escríbeme *asesor* y te conecto al " +
            ADVISOR + ".");

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

    private boolean isBienvenido(String s) {
        s = s.toLowerCase();
        return s.equals("hola") || s.equals("buen día") || s.equals("Hi") || s.equals("Hey");
    }

    private void sendBienvenido(String to) {
        sender.sendText(
                to,
                "👋 ¡Hola! Bienvenido al *ChatBot Unipoli*.\n\n" +
                        "Estoy aquí para ayudarte con información escolar, recordatorios y otras funciones útiles.\n\n" +
                        "📍 Cuando quieras ver todas mis opciones, solo escribe *menú*."
        );
    }

    // Prefijos válidos para interpretar que el usuario quiere un recordatorio
    private static final String[] REMINDER_PREFIXES = new String[]{
            // recordar
            "recuerdame",
            "recuerdame que",
            "recuerdame mañana",
            "recuerdame maniana",
            "recuerdame el",
            "recuerdame a las",
            "recuerdame que a las",

            // agendar
            "agendame",
            "agendame que",
            "agendame mañana",
            "agendame el",
            "agendame a las",

            // acordar / acuérdame
            "acuerdame",
            "acuerdame que",
            "acuerdame mañana",
            "acuerdame el",

            // otras formas naturales
            "ponme un recordatorio",
            "pon un recordatorio",
            "anotame",
            "anotame que",
            "anota que",
            "no se me olvide",
            "que no se me olvide",
            "avisame",
            "avisame que",
            "avísame",
            "avísame que"
    };

    private boolean isReminder(String s) {
        if (s == null) return false;

        String norm = s.toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .trim();

        for (String prefix : REMINDER_PREFIXES) {
            if (norm.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }



    private String quickMenuHint() {return "Escribe *menú* para ver opciones.";}

    private String human(LocalDateTime dt) {return dt.toLocalDate() + " " + hhmm(dt);}
    private String hhmm(LocalDateTime dt) {return String.format("%02d:%02d", dt.getHour(), dt.getMinute());}
    private String capitalize(String s) {return s.substring(0,1).toUpperCase() + s.substring(1);}
}
