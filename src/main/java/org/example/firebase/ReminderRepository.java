package org.example.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.example.reminders.Reminder;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    /**
     * Guarda un nuevo recordatorio en Firebase
     * @return ID del documento creado
     */
    public String save(Reminder reminder) {
        if (firestore == null) {
            System.out.println("⚠️ Firestore no disponible. Reminder guardado solo en memoria.");
            return null;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("fromPhone", reminder.fromE164);
            data.put("toPhone", reminder.toE164);
            data.put("text", reminder.text);
            data.put("when", toTimestamp(reminder.getWhenAsLocalDateTime()));
            data.put("status", reminder.status != null ? reminder.status : "PENDING");
            data.put("method", reminder.method != null ? reminder.method : "WHATSAPP");
            data.put("createdAt", toTimestamp(LocalDateTime.now()));
            data.put("sentAt", null);

            // Google Calendar ID (opcional)
            if (reminder.googleEventId != null) {
                data.put("googleEventId", reminder.googleEventId);
            }

            // Agregar a Firestore
            ApiFuture<DocumentReference> future = firestore.collection(COLLECTION).add(data);
            DocumentReference docRef = future.get();

            System.out.println("✅ Recordatorio guardado en Firebase: " + docRef.getId());
            System.out.println("   Para: " + reminder.toE164);
            System.out.println("   Mensaje: " + reminder.text);
            System.out.println("   Hora: " + reminder.getWhenAsLocalDateTime());

            return docRef.getId();

        } catch (Exception e) {
            System.err.println("❌ Error guardando recordatorio en Firebase: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene todos los recordatorios PENDIENTES
     * Se ejecuta al iniciar la aplicación para cargar recordatorios pendientes
     */
    public List<Reminder> findPending() {
        if (firestore == null) {
            return new ArrayList<>();
        }

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                    .whereEqualTo("status", "PENDING")
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Reminder> reminders = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                try {
                    Reminder reminder = documentToReminder(doc);
                    if (reminder != null) {
                        reminders.add(reminder);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error convirtiendo documento a Reminder: " + e.getMessage());
                }
            }

            System.out.println("📥 Cargados " + reminders.size() + " recordatorios pendientes desde Firebase");
            return reminders;

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo recordatorios pendientes: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Actualiza el estado de un recordatorio
     * @param reminderId ID del recordatorio
     * @param status Nuevo estado: SENT, FAILED, CANCELLED
     * @param sentAt Timestamp de cuando se envió (null si FAILED o CANCELLED)
     */
    public void updateStatus(String reminderId, String status, Timestamp sentAt) {
        if (firestore == null || reminderId == null) {
            return;
        }

        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);

            if (sentAt != null) {
                updates.put("sentAt", sentAt);
            }

            firestore.collection(COLLECTION)
                    .document(reminderId)
                    .update(updates)
                    .get();

            System.out.println("✅ Recordatorio " + reminderId + " actualizado a estado: " + status);

        } catch (Exception e) {
            System.err.println("❌ Error actualizando estado del recordatorio: " + e.getMessage());
        }
    }

    /**
     * Elimina un recordatorio de Firebase
     * Se usa después de enviarlo exitosamente o si se cancela
     */
    public void delete(String reminderId) {
        if (firestore == null || reminderId == null) {
            return;
        }

        try {
            firestore.collection(COLLECTION)
                    .document(reminderId)
                    .delete()
                    .get();

            System.out.println("🗑️ Recordatorio " + reminderId + " eliminado de Firebase");

        } catch (Exception e) {
            System.err.println("❌ Error eliminando recordatorio: " + e.getMessage());
        }
    }

    /**
     * Obtiene un recordatorio específico por ID
     */
    public Reminder findById(String reminderId) {
        if (firestore == null || reminderId == null) {
            return null;
        }

        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION)
                    .document(reminderId)
                    .get()
                    .get();

            if (!doc.exists()) {
                return null;
            }

            return documentToReminder(doc);

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo recordatorio por ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lista todos los recordatorios de un usuario
     */
    public List<Reminder> findByUser(String phoneNumber) {
        if (firestore == null || phoneNumber == null) {
            return new ArrayList<>();
        }

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                    .whereEqualTo("fromPhone", phoneNumber)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Reminder> reminders = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Reminder reminder = documentToReminder(doc);
                if (reminder != null) {
                    reminders.add(reminder);
                }
            }

            return reminders;

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo recordatorios del usuario: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Convierte un documento de Firestore a objeto Reminder
     */
    private Reminder documentToReminder(DocumentSnapshot doc) {
        try {
            Reminder reminder = new Reminder();
            reminder.id = doc.getId();
            reminder.fromE164 = doc.getString("fromPhone");
            reminder.toE164 = doc.getString("toPhone");
            reminder.text = doc.getString("text");
            reminder.status = doc.getString("status");
            reminder.method = doc.getString("method");
            reminder.googleEventId = doc.getString("googleEventId");

            // Convertir Timestamps a LocalDateTime
            Timestamp whenTs = doc.getTimestamp("when");
            if (whenTs != null) {
                reminder.setWhen(whenTs);
            }

            Timestamp createdTs = doc.getTimestamp("createdAt");
            if (createdTs != null) {
                reminder.setCreatedAt(createdTs);
            }

            Timestamp sentTs = doc.getTimestamp("sentAt");
            if (sentTs != null) {
                reminder.setSentAt(sentTs);
            }

            return reminder;

        } catch (Exception e) {
            System.err.println("❌ Error convirtiendo documento: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convierte LocalDateTime a Timestamp de Firebase
     */
    private Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        Instant instant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
    }

    /**
     * Obtiene estadísticas de recordatorios
     */
    public Map<String, Long> getStatistics() {
        if (firestore == null) {
            return new HashMap<>();
        }

        try {
            Map<String, Long> stats = new HashMap<>();

            // Contar PENDING
            long pending = firestore.collection(COLLECTION)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .get()
                    .size();
            stats.put("PENDING", pending);

            // Contar SENT
            long sent = firestore.collection(COLLECTION)
                    .whereEqualTo("status", "SENT")
                    .get()
                    .get()
                    .size();
            stats.put("SENT", sent);

            // Contar FAILED
            long failed = firestore.collection(COLLECTION)
                    .whereEqualTo("status", "FAILED")
                    .get()
                    .get()
                    .size();
            stats.put("FAILED", failed);

            return stats;

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo estadísticas: " + e.getMessage());
            return new HashMap<>();
        }
    }
}