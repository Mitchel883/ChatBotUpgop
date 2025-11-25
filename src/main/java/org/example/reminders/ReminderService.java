package org.example.reminders;

import com.google.cloud.Timestamp;
import org.example.CloudApiSender;
import org.example.firebase.ReminderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ReminderService {
    private final CopyOnWriteArrayList<Reminder> pending = new CopyOnWriteArrayList<>();
    private final CloudApiSender sender;
    private final ReminderRepository repository;

    public ReminderService(CloudApiSender sender, ReminderRepository repository) {
        this.sender = sender;
        this.repository = repository;
    }

    /**
     * Carga recordatorios pendientes desde Firebase al iniciar la aplicación
     */
    @PostConstruct
    public void loadPendingFromFirebase() {
        System.out.println("🔄 Cargando recordatorios pendientes desde Firebase...");
        List<Reminder> reminders = repository.findPending();
        pending.addAll(reminders);
        System.out.println("✅ Cargados " + reminders.size() + " recordatorios pendientes");
    }

    /**
     * Agrega un nuevo recordatorio
     * @return ID del recordatorio guardado en Firebase
     */
    public String add(Reminder r) {
        // 1. Guardar en Firebase PRIMERO
        String id = repository.save(r);

        if (id != null) {
            r.id = id;
            System.out.println("✅ Recordatorio guardado en Firebase con ID: " + id);
        } else {
            System.out.println("⚠️ No se pudo guardar en Firebase, solo en memoria");
        }

        // 2. Agregar a memoria
        pending.add(r);

        System.out.println("📌 Recordatorio agregado:");
        System.out.println("   ID: " + (id != null ? id : "solo-memoria"));
        System.out.println("   Para: " + r.toE164);
        System.out.println("   Mensaje: " + r.text);
        System.out.println("   Hora: " + r.getWhenAsLocalDateTime());
        System.out.println("   Total pendientes: " + pending.size());

        return id;
    }

    /**
     * Tick cada 30 segundos para verificar recordatorios pendientes
     */
    @Scheduled(fixedRate = 30000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();

        // Lista de recordatorios a procesar
        List<Reminder> toProcess = new java.util.ArrayList<>();

        // Buscar recordatorios que ya es hora de enviar
        for (Reminder r : pending) {
            LocalDateTime whenLdt = r.getWhenAsLocalDateTime();
            if (!whenLdt.isAfter(now)) {
                toProcess.add(r);
            }
        }

        // Procesar cada recordatorio
        for (Reminder r : toProcess) {
            processReminder(r);
            pending.remove(r); // Remover de memoria usando remove() directo
        }
    }

    /**
     * Procesa un recordatorio: lo envía, actualiza estado y elimina de Firebase
     */
    private void processReminder(Reminder r) {
        try {
            // 1. Enviar por WhatsApp
            sender.sendText(r.toE164, "⏰ Recordatorio: " + r.text);

            System.out.println("✅ Recordatorio enviado: " + r.text);

            // 2. Actualizar estado a SENT en Firebase
            if (r.id != null) {
                Timestamp now = Timestamp.now();
                repository.updateStatus(r.id, "SENT", now);

                // 3. Eliminar de Firebase (ya se envió)
                repository.delete(r.id);
                System.out.println("🗑️ Recordatorio eliminado de Firebase: " + r.id);
            }

        } catch (Exception e) {
            System.err.println("❌ Error enviando recordatorio: " + e.getMessage());
            e.printStackTrace();

            // Marcar como FAILED y eliminar
            if (r.id != null) {
                repository.updateStatus(r.id, "FAILED", null);
                repository.delete(r.id);
                System.out.println("🗑️ Recordatorio fallido eliminado: " + r.id);
            }
        }
    }

    /**
     * Cancela un recordatorio
     */
    public boolean cancel(String reminderId) {
        if (reminderId == null) {
            return false;
        }

        // Buscar en memoria
        Reminder toRemove = null;
        for (Reminder r : pending) {
            if (reminderId.equals(r.id)) {
                toRemove = r;
                break;
            }
        }

        if (toRemove != null) {
            pending.remove(toRemove);
            repository.updateStatus(reminderId, "CANCELLED", null);
            repository.delete(reminderId);
            System.out.println("🚫 Recordatorio cancelado: " + reminderId);
            return true;
        }

        return false;
    }

    /**
     * Remueve un recordatorio de la lista en memoria
     */
    public boolean remove(Reminder r) {
        return pending.remove(r);
    }

    /**
     * Obtiene el número de recordatorios pendientes
     */
    public int getPendingCount() {
        return pending.size();
    }

    /**
     * Lista todos los recordatorios pendientes
     */
    public List<Reminder> getPendingReminders() {
        return new java.util.ArrayList<>(pending);
    }
}