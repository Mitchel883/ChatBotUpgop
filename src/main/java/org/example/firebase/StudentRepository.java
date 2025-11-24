package org.example.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class StudentRepository {

    private final Firestore firestore;
    private static final String COLLECTION = "students";

    public StudentRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Obtiene información de un estudiante por su número de teléfono
     */
    public Map<String, Object> findByPhone(String phoneNumber) {
        if (firestore == null || phoneNumber == null) {
            return null;
        }

        try {
            // Normalizar el número (debe estar en formato E.164: 5218711234567)
            String normalizedPhone = normalizePhoneNumber(phoneNumber);

            DocumentSnapshot doc = firestore.collection(COLLECTION)
                    .document(normalizedPhone)
                    .get()
                    .get();

            if (!doc.exists()) {
                System.out.println("⚠️ Estudiante no encontrado: " + normalizedPhone);
                return null;
            }

            Map<String, Object> student = new HashMap<>(doc.getData());
            student.put("phoneNumber", doc.getId());

            System.out.println("✅ Estudiante encontrado: " + student.get("name"));
            return student;

        } catch (Exception e) {
            System.err.println("❌ Error fetching student: " + e.getMessage());
            return null;
        }
    }

    /**
     * Crea o actualiza información de un estudiante
     */
    public boolean saveOrUpdate(String phoneNumber, Map<String, Object> studentData) {
        if (firestore == null || phoneNumber == null || studentData == null) {
            return false;
        }

        try {
            String normalizedPhone = normalizePhoneNumber(phoneNumber);

            // Agregar timestamp de actualización
            studentData.put("updatedAt", Timestamp.now());

            // Si es nuevo, agregar timestamp de creación
            DocumentSnapshot existing = firestore.collection(COLLECTION)
                    .document(normalizedPhone)
                    .get()
                    .get();

            if (!existing.exists()) {
                studentData.put("createdAt", Timestamp.now());
            }

            firestore.collection(COLLECTION)
                    .document(normalizedPhone)
                    .set(studentData, SetOptions.merge())
                    .get();

            System.out.println("✅ Estudiante guardado/actualizado: " + normalizedPhone);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error saving student: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza campos específicos de un estudiante
     */
    public boolean updateFields(String phoneNumber, Map<String, Object> updates) {
        if (firestore == null || phoneNumber == null || updates == null) {
            return false;
        }

        try {
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            updates.put("updatedAt", Timestamp.now());

            firestore.collection(COLLECTION)
                    .document(normalizedPhone)
                    .update(updates)
                    .get();

            System.out.println("✅ Estudiante actualizado: " + normalizedPhone);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error updating student: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un estudiante existe
     */
    public boolean exists(String phoneNumber) {
        if (firestore == null || phoneNumber == null) {
            return false;
        }

        try {
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            DocumentSnapshot doc = firestore.collection(COLLECTION)
                    .document(normalizedPhone)
                    .get()
                    .get();

            return doc.exists();

        } catch (Exception e) {
            System.err.println("❌ Error checking student existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Normaliza un número de teléfono al formato E.164
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;

        // Remover caracteres no numéricos
        phone = phone.replaceAll("[^0-9]", "");

        // Si no empieza con código de país, agregar 52 (México)
        if (!phone.startsWith("52") && phone.length() == 10) {
            phone = "52" + phone;
        }

        return phone;
    }

    /**
     * Formatea información del estudiante para mostrar
     */
    public String formatStudentInfo(Map<String, Object> student) {
        if (student == null) return null;

        StringBuilder info = new StringBuilder();
        info.append("👤 *Información del Estudiante*\n\n");

        if (student.containsKey("name")) {
            info.append("Nombre: ").append(student.get("name")).append("\n");
        }

        if (student.containsKey("studentId")) {
            info.append("Matrícula: ").append(student.get("studentId")).append("\n");
        }

        if (student.containsKey("career")) {
            info.append("Carrera: ").append(student.get("career")).append("\n");
        }

        if (student.containsKey("semester")) {
            info.append("Semestre: ").append(student.get("semester")).append("\n");
        }

        if (student.containsKey("email")) {
            info.append("Email: ").append(student.get("email")).append("\n");
        }

        if (student.containsKey("status")) {
            info.append("Estatus: ").append(student.get("status")).append("\n");
        }

        return info.toString();
    }
}