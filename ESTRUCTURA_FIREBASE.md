# 📊 Estructura de Firebase para ChatBot Unipoli

## 🗂️ Estructura completa de la base de datos

```
unipoli-chatbot/
│
├── students/                    # Información de estudiantes
│   └── {phoneNumber}/          # Documento por número de teléfono (E.164)
│       ├── name: "Juan Pérez"
│       ├── email: "juan@unipoli.edu.mx"
│       ├── studentId: "2023001"
│       ├── career: "Ingeniería en Software"
│       ├── semester: 5
│       ├── status: "ACTIVO"
│       ├── createdAt: timestamp
│       └── updatedAt: timestamp
│
├── contacts/                    # Libreta de contactos por usuario
│   └── {phoneNumber}/
│       └── contacts/           # Subcolección
│           └── {contactId}/
│               ├── name: "María"
│               ├── phone: "5212345678901"
│               ├── email: "maria@email.com"
│               └── createdAt: timestamp
│
├── reminders/                   # Recordatorios pendientes
│   └── {reminderId}/
│       ├── fromPhone: "521234567890"
│       ├── toPhone: "521234567891"
│       ├── text: "revisar proyecto"
│       ├── when: timestamp
│       ├── status: "PENDING|SENT|FAILED"
│       ├── method: "WHATSAPP|EMAIL"
│       ├── googleEventId: "abc123"
│       ├── createdAt: timestamp
│       └── sentAt: timestamp
│
├── faq/                         # Preguntas frecuentes (jerárquico)
│   ├── {category}/             # DOCUMENTOS, INSCRIPCIONES, ACADEMICO, PAGOS
│   │   └── questions/          # Subcolección
│   │       └── {questionId}/
│   │           ├── keywords: ["constancia", "certificado"]
│   │           ├── question: "¿Cómo solicito una constancia?"
│   │           ├── answer: "Debes ir a..."
│   │           ├── documentType: "constancia"
│   │           ├── priority: 1
│   │           ├── active: true
│   │           ├── viewCount: 0
│   │           ├── helpfulCount: 0
│   │           ├── notHelpfulCount: 0
│   │           ├── createdAt: timestamp
│   │           └── updatedAt: timestamp
│
├── procedures/                  # Procedimientos escolares
│   └── {procedureId}/
│       ├── name: "Solicitar Constancia"
│       ├── category: "documentos"
│       ├── description: "Pasos para solicitar constancia..."
│       ├── requirements: ["INE", "Comprobante de pago"]
│       ├── steps: [
│       │   "1. Ir a control escolar",
│       │   "2. Llenar formato...",
│       │   "3. Pagar en caja..."
│       │ ]
│       ├── cost: "$50 MXN"
│       ├── deliveryTime: "3 días hábiles"
│       ├── contactPerson: "Lic. García - control@unipoli.edu.mx"
│       ├── active: true
│       ├── priority: 1
│       ├── createdAt: timestamp
│       └── updatedAt: timestamp
│
├── documents/                   # Catálogo de documentos
│   └── {documentId}/
│       ├── name: "Constancia de Estudios"
│       ├── keywords: ["constancia", "estudios", "escolar"]
│       ├── procedureId: "constancia_estudios"  # Referencia a procedures
│       ├── cost: "$50 MXN"
│       ├── deliveryTime: "3 días hábiles"
│       ├── active: true
│       └── createdAt: timestamp
│
├── schedules/                   # Horarios de atención
│   └── {departmentId}/
│       ├── department: "Control Escolar"
│       ├── location: "Edificio A, Planta Baja"
│       ├── phone: "8711234567"
│       ├── email: "control@unipoli.edu.mx"
│       ├── schedule: {
│       │   monday: "8:00-16:00",
│       │   tuesday: "8:00-16:00",
│       │   wednesday: "8:00-16:00",
│       │   thursday: "8:00-16:00",
│       │   friday: "8:00-14:00",
│       │   saturday: "Cerrado",
│       │   sunday: "Cerrado"
│       │ }
│       ├── notes: "No se atiende sin cita previa..."
│       └── createdAt: timestamp
│
└── analytics/                   # Estadísticas (opcional - futuro)
    └── {date}/
        ├── totalMessages: 150
        ├── remindersCreated: 45
        ├── topQuestions: ["constancia", "horario"]
        └── activeUsers: 89
```

---

## 🔑 Conceptos clave

### 1. **Estructura jerárquica de FAQs**

Los FAQs se organizan por categoría para mejor organización:

```
faq/
  ├── DOCUMENTOS/
  │     └── questions/
  │           ├── constancia_estudios
  │           └── kardex
  ├── INSCRIPCIONES/
  │     └── questions/
  │           └── ficha_admision
  └── ACADEMICO/
        └── questions/
              ├── horario_clases
              └── calificaciones
```

**Ventajas:**
- Mejor organización
- Facilita la administración por categoría
- Permite permisos granulares
- Escalable para muchas FAQs

### 2. **Identificación de estudiantes por teléfono**

Usamos el número de teléfono (formato E.164) como ID de documento:

```
students/
  └── 5218711234567/
        ├── name: "Juan Pérez"
        └── ...
```

**Formato E.164:** `[código país][número]`
- México: `52` + 10 dígitos
- Ejemplo: `5218711234567`

### 3. **Relaciones entre colecciones**

**FAQ → Documents → Procedures:**

```
FAQ (pregunta sobre constancia)
  ↓ documentType: "constancia"
Document (constancia_estudios)
  ↓ procedureId: "constancia_estudios"
Procedure (pasos detallados)
```

Esto permite:
- FAQ responde rápidamente
- Si el usuario quiere más detalles → mostrar Procedure
- Reutilizar procedures en múltiples FAQs

---

## 📝 Ejemplos de uso

### Ejemplo 1: Buscar FAQ por palabra clave

**Usuario escribe:** `horario`

**Proceso:**
1. `FAQRepository.findByKeyword("horario")`
2. Busca en todas las categorías donde `keywords` contiene "horario"
3. Encuentra `faq/ACADEMICO/questions/horario_clases`
4. Devuelve respuesta formateada

**Respuesta:**
```
📅 *Horario de Clases*

Para consultar tu horario:
1. Entra al portal de alumnos
2. Ve a la sección 'Mi horario'
3. Selecciona el periodo actual

💡 También puedes descargarlo en PDF
```

### Ejemplo 2: Consultar horario de atención

**Usuario escribe:** `horario control escolar`

**Proceso:**
1. `ScheduleRepository.findByDepartment("control_escolar")`
2. Obtiene datos de `schedules/control_escolar`
3. Formatea con `formatScheduleForUser()`

**Respuesta:**
```
📍 *Control Escolar*

🏢 Ubicación: Edificio A, Planta Baja, Oficina 101
📞 Teléfono: 8711234567
📧 Email: control@unipoli.edu.mx

⏰ *Horario de atención:*
Lunes: 08:00-16:00
Martes: 08:00-16:00
Miércoles: 08:00-16:00
Jueves: 08:00-16:00
Viernes: 08:00-14:00
Sábado: Cerrado

💡 No se atiende sin cita previa en temporada de inscripciones
```

### Ejemplo 3: Información de estudiante

**Usuario con número:** `5218711234567`

**Proceso:**
1. `StudentRepository.findByPhone("5218711234567")`
2. Obtiene datos de `students/5218711234567`

**Respuesta:**
```
👤 *Información del Estudiante*

Nombre: Juan Pérez García
Matrícula: 2023001
Carrera: Ingeniería en Software
Semestre: 5
Email: juan.perez@unipoli.edu.mx
Estatus: ACTIVO
```

---

## 🔧 Repositorios disponibles

### `FAQRepository`
- `findByKeyword(String keyword)` - Busca en todas las categorías
- `findByCategory(String category)` - Lista FAQs de una categoría
- `markAsHelpful(String category, String faqId, boolean helpful)` - Registra feedback

### `ProcedureRepository`
- `findByKeyword(String keyword)` - Busca procedimientos
- `findById(String procedureId)` - Obtiene procedimiento específico

### `DocumentRepository`
- `findByKeyword(String keyword)` - Busca en catálogo de documentos
- `findById(String documentId)` - Obtiene documento específico
- `findAllActive()` - Lista todos los documentos activos

### `ScheduleRepository`
- `findByDepartment(String departmentId)` - Obtiene horario de departamento
- `searchByDepartmentName(String keyword)` - Busca por nombre
- `formatScheduleForUser(Map schedule)` - Formatea para mostrar

### `StudentRepository`
- `findByPhone(String phoneNumber)` - Obtiene info de estudiante
- `saveOrUpdate(String phone, Map data)` - Crea/actualiza estudiante
- `exists(String phoneNumber)` - Verifica si existe
- `formatStudentInfo(Map student)` - Formatea para mostrar

### `ReminderRepository`
- `save(Reminder reminder)` - Guarda recordatorio
- `findPending()` - Lista recordatorios pendientes
- `updateStatus(String id, String status, Timestamp sentAt)` - Actualiza estado

---

## 🚀 Inicialización automática

Al iniciar la aplicación por primera vez, `DataInitializer` carga automáticamente:

✅ 6 FAQs en 4 categorías diferentes
✅ 3 Procedures (Constancia, Kardex, Inscripción)
✅ 3 Documents en el catálogo
✅ 4 Schedules (Control Escolar, Servicios, Caja, Biblioteca)
✅ 2 Students de ejemplo

**Logs esperados:**
```
📄 Verificando datos iniciales en Firebase...
🔧 Inicializando estructura de datos en Firebase...
📝 Inicializando FAQs...
✅ FAQs inicializados
📋 Inicializando Procedures...
✅ Procedures inicializados
📄 Inicializando catálogo de documentos...
✅ Catálogo de documentos inicializado
🕐 Inicializando horarios de atención...
✅ Horarios de atención inicializados
👤 Inicializando estudiantes de ejemplo...
✅ Estudiantes de ejemplo inicializados
✅ Estructura de datos inicializada en Firebase
```

---

## 📊 Métricas y Analytics

Cada FAQ registra automáticamente:
- `viewCount` - Veces que se consultó
- `helpfulCount` - Veces que fue útil
- `notHelpfulCount` - Veces que no fue útil

Estos datos se pueden usar para:
- Identificar FAQs más consultadas
- Mejorar respuestas que reciben muchos "no útil"
- Análisis de tendencias

---

## 🔮 Futuras expansiones

### Analytics Collection (próximamente)
```javascript
analytics/
  └── 2025-11-24/
        ├── totalMessages: 150
        ├── remindersCreated: 45
        ├── topQuestions: ["constancia", "horario", "calificaciones"]
        ├── activeUsers: 89
        └── categoriesQueried: {
              DOCUMENTOS: 50,
              ACADEMICO: 60,
              INSCRIPCIONES: 25,
              PAGOS: 15
            }
```

### Contacts Subcollection
```javascript
contacts/
  └── 5218711234567/
        └── contacts/
              └── maria_lopez/
                    ├── name: "María López"
                    ├── phone: "5218711234568"
                    └── relation: "Compañera"
```

---

## 💡 Buenas prácticas

1. **IDs descriptivos:** Usa nombres claros como `constancia_estudios` en lugar de IDs autogenerados
2. **Timestamps siempre:** Incluye `createdAt` y `updatedAt` en todos los documentos
3. **Campo active:** Usa para "soft delete" en lugar de eliminar documentos
4. **Normalización de teléfonos:** Siempre formato E.164
5. **Keywords en minúsculas:** Para búsquedas case-insensitive

---

¿Tienes preguntas sobre la estructura? ¡Pregunta!
