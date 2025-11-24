package org.example.models;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.List;

public class Procedure {
    @DocumentId
    public String id;

    public String name;
    public String description;
    public List<String> keywords;
    public List<String> requirements;
    public List<Step> steps;
    public Cost cost;
    public String deliveryTime;
    public Department department;
    public String category;
    public int priority;
    public boolean active;

    // ✅ CAMBIO: Usar Timestamp en lugar de LocalDateTime
    public Timestamp createdAt;
    public Timestamp updatedAt;

    // Métricas
    public int viewCount;
    public int helpfulCount;
    public int notHelpfulCount;

    // Constructor vacío requerido por Firestore
    public Procedure() {
        this.priority = 5;
        this.active = true;
        this.viewCount = 0;
        this.helpfulCount = 0;
        this.notHelpfulCount = 0;
    }

    // Clases anidadas
    public static class Step {
        public int order;
        public String title;
        public String description;

        public Step() {}

        public Step(int order, String title, String description) {
            this.order = order;
            this.title = title;
            this.description = description;
        }
    }

    public static class Cost {
        public double amount;
        public String currency;

        public Cost() {}

        public Cost(double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }
    }

    public static class Department {
        public String name;
        public String location;
        public String phone;
        public String email;

        public Department() {}
    }
}