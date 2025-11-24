package org.example.models;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.List;

public class FAQ {
    @DocumentId
    public String id;

    // Categoría (se asigna desde el path de Firestore)
    public String category;

    // Campos principales
    public List<String> keywords;
    public String question;
    public String answer;
    public String documentType;  // Relaciona con el catálogo de documents
    public int priority;

    // Estado
    public boolean active;

    // Métricas
    public int viewCount;
    public int helpfulCount;
    public int notHelpfulCount;

    // Timestamps
    public Timestamp createdAt;
    public Timestamp updatedAt;

    // Constructor vacío requerido por Firestore
    public FAQ() {
        this.priority = 10; // Default priority (menor número = mayor prioridad)
        this.active = true;
        this.viewCount = 0;
        this.helpfulCount = 0;
        this.notHelpfulCount = 0;
    }

    // Constructor con parámetros básicos
    public FAQ(String category, String question, String answer, List<String> keywords) {
        this();
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.keywords = keywords;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(int helpfulCount) { this.helpfulCount = helpfulCount; }

    public int getNotHelpfulCount() { return notHelpfulCount; }
    public void setNotHelpfulCount(int notHelpfulCount) { this.notHelpfulCount = notHelpfulCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}