package org.example.reminders;

import com.google.cloud.Timestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Reminder {
    // Campos originales
    public String toE164;
    public String text;

    // ✅ Usar Timestamp internamente
    private Timestamp whenTimestamp;

    public String googleEventId;

    // Nuevos campos para Firebase
    public String id;
    public String fromE164;
    public String status;

    // ✅ Usar Timestamp internamente
    private Timestamp createdAtTimestamp;
    private Timestamp sentAtTimestamp;

    public String method;

    // Constructor vacío para Firebase
    public Reminder() {
    }

    // Constructor original (3 parámetros) - mantener compatibilidad
    public Reminder(String toE164, String text, LocalDateTime when) {
        this.toE164 = toE164;
        this.fromE164 = toE164;  // Asume que quien crea es el destinatario
        this.text = text;
        this.whenTimestamp = toTimestamp(when);
        this.googleEventId = null;
        this.status = "PENDING";
        this.method = "WHATSAPP";
        this.createdAtTimestamp = Timestamp.now();
    }

    // Constructor con googleEventId (4 parámetros)
    public Reminder(String toE164, String text, LocalDateTime when, String googleEventId) {
        this.toE164 = toE164;
        this.fromE164 = toE164;
        this.text = text;
        this.whenTimestamp = toTimestamp(when);
        this.googleEventId = googleEventId;
        this.status = "PENDING";
        this.method = "WHATSAPP";
        this.createdAtTimestamp = Timestamp.now();
    }

    // Constructor completo para Firebase (5 parámetros)
    public Reminder(String fromE164, String toE164, String text, LocalDateTime when, String googleEventId) {
        this.fromE164 = fromE164;
        this.toE164 = toE164;
        this.text = text;
        this.whenTimestamp = toTimestamp(when);
        this.googleEventId = googleEventId;
        this.status = "PENDING";
        this.method = "WHATSAPP";
        this.createdAtTimestamp = Timestamp.now();
    }

    // ✅ Getters/Setters para Firestore (usa Timestamp)
    public Timestamp getWhen() {
        return whenTimestamp;
    }

    public void setWhen(Timestamp timestamp) {
        this.whenTimestamp = timestamp;
    }

    public Timestamp getCreatedAt() {
        return createdAtTimestamp;
    }

    public void setCreatedAt(Timestamp timestamp) {
        this.createdAtTimestamp = timestamp;
    }

    public Timestamp getSentAt() {
        return sentAtTimestamp;
    }

    public void setSentAt(Timestamp timestamp) {
        this.sentAtTimestamp = timestamp;
    }

    // ✅ Métodos de conveniencia para tu código (convierte a/desde LocalDateTime)
    public LocalDateTime getWhenAsLocalDateTime() {
        return toLocalDateTime(whenTimestamp);
    }

    public LocalDateTime getCreatedAtAsLocalDateTime() {
        return toLocalDateTime(createdAtTimestamp);
    }

    public LocalDateTime getSentAtAsLocalDateTime() {
        return toLocalDateTime(sentAtTimestamp);
    }

    // ✅ Métodos helper para conversión
    private static Timestamp toTimestamp(LocalDateTime ldt) {
        if (ldt == null) return null;
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) return null;
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneId.systemDefault()
        );
    }
}