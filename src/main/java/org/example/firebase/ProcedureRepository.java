package org.example.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.example.models.Procedure;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Repository
public class ProcedureRepository {

    private final Firestore firestore;
    private static final String COLLECTION = "procedures";

    public ProcedureRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public Procedure findByKeyword(String keyword) {
        if (firestore == null || keyword == null) {
            return null;
        }

        try {
            String normalizedKeyword = keyword.toLowerCase().trim();

            // ✅ SOLUCIÓN: Removemos orderBy para evitar necesidad de índice compuesto
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                    .whereEqualTo("active", true)
                    .whereArrayContains("keywords", normalizedKeyword)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                System.out.println("⚠️ No se encontró Procedure con keyword: " + normalizedKeyword);
                return null;
            }

            // Ordenar por prioridad en memoria
            List<Procedure> procedures = new ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                Procedure procedure = doc.toObject(Procedure.class);
                procedure.id = doc.getId();
                procedures.add(procedure);
            }

            procedures.sort(Comparator.comparingInt(p -> p.priority));

            Procedure result = procedures.get(0);
            System.out.println("✅ Procedure encontrado: " + result.name);

            // Incrementar viewCount
            incrementViewCount(result.id);

            return result;

        } catch (Exception e) {
            System.err.println("❌ Error searching Procedure by keyword '" + keyword + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<Procedure> findByCategory(String category) {
        if (firestore == null) {
            return new ArrayList<>();
        }

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                    .whereEqualTo("category", category)
                    .whereEqualTo("active", true)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Procedure> procedures = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Procedure procedure = doc.toObject(Procedure.class);
                procedure.id = doc.getId();
                procedures.add(procedure);
            }

            // Ordenar por prioridad en memoria
            procedures.sort(Comparator.comparingInt(p -> p.priority));

            return procedures;

        } catch (Exception e) {
            System.err.println("Error fetching Procedures by category: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void incrementViewCount(String procedureId) {
        if (firestore == null || procedureId == null) return;

        try {
            DocumentReference docRef = firestore.collection(COLLECTION).document(procedureId);
            docRef.update("viewCount", FieldValue.increment(1));
        } catch (Exception e) {
            System.err.println("Error incrementing view count: " + e.getMessage());
        }
    }

    public void markAsHelpful(String procedureId, boolean helpful) {
        if (firestore == null || procedureId == null) return;

        try {
            DocumentReference docRef = firestore.collection(COLLECTION).document(procedureId);
            String field = helpful ? "helpfulCount" : "notHelpfulCount";
            docRef.update(field, FieldValue.increment(1));
        } catch (Exception e) {
            System.err.println("Error marking Procedure as helpful: " + e.getMessage());
        }
    }
}
