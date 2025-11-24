package org.example.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ScheduleRepository {

    private final Firestore firestore;
    private static final String COLLECTION = "schedules";

    public ScheduleRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Obtiene el horario de un departamento específico
     */
    public Map<String, Object> findByDepartment(String departmentId) {
        if (firestore == null || departmentId == null) {
            return null;
        }

        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION)
                    .document(departmentId)
                    .get()
                    .get();

            if (!doc.exists()) {
                System.out.println("⚠️ No se encontró horario para: " + departmentId);
                return null;
            }

            Map<String, Object> schedule = new HashMap<>(doc.getData());
            schedule.put("id", doc.getId());

            System.out.println("✅ Horario encontrado: " + schedule.get("department"));
            return schedule;

        } catch (Exception e) {
            System.err.println("❌ Error fetching schedule: " + e.getMessage());
            return null;
        }
    }

    /**
     * Busca horarios por palabra clave en el nombre del departamento
     */
    public List<Map<String, Object>> searchByDepartmentName(String keyword) {
        if (firestore == null || keyword == null) {
            return new ArrayList<>();
        }

        try {
            String normalizedKeyword = keyword.toLowerCase().trim();

            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Map<String, Object>> results = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                String department = doc.getString("department");
                if (department != null && department.toLowerCase().contains(normalizedKeyword)) {
                    Map<String, Object> schedule = new HashMap<>(doc.getData());
                    schedule.put("id", doc.getId());
                    results.add(schedule);
                }
            }

            return results;

        } catch (Exception e) {
            System.err.println("❌ Error searching schedules: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lista todos los horarios disponibles
     */
    public List<Map<String, Object>> findAll() {
        if (firestore == null) {
            return new ArrayList<>();
        }

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Map<String, Object>> results = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Map<String, Object> schedule = new HashMap<>(doc.getData());
                schedule.put("id", doc.getId());
                results.add(schedule);
            }

            return results;

        } catch (Exception e) {
            System.err.println("❌ Error fetching all schedules: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Formatea un horario para mostrar al usuario
     */
    @SuppressWarnings("unchecked")
    public String formatScheduleForUser(Map<String, Object> schedule) {
        if (schedule == null) return null;

        StringBuilder response = new StringBuilder();

        response.append("📍 *").append(schedule.get("department")).append("*\n\n");

        if (schedule.containsKey("location")) {
            response.append("🏢 Ubicación: ").append(schedule.get("location")).append("\n");
        }

        if (schedule.containsKey("phone")) {
            response.append("📞 Teléfono: ").append(schedule.get("phone")).append("\n");
        }

        if (schedule.containsKey("email")) {
            response.append("📧 Email: ").append(schedule.get("email")).append("\n");
        }

        response.append("\n⏰ *Horario de atención:*\n");

        if (schedule.containsKey("schedule")) {
            Map<String, String> hours = (Map<String, String>) schedule.get("schedule");
            response.append("Lunes: ").append(hours.getOrDefault("monday", "Cerrado")).append("\n");
            response.append("Martes: ").append(hours.getOrDefault("tuesday", "Cerrado")).append("\n");
            response.append("Miércoles: ").append(hours.getOrDefault("wednesday", "Cerrado")).append("\n");
            response.append("Jueves: ").append(hours.getOrDefault("thursday", "Cerrado")).append("\n");
            response.append("Viernes: ").append(hours.getOrDefault("friday", "Cerrado")).append("\n");
            response.append("Sábado: ").append(hours.getOrDefault("saturday", "Cerrado")).append("\n");
        }

        if (schedule.containsKey("notes")) {
            response.append("\n💡 ").append(schedule.get("notes"));
        }

        return response.toString();
    }
}