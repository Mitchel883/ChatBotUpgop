package org.example.reminders;

import java.time.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReminderParser {

    public static class ReminderData {
        private final String title;
        private final ZonedDateTime start;

        public ReminderData(String title, ZonedDateTime start) {
            this.title = title;
            this.start = start;
        }

        public String getTitle() {
            return title;
        }

        public ZonedDateTime getStart() {
            return start;
        }
    }

    // Zona horaria México (CDMX / MTY)
    public static final String TIME_ZONE_ID = "America/Mexico_City";
    private static final ZoneId ZONE_ID = ZoneId.of(TIME_ZONE_ID);

    // "a las 4", "a la 1", "a las 4pm", "a las 10:30 am"
    private static final Pattern HOUR_PATTERN =
            Pattern.compile("(a las|a la)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // "el 25", "el 7"
    private static final Pattern DAY_OF_MONTH_PATTERN =
            Pattern.compile("\\bel\\s+(\\d{1,2})\\b");

    private static final Map<String, DayOfWeek> DAYS = new HashMap<>();
    static {
        DAYS.put("lunes", DayOfWeek.MONDAY);
        DAYS.put("martes", DayOfWeek.TUESDAY);
        DAYS.put("miercoles", DayOfWeek.WEDNESDAY);
        DAYS.put("miércoles", DayOfWeek.WEDNESDAY);
        DAYS.put("jueves", DayOfWeek.THURSDAY);
        DAYS.put("viernes", DayOfWeek.FRIDAY);
        DAYS.put("sabado", DayOfWeek.SATURDAY);
        DAYS.put("sábado", DayOfWeek.SATURDAY);
        DAYS.put("domingo", DayOfWeek.SUNDAY);
    }

    public static ReminderData parse(String body) {
        if (body == null) body = "";
        String original = body.trim();
        String norm = normalize(body);

        // 1) Hora obligatoria
        Matcher hm = HOUR_PATTERN.matcher(norm);
        if (!hm.find()) {
            throw new IllegalArgumentException(
                    "No pude detectar la hora. Usa algo como: 'recuérdame mañana a las 4pm que ...'");
        }

        // posiciones de la hora en el texto normalizado
        int timeStart = hm.start();
        int timeEnd = hm.end();

        int hour = Integer.parseInt(hm.group(2));
        String minStr = hm.group(3);
        String ampm = hm.group(4) != null ? hm.group(4).toLowerCase(Locale.ROOT) : null;
        int minute = (minStr != null) ? Integer.parseInt(minStr) : 0;

        if (ampm != null) {
            if ("pm".equals(ampm) && hour < 12) hour += 12;
            if ("am".equals(ampm) && hour == 12) hour = 0;
        }

        // 2) Fecha
        LocalDate date = LocalDate.now(ZONE_ID);

        boolean contienePasadoManana =
                norm.contains("pasado mañana") || norm.contains("pasado manana");
        boolean contieneManana =
                norm.contains("mañana") || norm.contains("manana");

        if (contienePasadoManana) {
            date = date.plusDays(2);
        } else if (contieneManana) {
            date = date.plusDays(1);
        } else {
            // día de la semana
            DayOfWeek dow = detectDayOfWeek(norm);
            if (dow != null) {
                date = nextOrSame(date, dow);
            } else {
                // día del mes tipo "el 25"
                Matcher dm = DAY_OF_MONTH_PATTERN.matcher(norm);
                if (dm.find()) {
                    int day = Integer.parseInt(dm.group(1));
                    LocalDate candidate = date.withDayOfMonth(
                            Math.min(day, date.lengthOfMonth())
                    );
                    if (candidate.isBefore(date)) {
                        candidate = candidate.plusMonths(1);
                    }
                    date = candidate;
                }
            }
        }

        LocalTime time = LocalTime.of(hour, minute);
        ZonedDateTime start = ZonedDateTime.of(date, time, ZONE_ID);

        // 3) Título
        String title = extractTitle(original, norm, timeStart, timeEnd);
        if (title == null || title.trim().isEmpty()) {
            title = "Recordatorio";
        }

        return new ReminderData(title.trim(), start);
    }

    private static String normalize(String text) {
        text = text.toLowerCase(Locale.ROOT);
        text = text.replace("á","a").replace("é","e")
                .replace("í","i").replace("ó","o").replace("ú","u");
        return text;
    }

    private static DayOfWeek detectDayOfWeek(String norm) {
        for (Map.Entry<String, DayOfWeek> e : DAYS.entrySet()) {
            if (norm.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private static LocalDate nextOrSame(LocalDate base, DayOfWeek dow) {
        int current = base.getDayOfWeek().getValue();
        int target = dow.getValue();
        int diff = (target - current + 7) % 7;
        if (diff == 0) return base;
        return base.plusDays(diff);
    }

    /**
     * Regla para el título:
     *  - Si hay texto DESPUÉS de la hora => ese texto es el título.
     *  - Si no hay texto después, usamos lo que esté entre "recuérdame/que" y "a las...".
     *  - Si no se puede, tomamos todo lo que sigue a "recuérdame/que".
     */
    private static String extractTitle(String original, String norm, int timeStart, int timeEnd) {
        String lower = original.toLowerCase(Locale.ROOT);

        int idxRec = lower.indexOf("recuérdame");
        if (idxRec < 0) idxRec = lower.indexOf("recuerdame");

        int baseStart = 0;
        if (idxRec >= 0) {
            baseStart = idxRec + "recuérdame".length();
        }

        int idxQue = lower.indexOf("que ", baseStart);
        if (idxQue >= 0 && idxQue < timeStart) {
            baseStart = idxQue + "que ".length();
        }

        // 1) Texto después de la hora => título
        if (timeStart >= 0 && timeEnd > timeStart && timeEnd < original.length()) {
            String after = original.substring(timeEnd).trim();
            if (!after.isEmpty()) {
                return after;
            }
        }

        // 2) Si no hay texto después, usar lo que está entre baseStart y la hora
        if (timeStart > baseStart && timeStart <= original.length()) {
            String between = original.substring(baseStart, timeStart).trim();
            if (!between.isEmpty()) {
                return between;
            }
        }

        // 3) Fallback: todo lo que haya después de "recuérdame/que"
        if (baseStart < original.length()) {
            return original.substring(baseStart).trim();
        }

        return original;
    }

}
