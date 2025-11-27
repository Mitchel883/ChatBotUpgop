# 📅 Sistema Completo de Recordatorios - ChatBot Unipoli

## 🎯 Objetivo

Implementar un sistema completo de recordatorios que:
1. ✅ Guarda recordatorios en Firebase cuando el usuario los crea
2. ✅ Opcionalmente los guarda también en Google Calendar
3. ✅ Envía mensaje por WhatsApp a la hora programada
4. ✅ Elimina el recordatorio de Firebase después de enviarlo

---

## 🔄 Flujo Completo del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│ 1. USUARIO CREA RECORDATORIO                                │
│    "Recuérdame mañana a las 4pm que debo enviar la tarea"  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. CHATBOT PARSEA EL MENSAJE                                │
│    - Extrae: hora (4pm), día (mañana), mensaje (tarea)     │
│    - Crea objeto Reminder con status: PENDING               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. GUARDAR EN FIREBASE                                       │
│    Collection: reminders                                     │
│    Document: {                                               │
│      fromPhone: "5218711234567"                             │
│      toPhone: "5218711234567"                               │
│      text: "debo enviar la tarea"                           │
│      when: Timestamp(2025-11-25T16:00:00)                   │
│      status: "PENDING"                                       │
│      method: "WHATSAPP"                                      │
│      createdAt: Timestamp(now)                              │
│    }                                                         │
│    ✅ Retorna ID: "abc123xyz"                               │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. (OPCIONAL) GUARDAR EN GOOGLE CALENDAR                    │
│    - Crea evento en Google Calendar                         │
│    - Guarda googleEventId en el recordatorio                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. AGREGAR A MEMORIA (lista pending)                        │
│    - Se agrega a CopyOnWriteArrayList<Reminder>            │
│    - Permanece en memoria hasta enviarse                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. CONFIRMAR AL USUARIO                                      │
│    "✅ Recordatorio programado para mañana 25/11 a las 4pm" │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼ (esperar hasta la hora programada)
                     │
┌─────────────────────────────────────────────────────────────┐
│ 7. SCHEDULER VERIFICA CADA 30 SEGUNDOS                      │
│    @Scheduled(fixedRate = 30000)                            │
│    ¿Ya es hora de enviar algún recordatorio?                │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼ (SI: hora actual >= hora programada)
                     │
┌─────────────────────────────────────────────────────────────┐
│ 8. ENVIAR MENSAJE POR WHATSAPP                              │
│    sender.sendText(toPhone, "⏰ Recordatorio: " + text)     │
│    ✅ Mensaje enviado exitosamente                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 9. ACTUALIZAR ESTADO EN FIREBASE                            │
│    - status: "PENDING" → "SENT"                             │
│    - sentAt: Timestamp(now)                                 │
│    ✅ Estado actualizado                                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 10. ELIMINAR DE FIREBASE                                     │
│     repository.delete(reminderId)                           │
│     🗑️ Recordatorio eliminado de Firebase                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 11. REMOVER DE MEMORIA                                       │
│     pending.remove(reminder)                                │
│     ✅ Recordatorio completado y removido                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Estructura en Firebase

### Collection: `reminders`

```javascript
reminders/
  └── abc123xyz/                    // ID autogenerado
        ├── fromPhone: "5218711234567"
        ├── toPhone: "5218711234567"
        ├── text: "debo enviar la tarea"
        ├── when: Timestamp(2025-11-25T16:00:00)
        ├── status: "PENDING"        // PENDING | SENT | FAILED | CANCELLED
        ├── method: "WHATSAPP"       // WHATSAPP | EMAIL
        ├── googleEventId: "cal123"  // (opcional) ID del evento en Google Calendar
        ├── createdAt: Timestamp
        └── sentAt: Timestamp        // null hasta que se envíe
```

### Estados posibles:

| Estado | Descripción | ¿Se elimina? |
|--------|-------------|--------------|
| `PENDING` | Esperando a ser enviado | No |
| `SENT` | Enviado exitosamente | Sí (inmediatamente) |
| `FAILED` | Error al enviar | Sí (para no reintentar) |
| `CANCELLED` | Cancelado por el usuario | Sí (inmediatamente) |

---

## 💻 Implementación - Archivos necesarios

### 1️⃣ Modelo: `Reminder.java`

**Ubicación:** `src/main/java/org/example/reminders/Reminder.java`

**Características clave:**
- ✅ Usa `Timestamp` de Firebase internamente
- ✅ Métodos `getWhenAsLocalDateTime()` para compatibilidad
- ✅ Conversión automática entre `Timestamp` y `LocalDateTime`

```java
public class Reminder {
    public String id;              // ID del documento en Firebase
    public String fromE164;        // Quien crea el recordatorio
    public String toE164;          // A quien enviar
    public String text;            // Mensaje del recordatorio
    private Timestamp whenTimestamp;    // Hora programada
    public String status;          // PENDING, SENT, FAILED, CANCELLED
    public String method;          // WHATSAPP, EMAIL
    public String googleEventId;   // (opcional) ID de Google Calendar
    private Timestamp createdAtTimestamp;
    private Timestamp sentAtTimestamp;
    
    // Métodos de conveniencia
    public LocalDateTime getWhenAsLocalDateTime() { ... }
}
```

---

### 2️⃣ Repositorio: `ReminderRepository.java`

**Ubicación:** `src/main/java/org/example/firebase/ReminderRepository.java`

**Métodos principales:**

```java
// Guardar nuevo recordatorio
String save(Reminder reminder)

// Obtener recordatorios pendientes (al iniciar app)
List<Reminder> findPending()

// Actualizar estado (SENT, FAILED)
void updateStatus(String id, String status, Timestamp sentAt)

// Eliminar recordatorio
void delete(String reminderId)

// Obtener recordatorios de un usuario
List<Reminder> findByUser(String phoneNumber)

// Estadísticas
Map<String, Long> getStatistics()
```

---

### 3️⃣ Servicio: `ReminderService.java`

**Ubicación:** `src/main/java/org/example/reminders/ReminderService.java`

**Métodos principales:**

```java
// Al iniciar aplicación: cargar pendientes desde Firebase
@PostConstruct
void loadPendingFromFirebase()

// Agregar nuevo recordatorio
String add(Reminder reminder)

// Verificar cada 30 segundos si hay recordatorios que enviar
@Scheduled(fixedRate = 30000)
void tick()

// Cancelar recordatorio
boolean cancel(String reminderId)

// Obtener estadísticas
Map<String, Object> getStatistics()
```

---

## 🚀 Proceso de Implementación

### Paso 1: Reemplazar archivos

```bash
# Modelo
cp Reminder.java src/main/java/org/example/reminders/Reminder.java

# Repositorio
cp ReminderRepository_COMPLETO.java src/main/java/org/example/firebase/ReminderRepository.java

# Servicio
cp ReminderService_COMPLETO.java src/main/java/org/example/reminders/ReminderService.java
```

### Paso 2: Agregar imports necesarios

Asegúrate de tener estos imports en tus archivos:

```java
import com.google.cloud.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
```

### Paso 3: Configurar el Scheduler

En tu clase principal o configuración, asegúrate de tener:

```java
@SpringBootApplication
@EnableScheduling  // ⬅️ IMPORTANTE
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
```

### Paso 4: Reiniciar aplicación

```bash
mvn clean spring-boot:run
```

---

## 📝 Logs esperados

### Al iniciar la aplicación:

```
✅ Firebase initialized successfully
🔄 Cargando recordatorios pendientes desde Firebase...
✅ Cargados 2 recordatorios pendientes:
   📌 enviar la tarea - Para: 5218711234567 - Hora: 2025-11-25T16:00
   📌 llamar a Juan - Para: 5218711234567 - Hora: 2025-11-26T10:00
```

### Al crear un recordatorio:

```
✅ Recordatorio guardado en Firebase: abc123xyz
📌 Recordatorio agregado:
   ID: abc123xyz
   Para: 5218711234567
   Mensaje: enviar la tarea
   Hora programada: 2025-11-25T16:00
   Total pendientes: 3
```

### Al enviar un recordatorio:

```
⏰ Procesando recordatorio:
   ID: abc123xyz
   Para: 5218711234567
   Mensaje: enviar la tarea
✅ Mensaje enviado exitosamente por WhatsApp
✅ Estado actualizado a SENT en Firebase
🗑️ Recordatorio eliminado de Firebase
✅ Recordatorio removido de memoria
   Recordatorios pendientes restantes: 2
```

### Si hay un error:

```
❌ Error procesando recordatorio:
   Mensaje de error: Connection timeout
⚠️ Recordatorio marcado como FAILED en Firebase
🗑️ Recordatorio FAILED eliminado de Firebase
⚠️ Recordatorio removido de memoria (FAILED)
```

---

## 🧪 Pruebas

### Prueba 1: Crear recordatorio simple

**Entrada (WhatsApp):**
```
Recuérdame mañana a las 4pm que debo enviar la tarea
```

**Esperado:**
1. Se guarda en Firebase con status: PENDING
2. Se agrega a memoria
3. Usuario recibe confirmación
4. A las 4pm del día siguiente:
   - Se envía mensaje: "⏰ *Recordatorio* debo enviar la tarea"
   - Se marca como SENT
   - Se elimina de Firebase
   - Se remueve de memoria

### Prueba 2: Reinicio del servidor

**Escenario:**
1. Crea 3 recordatorios
2. Detén el servidor
3. Reinicia el servidor

**Esperado:**
```
✅ Cargados 3 recordatorios pendientes desde Firebase
```

Los recordatorios se mantienen y se enviarán a su hora.

### Prueba 3: Verificar en Firebase Console

1. Ve a Firebase Console → Firestore Database
2. Colección: `reminders`
3. **Antes de la hora:** Deberías ver documentos con `status: "PENDING"`
4. **Después de enviarse:** Los documentos deben haber desaparecido

---

## 🔍 Debugging

### Verificar recordatorios en memoria:

Agrega un endpoint REST (opcional):

```java
@RestController
@RequestMapping("/api/reminders")
public class ReminderController {
    
    @Autowired
    private ReminderService reminderService;
    
    @GetMapping("/pending")
    public List<Reminder> getPending() {
        return reminderService.getPending();
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return reminderService.getStatistics();
    }
}
```

Luego consulta:
```bash
curl http://localhost:8080/api/reminders/pending
curl http://localhost:8080/api/reminders/stats
```

---

## ⚡ Ventajas de esta implementación

1. ✅ **Persistencia:** Los recordatorios sobreviven reinicios del servidor
2. ✅ **Trazabilidad:** Se puede ver historial de recordatorios enviados/fallidos
3. ✅ **Limpieza automática:** Se eliminan después de enviarse (no acumula basura)
4. ✅ **Manejo de errores:** Si falla, se marca como FAILED y se elimina
5. ✅ **Escalable:** Funciona con múltiples usuarios simultáneos
6. ✅ **Auditable:** Se registran timestamps de creación y envío

---

## 🔮 Mejoras futuras

### Fase 2: Reintentos inteligentes
```java
// Si falla, reintentar 3 veces antes de marcar como FAILED
private int retryCount;
private Timestamp lastRetryAt;
```

### Fase 3: Recordatorios recurrentes
```java
// Soporte para "cada día a las 8am"
private String recurrence;  // DAILY, WEEKLY, MONTHLY
private boolean isRecurring;
```

### Fase 4: Notificaciones múltiples
```java
// Enviar por WhatsApp Y Email
private List<String> methods;  // ["WHATSAPP", "EMAIL"]
```

---

¡Sistema de recordatorios completo implementado! 🎉
