package org.example.reminders;

import org.example.CloudApiSender;
import org.example.calendar.GoogleCalendarService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ReminderService {
    private final CopyOnWriteArrayList<Reminder> pending = new CopyOnWriteArrayList<>();
    private final CloudApiSender sender;
    private final GoogleCalendarService calendarService;

    public ReminderService(CloudApiSender sender, GoogleCalendarService calendarService) {
        this.sender = sender;
        this.calendarService = calendarService;
    }

    /**
     * Agrega un recordatorio y lo sincroniza con Google Calendar
     */
    public void add(Reminder r) {
        // Crear evento en Google Calendar
        String eventId = calendarService.createEvent(
                "Recordatorio Unipoli",
                r.text,
                r.when,
                30 // Duración de 30 minutos por defecto
        );

        // Guardar el ID del evento en el recordatorio
        if (eventId != null) {
            r.googleEventId = eventId;
            System.out.println("📅 Recordatorio sincronizado con Google Calendar: " + eventId);
        }

        pending.add(r);
    }

    /**
     * Revisa cada 30 segundos si hay recordatorios que enviar
     */
    @Scheduled(fixedRate = 30000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        Iterator<Reminder> it = pending.iterator();
        while (it.hasNext()) {
            Reminder r = it.next();
            if (!r.when.isAfter(now)) {
                // Enviar recordatorio por WhatsApp
                sender.sendText(r.toE164, "⏰ Recordatorio: " + r.text);

                // Eliminar de la lista local
                it.remove();

                // Nota: El evento permanece en Google Calendar como registro histórico
                // Si quieres eliminarlo automáticamente, descomenta la siguiente línea:
                // if (r.googleEventId != null) calendarService.deleteEvent(r.googleEventId);

                System.out.println("✅ Recordatorio enviado: " + r.text);
            }
        }
    }

    /**
     * Elimina un recordatorio específico (por si implementas cancelación)
     */
    public boolean remove(Reminder r) {
        boolean removed = pending.remove(r);
        if (removed && r.googleEventId != null) {
            calendarService.deleteEvent(r.googleEventId);
        }
        return removed;
    }

    /**
     * Obtiene el enlace al calendario compartido
     */
    public String getCalendarLink() {
        return calendarService.getCalendarLink();
    }
}