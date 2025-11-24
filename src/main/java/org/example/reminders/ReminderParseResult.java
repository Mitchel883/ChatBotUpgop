package org.example.reminders;

import java.time.ZonedDateTime;

// Puedes ponerlo en su propio archivo o como clase estática interna
public class ReminderParseResult {
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String title;
    private final String originalText;

    public ReminderParseResult(ZonedDateTime start,
                               ZonedDateTime end,
                               String title,
                               String originalText) {
        this.start = start;
        this.end = end;
        this.title = title;
        this.originalText = originalText;
    }

    public ZonedDateTime getStart() { return start; }
    public ZonedDateTime getEnd() { return end; }
    public String getTitle() { return title; }
    public String getOriginalText() { return originalText; }
}
