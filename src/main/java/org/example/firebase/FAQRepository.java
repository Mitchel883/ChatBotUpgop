package org.example.firebase;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.example.models.FAQ;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Repository
public class FAQRepository {

    private final Firestore firestore;
    private static final String COLLECTION = "faq";
    private static final String[] CATEGORIES = {"DOCUMENTOS", "INSCRIPCIONES", "ACADEMICO", "PAGOS", "SERVICIOS"};

    public FAQRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public FAQ findByKeyword(String keyword) {
        if (firestore == null || keyword == null) {
            return null;
        }

        try {
            String normalizedKeyword = keyword.toLowerCase().trim();
            List<FAQ> allFaqs = new ArrayList<>();

            // Buscar en todas las categorías
            for (String category : CATEGORIES) {
                ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                        .document(category)
                        .collection("questions")
                        .whereEqualTo("active", true)
                        .whereArrayContains("keywords", normalizedKeyword)
                        .get();

                List<QueryDocumentSnapshot> documents = future.get().getDocuments();

                for (QueryDocumentSnapshot doc : documents) {
                    FAQ faq = doc.toObject(FAQ.class);
                    faq.id = doc.getId();
                    faq.category = category;
                    allFaqs.add(faq);
                }
            }

            if (allFaqs.isEmpty()) {
                System.out.println("⚠️ No se encontró FAQ con keyword: " + normalizedKeyword);
                return null;
            }

            // Ordenar por prioridad (menor número = mayor prioridad)
            allFaqs.sort(Comparator.comparingInt(f -> f.priority));

            FAQ result = allFaqs.get(0);
            System.out.println("✅ FAQ encontrado: " + result.question + " (Categoría: " + result.category + ")");

            // Incrementar viewCount
            incrementViewCount(result.category, result.id);

            return result;

        } catch (Exception e) {
            System.err.println("❌ Error searching FAQ by keyword '" + keyword + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<FAQ> findByCategory(String category) {
        if (firestore == null) {
            return new ArrayList<>();
        }

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                    .document(category)
                    .collection("questions")
                    .whereEqualTo("active", true)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<FAQ> faqs = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                FAQ faq = doc.toObject(FAQ.class);
                faq.id = doc.getId();
                faq.category = category;
                faqs.add(faq);
            }

            // Ordenar por prioridad en memoria
            faqs.sort(Comparator.comparingInt(f -> f.priority));

            return faqs;

        } catch (Exception e) {
            System.err.println("Error fetching FAQs by category: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void incrementViewCount(String category, String faqId) {
        if (firestore == null || category == null || faqId == null) return;

        try {
            DocumentReference docRef = firestore.collection(COLLECTION)
                    .document(category)
                    .collection("questions")
                    .document(faqId);
            docRef.update("viewCount", FieldValue.increment(1));
        } catch (Exception e) {
            System.err.println("Error incrementing view count: " + e.getMessage());
        }
    }

    public void markAsHelpful(String category, String faqId, boolean helpful) {
        if (firestore == null || category == null || faqId == null) return;

        try {
            DocumentReference docRef = firestore.collection(COLLECTION)
                    .document(category)
                    .collection("questions")
                    .document(faqId);
            String field = helpful ? "helpfulCount" : "notHelpfulCount";
            docRef.update(field, FieldValue.increment(1));
        } catch (Exception e) {
            System.err.println("Error marking FAQ as helpful: " + e.getMessage());
        }
    }

    /**
     * Busca en todas las categorías - método alternativo más eficiente
     * (requiere índice compuesto en Firestore)
     */
    public List<FAQ> searchAcrossAllCategories(String keyword) {
        if (firestore == null || keyword == null) {
            return new ArrayList<>();
        }

        try {
            String normalizedKeyword = keyword.toLowerCase().trim();
            List<FAQ> allFaqs = new ArrayList<>();

            // Buscar en todas las categorías en paralelo
            for (String category : CATEGORIES) {
                ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION)
                        .document(category)
                        .collection("questions")
                        .whereEqualTo("active", true)
                        .whereArrayContains("keywords", normalizedKeyword)
                        .get();

                List<QueryDocumentSnapshot> documents = future.get().getDocuments();

                for (QueryDocumentSnapshot doc : documents) {
                    FAQ faq = doc.toObject(FAQ.class);
                    faq.id = doc.getId();
                    faq.category = category;
                    allFaqs.add(faq);
                }
            }

            // Ordenar por prioridad
            allFaqs.sort(Comparator.comparingInt(f -> f.priority));

            return allFaqs;

        } catch (Exception e) {
            System.err.println("Error searching across categories: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}