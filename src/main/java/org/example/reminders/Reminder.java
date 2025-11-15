package org.example.reminders;

import java.time.LocalDateTime;

public class Reminder {
    public final String toE164;
    public final String text;
    public final LocalDateTime when;
    public String googleEventId; // Nuevo campo para almacenar el ID del evento en Google Calendar

    public Reminder(String toE164, String text, LocalDateTime when) {
        this.toE164 = toE164;
        this.text = text;
        this.when = when;
        this.googleEventId = null;
    }

    public Reminder(String toE164, String text, LocalDateTime when, String googleEventId) {
        this.toE164 = toE164;
        this.text = text;
        this.when = when;
        this.googleEventId = googleEventId;
    }
}