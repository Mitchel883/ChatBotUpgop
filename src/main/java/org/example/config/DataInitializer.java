package org.example.config;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(Firestore firestore) {
        return args -> {
            if (firestore == null) {
                System.out.println("⚠️ Firestore no disponible. Saltando inicialización de datos.");
                return;
            }

            System.out.println("📄 Verificando datos iniciales en Firebase...");

            // Verificar si ya existen datos
            long faqCount = firestore.collection("faq").limit(1).get().get().size();

            if (faqCount > 0) {
                System.out.println("✅ Datos ya existen en Firebase. Saltando inicialización.");
                return;
            }

            System.out.println("🔧 Inicializando estructura de datos en Firebase...");

            Timestamp now = Timestamp.now();

            // 1. Inicializar FAQs (por categoría)
            initializeFAQs(firestore, now);

            // 2. Inicializar Procedures
            initializeProcedures(firestore, now);

            // 3. Inicializar Documents (Catálogo)
            initializeDocuments(firestore, now);

            // 4. Inicializar Schedules (Horarios de atención)
            initializeSchedules(firestore, now);

            // 5. Inicializar Students de ejemplo (opcional)
            initializeExampleStudents(firestore, now);

            // 6. Verificar colección de Reminders (se crea automáticamente al usar)
            initializeRemindersCollection(firestore);

            System.out.println("✅ Estructura de datos inicializada en Firebase");
        };
    }

    // ==================== FAQs ====================
    private void initializeFAQs(Firestore firestore, Timestamp now) throws Exception {
        System.out.println("📝 Inicializando FAQs...");

        // FAQ Categoría: DOCUMENTOS
        createFAQ(firestore, "DOCUMENTOS",
                "constancia_estudios",
                Arrays.asList("constancia", "estudios", "escolar", "certificado", "documento"),
                "¿Cómo solicito una constancia?",
                "📄 *Constancia de Estudios*\n\n" +
                        "Para solicitar tu constancia:\n" +
                        "1. Entra al portal institucional\n" +
                        "2. Ve a Servicios Escolares → Constancias\n" +
                        "3. Lleva tu matrícula y una identificación oficial\n\n" +
                        "💰 Costo: $50 MXN\n" +
                        "⏰ Tiempo de entrega: 3 días hábiles",
                "constancia",
                1,
                now
        );

        createFAQ(firestore, "DOCUMENTOS",
                "kardex",
                Arrays.asList("kardex", "historial", "calificaciones", "record", "materias"),
                "¿Cómo obtengo mi kardex?",
                "📊 *Kardex*\n\n" +
                        "Para solicitar tu kardex:\n" +
                        "1. Acude a Control Escolar\n" +
                        "2. Presenta identificación oficial\n" +
                        "3. Realiza el pago correspondiente\n\n" +
                        "💰 Costo: $100 MXN\n" +
                        "⏰ Entrega: 5 días hábiles",
                "kardex",
                2,
                now
        );

        // FAQ Categoría: INSCRIPCIONES
        createFAQ(firestore, "INSCRIPCIONES",
                "ficha_admision",
                Arrays.asList("ficha", "admision", "inscripcion", "generar ficha", "registro"),
                "¿Cómo genero mi ficha?",
                "📝 *Ficha de Admisión*\n\n" +
                        "Para generar tu ficha:\n" +
                        "1. Visita: https://admisiones.unipoli.edu.mx\n" +
                        "2. Registra tus datos personales\n" +
                        "3. Descarga tu ficha en PDF\n\n" +
                        "💡 Tip: Guarda tu número de ficha para futuras consultas",
                null,
                1,
                now
        );

        // FAQ Categoría: ACADEMICO
        createFAQ(firestore, "ACADEMICO",
                "horario_clases",
                Arrays.asList("horario", "clases", "consultar horario", "ver horario", "mi horario"),
                "¿Cómo consulto mi horario?",
                "📅 *Horario de Clases*\n\n" +
                        "Para consultar tu horario:\n" +
                        "1. Entra al portal de alumnos\n" +
                        "2. Ve a la sección 'Mi horario'\n" +
                        "3. Selecciona el periodo actual\n\n" +
                        "💡 También puedes descargarlo en PDF",
                null,
                1,
                now
        );

        createFAQ(firestore, "ACADEMICO",
                "calificaciones",
                Arrays.asList("calificaciones", "notas", "parciales", "ver calificaciones"),
                "¿Cómo consulto mis calificaciones?",
                "📈 *Calificaciones*\n\n" +
                        "Para consultar tus calificaciones:\n" +
                        "1. Ingresa al portal de alumnos\n" +
                        "2. Selecciona 'Calificaciones'\n" +
                        "3. Elige el periodo que deseas consultar\n\n" +
                        "Las calificaciones se actualizan después de cada periodo de exámenes.",
                null,
                2,
                now
        );

        // FAQ Categoría: PAGOS
        createFAQ(firestore, "PAGOS",
                "pagos_colegiaturas",
                Arrays.asList("pagar", "colegiatura", "mensualidad", "pagos", "cuanto pagar"),
                "¿Cómo realizo el pago de colegiatura?",
                "💳 *Pago de Colegiatura*\n\n" +
                        "Opciones de pago:\n" +
                        "1. *En línea:* Portal de alumnos → Pagos\n" +
                        "2. *Transferencia:* Usa tu número de referencia\n" +
                        "3. *Caja:* Lunes a viernes 8:00-16:00\n\n" +
                        "💰 Consulta tu adeudo en el portal de alumnos",
                null,
                1,
                now
        );

        System.out.println("✅ FAQs inicializados");
    }

    private void createFAQ(Firestore firestore, String category, String questionId,
                           List<String> keywords, String question, String answer,
                           String documentType, int priority, Timestamp now) throws Exception {

        Map<String, Object> faq = new HashMap<>();
        faq.put("keywords", keywords);
        faq.put("question", question);
        faq.put("answer", answer);
        faq.put("documentType", documentType);
        faq.put("priority", priority);
        faq.put("active", true);
        faq.put("viewCount", 0);
        faq.put("helpfulCount", 0);
        faq.put("notHelpfulCount", 0);
        faq.put("createdAt", now);
        faq.put("updatedAt", now);

        firestore.collection("faq")
                .document(category)
                .collection("questions")
                .document(questionId)
                .set(faq)
                .get();
    }

    // ==================== PROCEDURES ====================
    private void initializeProcedures(Firestore firestore, Timestamp now) throws Exception {
        System.out.println("📋 Inicializando Procedures...");

        // Procedure: Solicitar Constancia
        Map<String, Object> proc1 = new HashMap<>();
        proc1.put("name", "Solicitar Constancia de Estudios");
        proc1.put("category", "documentos");
        proc1.put("description", "Documento oficial que acredita tu inscripción actual en la universidad");
        proc1.put("requirements", Arrays.asList(
                "Identificación oficial (INE, Pasaporte o Licencia)",
                "Comprobante de pago vigente",
                "Estar al corriente en pagos"
        ));
        proc1.put("steps", Arrays.asList(
                "1. Ir a Control Escolar (Edificio A, Planta Baja)",
                "2. Solicitar formato de constancia",
                "3. Llenar el formato con letra legible",
                "4. Pagar $50 MXN en caja o transferencia",
                "5. Entregar formato y comprobante de pago",
                "6. Recoger constancia en 3 días hábiles"
        ));
        proc1.put("cost", "$50 MXN");
        proc1.put("deliveryTime", "3 días hábiles");
        proc1.put("contactPerson", "Lic. García - control@unipoli.edu.mx - Ext. 101");
        proc1.put("active", true);
        proc1.put("priority", 1);
        proc1.put("createdAt", now);
        proc1.put("updatedAt", now);

        firestore.collection("procedures").document("constancia_estudios").set(proc1).get();

        // Procedure: Solicitar Kardex
        Map<String, Object> proc2 = new HashMap<>();
        proc2.put("name", "Solicitar Kardex");
        proc2.put("category", "documentos");
        proc2.put("description", "Historial académico completo de tu carrera");
        proc2.put("requirements", Arrays.asList(
                "Identificación oficial",
                "No tener adeudos pendientes",
                "Estar inscrito en el semestre actual"
        ));
        proc2.put("steps", Arrays.asList(
                "1. Acude a Control Escolar",
                "2. Solicita el formato de kardex",
                "3. Presenta tu identificación oficial",
                "4. Realiza el pago de $100 MXN",
                "5. Recoge tu kardex en 5 días hábiles"
        ));
        proc2.put("cost", "$100 MXN");
        proc2.put("deliveryTime", "5 días hábiles");
        proc2.put("contactPerson", "Lic. Martínez - control@unipoli.edu.mx - Ext. 102");
        proc2.put("active", true);
        proc2.put("priority", 2);
        proc2.put("createdAt", now);
        proc2.put("updatedAt", now);

        firestore.collection("procedures").document("kardex").set(proc2).get();

        // Procedure: Inscripción a Nuevo Semestre
        Map<String, Object> proc3 = new HashMap<>();
        proc3.put("name", "Inscripción a Nuevo Semestre");
        proc3.put("category", "academico");
        proc3.put("description", "Proceso para inscribirte al siguiente semestre");
        proc3.put("requirements", Arrays.asList(
                "Haber aprobado el 70% de las materias del semestre anterior",
                "No tener adeudos de semestres anteriores",
                "Tener kardex actualizado"
        ));
        proc3.put("steps", Arrays.asList(
                "1. Consulta tus calificaciones finales en el portal",
                "2. Revisa la lista de materias disponibles",
                "3. Selecciona tus materias en el portal de alumnos",
                "4. Realiza el pago de inscripción",
                "5. Imprime tu comprobante de inscripción"
        ));
        proc3.put("cost", "Según plan de estudios (consulta en caja)");
        proc3.put("deliveryTime", "Inmediato (una vez realizado el pago)");
        proc3.put("contactPerson", "Servicios Escolares - escolares@unipoli.edu.mx");
        proc3.put("active", true);
        proc3.put("priority", 1);
        proc3.put("createdAt", now);
        proc3.put("updatedAt", now);

        firestore.collection("procedures").document("inscripcion_semestre").set(proc3).get();

        System.out.println("✅ Procedures inicializados");
    }

    // ==================== DOCUMENTS (Catálogo) ====================
    private void initializeDocuments(Firestore firestore, Timestamp now) throws Exception {
        System.out.println("📄 Inicializando catálogo de documentos...");

        // Documento: Constancia de Estudios
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("name", "Constancia de Estudios");
        doc1.put("keywords", Arrays.asList("constancia", "estudios", "escolar", "certificado"));
        doc1.put("procedureId", "constancia_estudios");
        doc1.put("cost", "$50 MXN");
        doc1.put("deliveryTime", "3 días hábiles");
        doc1.put("active", true);
        doc1.put("createdAt", now);

        firestore.collection("documents").document("constancia_estudios").set(doc1).get();

        // Documento: Kardex
        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("name", "Kardex");
        doc2.put("keywords", Arrays.asList("kardex", "historial", "calificaciones", "materias"));
        doc2.put("procedureId", "kardex");
        doc2.put("cost", "$100 MXN");
        doc2.put("deliveryTime", "5 días hábiles");
        doc2.put("active", true);
        doc2.put("createdAt", now);

        firestore.collection("documents").document("kardex").set(doc2).get();

        // Documento: Certificado de Calificaciones
        Map<String, Object> doc3 = new HashMap<>();
        doc3.put("name", "Certificado de Calificaciones");
        doc3.put("keywords", Arrays.asList("certificado", "calificaciones", "notas", "parciales"));
        doc3.put("procedureId", null); // Sin procedimiento específico
        doc3.put("cost", "$75 MXN");
        doc3.put("deliveryTime", "5 días hábiles");
        doc3.put("active", true);
        doc3.put("createdAt", now);

        firestore.collection("documents").document("certificado_calificaciones").set(doc3).get();

        System.out.println("✅ Catálogo de documentos inicializado");
    }

    // ==================== SCHEDULES (Horarios de atención) ====================
    private void initializeSchedules(Firestore firestore, Timestamp now) throws Exception {
        System.out.println("🕐 Inicializando horarios de atención...");

        // Control Escolar
        Map<String, Object> schedule1 = new HashMap<>();
        schedule1.put("department", "Control Escolar");
        schedule1.put("location", "Edificio A, Planta Baja, Oficina 101");
        schedule1.put("phone", "8711234567");
        schedule1.put("email", "control@unipoli.edu.mx");

        Map<String, String> hours1 = new HashMap<>();
        hours1.put("monday", "08:00-16:00");
        hours1.put("tuesday", "08:00-16:00");
        hours1.put("wednesday", "08:00-16:00");
        hours1.put("thursday", "08:00-16:00");
        hours1.put("friday", "08:00-14:00");
        hours1.put("saturday", "Cerrado");
        hours1.put("sunday", "Cerrado");
        schedule1.put("schedule", hours1);
        schedule1.put("notes", "No se atiende sin cita previa en temporada de inscripciones");
        schedule1.put("createdAt", now);

        firestore.collection("schedules").document("control_escolar").set(schedule1).get();

        // Servicios Escolares
        Map<String, Object> schedule2 = new HashMap<>();
        schedule2.put("department", "Servicios Escolares");
        schedule2.put("location", "Edificio B, Segundo Piso, Oficina 201");
        schedule2.put("phone", "8711234568");
        schedule2.put("email", "escolares@unipoli.edu.mx");

        Map<String, String> hours2 = new HashMap<>();
        hours2.put("monday", "08:00-16:00");
        hours2.put("tuesday", "08:00-16:00");
        hours2.put("wednesday", "08:00-16:00");
        hours2.put("thursday", "08:00-16:00");
        hours2.put("friday", "08:00-16:00");
        hours2.put("saturday", "09:00-13:00");
        hours2.put("sunday", "Cerrado");
        schedule2.put("schedule", hours2);
        schedule2.put("notes", "Atención los sábados solo durante periodo de inscripciones");
        schedule2.put("createdAt", now);

        firestore.collection("schedules").document("servicios_escolares").set(schedule2).get();

        // Caja
        Map<String, Object> schedule3 = new HashMap<>();
        schedule3.put("department", "Caja / Pagos");
        schedule3.put("location", "Edificio A, Planta Baja, Oficina 105");
        schedule3.put("phone", "8711234569");
        schedule3.put("email", "pagos@unipoli.edu.mx");

        Map<String, String> hours3 = new HashMap<>();
        hours3.put("monday", "08:00-16:00");
        hours3.put("tuesday", "08:00-16:00");
        hours3.put("wednesday", "08:00-16:00");
        hours3.put("thursday", "08:00-16:00");
        hours3.put("friday", "08:00-14:00");
        hours3.put("saturday", "Cerrado");
        hours3.put("sunday", "Cerrado");
        schedule3.put("schedule", hours3);
        schedule3.put("notes", "Pagos en efectivo, tarjeta o transferencia");
        schedule3.put("createdAt", now);

        firestore.collection("schedules").document("caja").set(schedule3).get();

        // Biblioteca
        Map<String, Object> schedule4 = new HashMap<>();
        schedule4.put("department", "Biblioteca");
        schedule4.put("location", "Edificio C, Planta Baja");
        schedule4.put("phone", "8711234570");
        schedule4.put("email", "biblioteca@unipoli.edu.mx");

        Map<String, String> hours4 = new HashMap<>();
        hours4.put("monday", "07:00-20:00");
        hours4.put("tuesday", "07:00-20:00");
        hours4.put("wednesday", "07:00-20:00");
        hours4.put("thursday", "07:00-20:00");
        hours4.put("friday", "07:00-18:00");
        hours4.put("saturday", "09:00-14:00");
        hours4.put("sunday", "Cerrado");
        schedule4.put("schedule", hours4);
        schedule4.put("notes", "Préstamo de libros con credencial vigente");
        schedule4.put("createdAt", now);

        firestore.collection("schedules").document("biblioteca").set(schedule4).get();

        System.out.println("✅ Horarios de atención inicializados");
    }

    // ==================== STUDENTS (Ejemplos) ====================
    private void initializeExampleStudents(Firestore firestore, Timestamp now) throws Exception {
        System.out.println("👤 Inicializando estudiantes de ejemplo...");

        // Estudiante de ejemplo 1
        Map<String, Object> student1 = new HashMap<>();
        student1.put("name", "Juan Pérez García");
        student1.put("email", "juan.perez@unipoli.edu.mx");
        student1.put("studentId", "2023001");
        student1.put("career", "Ingeniería en Software");
        student1.put("semester", 5);
        student1.put("status", "ACTIVO");
        student1.put("createdAt", now);
        student1.put("updatedAt", now);

        firestore.collection("students").document("5218711234567").set(student1).get();

        // Estudiante de ejemplo 2
        Map<String, Object> student2 = new HashMap<>();
        student2.put("name", "María López Hernández");
        student2.put("email", "maria.lopez@unipoli.edu.mx");
        student2.put("studentId", "2023002");
        student2.put("career", "Ingeniería Industrial");
        student2.put("semester", 3);
        student2.put("status", "ACTIVO");
        student2.put("createdAt", now);
        student2.put("updatedAt", now);

        firestore.collection("students").document("5218711234568").set(student2).get();

        System.out.println("✅ Estudiantes de ejemplo inicializados");
    }

    // ==================== REMINDERS ====================
    private void initializeRemindersCollection(Firestore firestore) throws Exception {
        System.out.println("⏰ Verificando colección de recordatorios...");

        // Verificar si ya existe la colección
        long reminderCount = firestore.collection("reminders").limit(1).get().get().size();

        if (reminderCount > 0) {
            System.out.println("✅ Colección de reminders ya existe con " + reminderCount + " recordatorio(s)");
            return;
        }

        // La colección se creará automáticamente cuando se agregue el primer recordatorio
        // No es necesario crear documentos de ejemplo para reminders ya que son temporales

        System.out.println("✅ Colección de reminders lista para usarse");
        System.out.println("   Los recordatorios se crearán automáticamente cuando los usuarios los soliciten");
    }
}