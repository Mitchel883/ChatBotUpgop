package org.example.reminders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parsers súper simples para español */
public class NLP {
    // "recuérdame mañana a las 4pm que debo enviar la tarea"
    private static final Pattern REMEMBER =
            Pattern.compile("recu[eé]rdame\\s+(hoy|ma[nñ]ana)?\\s*(?:a\\s*las\\s*)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s+que\\s+(.+)", Pattern.CASE_INSENSITIVE);

    // "envía mensaje a Juan que debe enviar la tarea a las 3pm"
    private static final Pattern SEND_TO =
            Pattern.compile("env[ií]a\\s+mensaje\\s+a\\s+([a-záéíóúñ ]+)\\s+que\\s+(.+?)(?:\\s+a\\s+las\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?)?$", Pattern.CASE_INSENSITIVE);

    public static class RemindParse { public LocalDateTime when; public String text; }

    public static RemindParse parseReminder(String txt) {
        Matcher m = REMEMBER.matcher(txt);
        if (!m.find()) return null;

        String dayWord = orNull(m.group(1));
        Integer h = toInt(m.group(2));
        Integer min = m.group(3) != null ? toInt(m.group(3)) : 0;
        String ampm = orNull(m.group(4));
        String what = m.group(5).trim();

        if (ampm != null) {
            boolean pm = ampm.equalsIgnoreCase("pm");
            if (pm && h < 12) h += 12;
            if (!pm && h == 12) h = 0;
        }
        LocalDate day = LocalDate.now();
        if (dayWord != null && dayWord.toLowerCase().contains("mañ")) day = day.plusDays(1);

        RemindParse r = new RemindParse();
        r.when = LocalDateTime.of(day, LocalTime.of(h, min));
        r.text = what;
        return r;
    }

    public static class SendParse { public String contact; public String text; public Integer hour; public Integer minute; public String ampm; }

    public static SendParse parseSendTo(String txt) {
        Matcher m = SEND_TO.matcher(txt);
        if (!m.find()) return null;
        SendParse s = new SendParse();
        s.contact = m.group(1).trim();
        s.text = m.group(2).trim();
        s.hour = m.group(3) != null ? toInt(m.group(3)) : null;
        s.minute = m.group(4) != null ? toInt(m.group(4)) : 0;
        s.ampm = orNull(m.group(5));
        return s;
    }

    private static Integer toInt(String s) { return s == null ? null : Integer.parseInt(s); }
    private static String orNull(String s) { return (s != null && !s.trim().isEmpty()) ? s.trim() : null; }
}
