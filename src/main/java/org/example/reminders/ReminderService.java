package org.example.reminders;

import org.example.CloudApiSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ReminderService {
    private final CopyOnWriteArrayList<Reminder> pending = new CopyOnWriteArrayList<>();
    private final CloudApiSender sender;

    public ReminderService(CloudApiSender sender) {
        this.sender = sender;
    }

    public void add(Reminder r) {
        pending.add(r);
    }

    // Revisa cada 30s
    @Scheduled(fixedRate = 30000)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        Iterator<Reminder> it = pending.iterator();
        while (it.hasNext()) {
            Reminder r = it.next();
            if (!r.when.isAfter(now)) {
                sender.sendText(r.toE164, "⏰ Recordatorio: " + r.text);
                it.remove();
            }
        }
    }
}
