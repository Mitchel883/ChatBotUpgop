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

    public InfoService() {
        faq.put("constancia",
                "Para solicitar tu constancia: entra al portal institucional ➜ Servicios Escolares ➜ Constancias. " +
                        "Lleva tu matrícula y una identificación.");
        faq.put("ficha",
                "Para generar tu ficha: visita admisiones.unipoli.edu.mx ➜ Registra tus datos ➜ Descarga tu ficha.");
        faq.put("horario",
                "Tu horario se consulta en el portal de alumnos ➜ Mi horario.");
    }

    public String lookup(String userText, String advisorPhone) {
        String t = userText.toLowerCase();
        for (Map.Entry<String,String> e : faq.entrySet()) {
            if (t.contains(e.getKey())) return e.getValue();
        }
        return "No encontré esa información 🤔. ¿Quieres hablar con un asesor? Escríbeme *asesor* y te conecto al " +
                formPhone(advisorPhone) + ".";
    }

    private String formPhone(String e164) {
        if (e164 == null) return "";
        return "+" + e164;
    }
}