package org.example.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.example.reminders.Reminder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ReminderRepository {

    private final Firestore firestore;
    private static final String COLLECTION = "reminders";

    public ReminderRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public String save(Reminder reminder) {
        if (firestore == null) {
            System.out.println("⚠️ Firestore not available. Reminder not saved to Firebase.");
            return null;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("fromE164", reminder.fromE164);
            data.put("toE164", reminder.toE164);
            data.put("text", reminder.text);
            // ✅ Usar los getters que devuelven Timestamp directamente
            data.put("when", reminder.getWhen());
            data.put("status", reminder.status);
            data.put("method", reminder.method);
            data.put("createdAt", reminder.getCreatedAt());
            data.put("sentAt", reminder.getSentAt());
            data.put("googleEventId", reminder.googleEventId);

            ApiFuture<DocumentReference> future = firestore.collection(COLLECTION).add(data);
            DocumentReference docRef = future.get();

            System.out.println("📝 Reminder saved to Firebase: " + docRef.getId());
            return docRef.getId();
        } catch (Exception e) {
            System.err.println("❌ Error saving reminder to Firebase: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<Reminder> findPending() {
        if (firestore == null) {
            return new ArrayList<>();
        }

        try {
            // ✅ Sin orderBy para evitar necesidad de índice
            // Ordenaremos en memoria en el servicio si es necesario
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                    .whereEqualTo("status", "PENDING")
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Reminder> reminders = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                try {
                    // ✅ Usar toObject directamente - Firestore maneja la conversión
                    Reminder reminder = doc.toObject(Reminder.class);
                    reminder.id = doc.getId();
                    reminders.add(reminder);
                } catch (Exception e) {
                    System.err.println("❌ Error converting document " + doc.getId() + " to Reminder: " + e.getMessage());
                }
            }

            System.out.println("🔥 Loaded " + reminders.size() + " pending reminders from Firebase");
            return reminders;
        } catch (Exception e) {
            System.err.println("❌ Error fetching pending reminders: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void updateStatus(String id, String status, Timestamp sentAt) {
        if (firestore == null || id == null) return;

        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            if (sentAt != null) {
                updates.put("sentAt", sentAt);
            }

            firestore.collection(COLLECTION).document(id).update(updates).get();
            System.out.println("✅ Reminder " + id + " updated to: " + status);
        } catch (Exception e) {
            System.err.println("❌ Error updating reminder status: " + e.getMessage());
        }
    }

    public void delete(String id) {
        if (firestore == null || id == null) return;

        try {
            firestore.collection(COLLECTION).document(id).delete().get();
            System.out.println("✅ Reminder " + id + " deleted");
        } catch (Exception e) {
            System.err.println("❌ Error deleting reminder: " + e.getMessage());
        }
    }
}