package org.example.info;

import org.example.firebase.FAQRepository;
import org.example.firebase.ProcedureRepository;
import org.example.models.FAQ;
import org.example.models.Procedure;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class InfoService {
    private final Map<String, String> faq = new LinkedHashMap<>();
    private final FAQRepository faqRepository;
    private final ProcedureRepository procedureRepository;

    public InfoService(FAQRepository faqRepository, ProcedureRepository procedureRepository) {
        this.faqRepository = faqRepository;
        this.procedureRepository = procedureRepository;
        loadHardcodedFAQs();
    }

    private void loadHardcodedFAQs() {
        // --- CONSTANCIA ---
        addKeyword(
                new String[]{
                        "constancia", "constancias", "mi constancia",
                        "sacar constancia", "generar constancia",
                        "ayudame con la constancia", "necesito constancia",
                        "obtener constancia"
                },
                "📄 *Constancia*\n" +
                        "Para solicitar tu constancia: entra al portal institucional ➜ Servicios Escolares ➜ Constancias.\n" +
                        "Lleva tu matrícula y una identificación."
        );

        // --- FICHA ---
        addKeyword(
                new String[]{
                        "ficha", "fichas", "mi ficha",
                        "generar ficha", "sacar ficha",
                        "obtener ficha", "crear ficha",
                        "ayudame con la ficha"
                },
                "📝 *Ficha de admisión*\n" +
                        "Para generar tu ficha visita: https://admisiones.unipoli.edu.mx\n" +
                        "➜ Registra tus datos ➜ Descarga tu ficha."
        );

        // --- HORARIO ---
        // ❌ COMENTADO: Ahora se usa SOLO la respuesta de Firebase
        // Este hardcoded estaba impidiendo que se consultara la base de datos
        /*
        addKeyword(
                new String[]{
                        "horario", "mi horario",
                        "consultar horario", "ver horario",
                        "checar horario", "horarios"
                },
                "📅 *Horario*\n" +
                        "Tu horario se consulta en el portal de alumnos ➜ Mi horario."
        );
        */
    }

    private void addKeyword(String[] keywords, String response) {
        for (String k : keywords) {
            faq.put(k.toLowerCase(), response);
        }
    }

    public String lookup(String userText, String advisorPhone) {
        if (userText == null || userText.trim().isEmpty()) {
            return null;
        }

        String normalized = userText.toLowerCase().trim();

        // PRIORIDAD 1: Buscar en Firebase (si está disponible)
        String firebaseAnswer = searchInFirebase(normalized);
        if (firebaseAnswer != null) {
            return firebaseAnswer;
        }

        // PRIORIDAD 2: Buscar en FAQs hardcodeados (fallback)
        for (Map.Entry<String, String> e : faq.entrySet()) {
            if (normalized.contains(e.getKey())) {
                return e.getValue();
            }
        }

        // No se encontró información
        return null;
    }

    private String searchInFirebase(String normalizedText) {
        if (faqRepository == null && procedureRepository == null) {
            return null; // Firebase no disponible
        }

        String[] words = normalizedText.split("\\s+");

        // Buscar en FAQ de Firebase
        if (faqRepository != null) {
            for (String word : words) {
                try {
                    FAQ faq = faqRepository.findByKeyword(word);
                    if (faq != null) {
                        return faq.answer;
                    }
                } catch (Exception e) {
                    // Si Firebase falla, continuar con hardcoded
                    System.err.println("Error searching FAQ in Firebase: " + e.getMessage());
                }
            }
        }

        // Buscar en Procedures de Firebase
        if (procedureRepository != null) {
            for (String word : words) {
                try {
                    Procedure procedure = procedureRepository.findByKeyword(word);
                    if (procedure != null) {
                        return formatProcedureResponse(procedure);
                    }
                } catch (Exception e) {
                    System.err.println("Error searching Procedure in Firebase: " + e.getMessage());
                }
            }
        }

        return null;
    }

    private String formatProcedureResponse(Procedure procedure) {
        StringBuilder response = new StringBuilder();

        response.append("📄 *").append(procedure.name).append("*\n\n");

        if (procedure.description != null && !procedure.description.isEmpty()) {
            response.append(procedure.description).append("\n\n");
        }

        if (procedure.requirements != null && !procedure.requirements.isEmpty()) {
            response.append("✅ *Requisitos:*\n");
            for (String req : procedure.requirements) {
                response.append("• ").append(req).append("\n");
            }
            response.append("\n");
        }

        if (procedure.steps != null && !procedure.steps.isEmpty()) {
            response.append("📝 *Pasos:*\n");
            for (Procedure.Step step : procedure.steps) {
                response.append(step.order).append(". ").append(step.title).append("\n");
            }
            response.append("\n");
        }

        if (procedure.cost != null) {
            response.append("💰 *Costo:* $").append(procedure.cost.amount)
                    .append(" ").append(procedure.cost.currency).append("\n");
        }

        if (procedure.deliveryTime != null) {
            response.append("⏰ *Tiempo:* ").append(procedure.deliveryTime).append("\n");
        }

        if (procedure.department != null) {
            response.append("\n📍 *").append(procedure.department.name).append("*\n");
            if (procedure.department.location != null) {
                response.append("Ubicación: ").append(procedure.department.location).append("\n");
            }
            if (procedure.department.phone != null) {
                response.append("📞 ").append(procedure.department.phone).append("\n");
            }
        }

        return response.toString().trim();
    }

    // Métodos legacy para compatibilidad
    public String findAnswer(String userMessage) {
        return lookup(userMessage, null);
    }

    private String formPhone(String e164) {
        if (e164 == null) return "";
        return "+" + e164;
    }
}