package org.example.info;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class InfoService {
    private final Map<String, String> faq = new LinkedHashMap<>();


    public InfoService() {

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
        addKeyword(
                new String[]{
                        "horario", "mi horario",
                        "consultar horario", "ver horario",
                        "checar horario", "horarios"
                },
                "📅 *Horario*\n" +
                        "Tu horario se consulta en el portal de alumnos ➜ Mi horario."
        );
    }

    private void addKeyword(String[] keywords, String response) {
        for (String k : keywords) {
            faq.put(k.toLowerCase(), response);
        }
    }

    public String findAnswer(String userMessage) {
        String normalized = userMessage.toLowerCase();

        // buscar coincidencias parciales en cualquier parte del mensaje
        for (String key : faq.keySet()) {
            if (normalized.contains(key)) {
                return faq.get(key);
            }
        }
        return null; // no encontrado
    }

    public String lookup(String userText, String advisorPhone) {
        String t = userText.toLowerCase();
        for (Map.Entry<String,String> e : faq.entrySet()) {
            if (t.contains(e.getKey()))
                return e.getValue();
        }
       return null;
    }

    private String formPhone(String e164) {
        if (e164 == null) return "";
        return "+" + e164; // solo formato bonito
    }
}
