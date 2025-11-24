package org.example.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DocumentRepository {

    private final Firestore firestore;
    private static final String COLLECTION = "documents";

    public DocumentRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Busca un documento por palabra clave
     */
    public Map<String, Object> findByKeyword(String keyword) {
        if (firestore == null || keyword == null) {
            return null;
        }

        try {
            String normalizedKeyword = keyword.toLowerCase().trim();

            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                    .whereEqualTo("active", true)
                    .whereArrayContains("keywords", normalizedKeyword)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                return null;
            }

            QueryDocumentSnapshot doc = documents.get(0);
            Map<String, Object> document = new HashMap<>(doc.getData());
            document.put("id", doc.getId());

            System.out.println("✅ Documento encontrado: " + document.get("name"));
            return document;

        } catch (Exception e) {
            System.err.println("❌ Error searching document by keyword: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene un documento por ID
     */
    public Map<String, Object> findById(String documentId) {
        if (firestore == null || documentId == null) {
            return null;
        }

        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION)
                    .document(documentId)
                    .get()
                    .get();

            if (!doc.exists()) {
                return null;
            }

            Map<String, Object> document = new HashMap<>(doc.getData());
            document.put("id", doc.getId());
            return document;

        } catch (Exception e) {
            System.err.println("❌ Error fetching document by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lista todos los documentos activos
     */
    public List<Map<String, Object>> findAllActive() {
        if (firestore == null) {
            return new ArrayList<>();
        }

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                    .whereEqualTo("active", true)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Map<String, Object>> result = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Map<String, Object> document = new HashMap<>(doc.getData());
                document.put("id", doc.getId());
                result.add(document);
            }

            return result;

        } catch (Exception e) {
            System.err.println("❌ Error fetching all documents: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}