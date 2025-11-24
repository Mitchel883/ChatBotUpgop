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

    @PostConstruct
    public void loadPendingFromFirebase() {
        System.out.println("📄 Loading pending reminders from Firebase...");
        List<Reminder> reminders = repository.findPending();
        pending.addAll(reminders);
        System.out.println("✅ Loaded " + reminders.size() + " pending reminders");
    }

    public void add(Reminder r) {
        // Guardar en Firebase
        String id = repository.save(r);
        if (id != null) {
            r.id = id;
        }

        // Agregar a memoria
        pending.add(r);
        System.out.println("📌 Reminder added: " + r.text + " for " + r.getWhenAsLocalDateTime());
    }

    @Scheduled(fixedRate = 30000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        Iterator<Reminder> it = pending.iterator();

        while (it.hasNext()) {
            Reminder r = it.next();

            // ✅ Convertir Timestamp a LocalDateTime para comparar
            LocalDateTime whenLdt = r.getWhenAsLocalDateTime();

            if (whenLdt != null && !whenLdt.isAfter(now)) {
                try {
                    sender.sendText(r.toE164, "⏰ Recordatorio: " + r.text);

                    // ✅ Convertir LocalDateTime a Timestamp para Firebase
                    Timestamp nowTimestamp = toTimestamp(LocalDateTime.now());
                    repository.updateStatus(r.id, "SENT", nowTimestamp);

                    it.remove();

                    System.out.println("✅ Reminder sent: " + r.text);
                } catch (Exception e) {
                    System.err.println("❌ Error sending reminder: " + e.getMessage());

                    repository.updateStatus(r.id, "FAILED", null);
                    it.remove();
                }
            }
        }
    }

    public boolean remove(Reminder r) {
        return pending.remove(r);
    }

    // ✅ Helper para convertir LocalDateTime a Timestamp
    private static Timestamp toTimestamp(LocalDateTime ldt) {
        if (ldt == null) return null;
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
    }
}