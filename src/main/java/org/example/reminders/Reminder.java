package org.example.reminders;

import java.time.LocalDateTime;

public class Reminder {
    public String toE164;      // a quién enviar
    public String text;        // mensaje a recordar
    public LocalDateTime when; // fecha/hora MX

    public Reminder(String toE164, String text, LocalDateTime when) {
        this.toE164 = toE164; this.text = text; this.when = when;
    }
}
