# 🔄 Guía de Migración - Sistema de Recordatorios

## 📋 Resumen

Esta guía te ayudará a migrar tu sistema actual de recordatorios para que:
- ✅ Guarde en Firebase automáticamente
- ✅ Envíe mensajes por WhatsApp a la hora programada
- ✅ Elimine de Firebase después de enviar

---

## 🎯 Cambios necesarios

### Archivos a reemplazar: 3

| Archivo | Acción | Ubicación |
|---------|--------|-----------|
| `Reminder.java` | Reemplazar | `src/main/java/org/example/reminders/` |
| `ReminderRepository.java` | Reemplazar | `src/main/java/org/example/firebase/` |
| `ReminderService.java` | Reemplazar | `src/main/java/org/example/reminders/` |

---

## 📝 Paso a Paso

### Paso 1: Backup del código actual

```bash
# Crear backup
cp src/main/java/org/example/reminders/Reminder.java Reminder.java.backup
cp src/main/java/org/example/firebase/ReminderRepository.java ReminderRepository.java.backup
cp src/main/java/org/example/reminders/ReminderService.java ReminderService.java.backup
```

### Paso 2: Descargar archivos nuevos

Descarga estos 3 archivos:
1. [Reminder.java](computer:///mnt/user-data/outputs/Reminder.java)
2. [ReminderRepository_COMPLETO.java](computer:///mnt/user-data/outputs/ReminderRepository_COMPLETO.java)
3. [ReminderService_COMPLETO.java](computer:///mnt/user-data/outputs/ReminderService_COMPLETO.java)

### Paso 3: Reemplazar archivos

```bash
# Copiar los nuevos archivos
cp Reminder.java src/main/java/org/example/reminders/Reminder.java

cp ReminderRepository_COMPLETO.java src/main/java/org/example/firebase/ReminderRepository.java

cp ReminderService_COMPLETO.java src/main/java/org/example/reminders/ReminderService.java
```

### Paso 4: Verificar imports

Asegúrate de que estos imports estén presentes:

**En Reminder.java:**
```java
import com.google.cloud.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
```

**En ReminderRepository.java:**
```java
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
```

**En ReminderService.java:**
```java
import com.google.cloud.Timestamp;
import org.springframework.scheduling.annotation.Scheduled;
import javax.annotation.PostConstruct;
```

### Paso 5: Habilitar Scheduling

En tu clase principal, agrega `@EnableScheduling`:

**Antes:**
```java
@SpringBootApplication
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
```

**Después:**
```java
@SpringBootApplication
@EnableScheduling  // ⬅️ AGREGAR ESTO
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
```

### Paso 6: Agregar imports faltantes en ReminderService

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
```

### Paso 7: Compilar

```bash
mvn clean compile
```

**Si hay errores de compilación:**
- Verifica que todos los imports estén presentes
- Asegúrate de que Firebase esté inicializado
- Verifica que `CloudApiSender` esté en el classpath

### Paso 8: Limpiar base de datos (opcional)

Si ya tenías recordatorios antiguos en Firebase con estructura diferente:

```bash
# En Firebase Console:
# 1. Ve a Firestore Database
# 2. Elimina la colección "reminders"
# 3. Los nuevos recordatorios se crearán con la estructura correcta
```

### Paso 9: Reiniciar aplicación

```bash
mvn spring-boot:run
```

### Paso 10: Verificar logs

Deberías ver:
```
✅ Firebase initialized successfully
🔄 Cargando recordatorios pendientes desde Firebase...
✅ No hay recordatorios pendientes
```

O si ya tenías recordatorios:
```
✅ Cargados X recordatorios pendientes:
   📌 [mensaje] - Para: [teléfono] - Hora: [fecha]
```

---

## 🧪 Prueba de funcionamiento

### Test 1: Crear recordatorio

**Envía por WhatsApp:**
```
Recuérdame mañana a las 3pm que debo llamar a Juan
```

**Verifica en logs:**
```
✅ Recordatorio guardado en Firebase: abc123xyz
📌 Recordatorio agregado:
   ID: abc123xyz
   Para: 5218711234567
   Mensaje: debo llamar a Juan
   Hora programada: 2025-11-26T15:00
   Total pendientes: 1
```

**Verifica en Firebase Console:**
1. Ve a Firestore Database → Collection `reminders`
2. Deberías ver un documento nuevo con:
   - `status: "PENDING"`
   - `when: [timestamp de mañana 3pm]`
   - `text: "debo llamar a Juan"`

### Test 2: Esperar envío

**A las 3pm del día siguiente:**

**Logs esperados:**
```
⏰ Procesando recordatorio:
   ID: abc123xyz
   Para: 5218711234567
   Mensaje: debo llamar a Juan
✅ Mensaje enviado exitosamente por WhatsApp
✅ Estado actualizado a SENT en Firebase
🗑️ Recordatorio eliminado de Firebase
✅ Recordatorio removido de memoria
   Recordatorios pendientes restantes: 0
```

**WhatsApp recibe:**
```
⏰ *Recordatorio*

debo llamar a Juan
```

**En Firebase Console:**
- El documento ya NO debe existir (fue eliminado)

### Test 3: Reinicio del servidor

**Antes del reinicio:**
1. Crea 2 recordatorios para 1 hora después
2. Verifica que estén en Firebase

**Detén el servidor:**
```bash
Ctrl+C
```

**Reinicia el servidor:**
```bash
mvn spring-boot:run
```

**Logs esperados:**
```
🔄 Cargando recordatorios pendientes desde Firebase...
✅ Cargados 2 recordatorios pendientes:
   📌 [recordatorio 1]
   📌 [recordatorio 2]
```

**Resultado:** Los recordatorios se mantienen y se enviarán a su hora.

---

## 🔍 Troubleshooting

### Problema 1: No se guardan en Firebase

**Síntoma:**
```
⚠️ Firestore no disponible. Reminder guardado solo en memoria.
```

**Solución:**
1. Verifica que Firebase esté inicializado:
```java
@Bean
public Firestore firestore() {
    if (FirebaseApp.getApps().isEmpty()) {
        System.out.println("⚠️ Firebase not initialized");
        return null;
    }
    return FirestoreClient.getFirestore();
}
```

2. Verifica el archivo de credenciales:
```properties
firebase.credentials.path=classpath:firebase-credentials.json
```

3. Reinicia la aplicación

### Problema 2: Error de Timestamp

**Síntoma:**
```
Can't convert object of type com.google.cloud.Timestamp to type java.time.LocalDateTime
```

**Solución:**
Asegúrate de usar el `Reminder.java` actualizado que tiene los métodos de conversión:
```java
public LocalDateTime getWhenAsLocalDateTime() {
    return toLocalDateTime(whenTimestamp);
}
```

### Problema 3: Recordatorios no se envían

**Síntoma:**
Los recordatorios aparecen en Firebase pero no se envían.

**Verificar:**

1. **¿Está habilitado el Scheduling?**
```java
@EnableScheduling  // ⬅️ En MainApplication
```

2. **¿El método tick() se está ejecutando?**
Agrega un log temporal:
```java
@Scheduled(fixedRate = 30000)
public void tick() {
    System.out.println("🔄 Verificando recordatorios... Pendientes: " + pending.size());
    // ... resto del código
}
```

3. **¿La hora es correcta?**
Verifica la zona horaria:
```java
System.out.println("Hora actual: " + LocalDateTime.now());
System.out.println("Hora recordatorio: " + reminder.getWhenAsLocalDateTime());
```

### Problema 4: Recordatorios no se eliminan

**Síntoma:**
Los recordatorios se envían pero permanecen en Firebase.

**Verificar:**

1. **¿Se está llamando a delete()?**
```java
// En ReminderService.tick()
repository.delete(reminder.id);  // ⬅️ Debe estar presente
```

2. **¿El ID existe?**
```java
if (reminder.id != null) {
    repository.delete(reminder.id);
}
```

---

## ✅ Checklist de Migración

- [ ] Backup del código actual realizado
- [ ] Archivos nuevos descargados
- [ ] `Reminder.java` reemplazado
- [ ] `ReminderRepository.java` reemplazado
- [ ] `ReminderService.java` reemplazado
- [ ] `@EnableScheduling` agregado
- [ ] Compilación exitosa (mvn clean compile)
- [ ] Firebase inicializado correctamente
- [ ] Aplicación reiniciada
- [ ] Test 1: Crear recordatorio ✓
- [ ] Verificado en Firebase Console ✓
- [ ] Test 2: Recordatorio enviado a su hora ✓
- [ ] Test 3: Recordatorio eliminado después de enviar ✓
- [ ] Test 4: Reinicio mantiene recordatorios ✓

---

## 📊 Comparación: Antes vs Después

### ❌ ANTES

```
Usuario crea recordatorio
    ↓
Se guarda solo en memoria (lista pending)
    ↓
Si el servidor se reinicia → SE PIERDE
    ↓
A la hora programada → se envía
    ↓
Se remueve de memoria
    ↓
FIN (sin rastro del recordatorio)
```

**Problemas:**
- ❌ Se pierde si el servidor se reinicia
- ❌ No hay persistencia
- ❌ No hay historial
- ❌ No se puede auditar

### ✅ DESPUÉS

```
Usuario crea recordatorio
    ↓
1. Se guarda en Firebase (status: PENDING)
    ↓
2. Se agrega a memoria
    ↓
Si el servidor se reinicia → SE RECUPERA desde Firebase
    ↓
A la hora programada:
    ↓
3. Se envía por WhatsApp
    ↓
4. Se marca como SENT en Firebase
    ↓
5. Se elimina de Firebase (ya fue enviado)
    ↓
6. Se remueve de memoria
    ↓
FIN (historial: se pueden ver los SENT antes de eliminarlos)
```

**Ventajas:**
- ✅ Persistencia garantizada
- ✅ Sobrevive reinicios
- ✅ Auditable (se pueden ver estados)
- ✅ Limpieza automática
- ✅ Manejo de errores (marca como FAILED)

---

## 🎉 ¡Migración completa!

Tu sistema de recordatorios ahora:
- ✅ Guarda en Firebase automáticamente
- ✅ Sobrevive reinicios del servidor
- ✅ Envía por WhatsApp a la hora exacta
- ✅ Elimina después de enviar (no acumula basura)
- ✅ Maneja errores correctamente

---

## 📞 Soporte

Si encuentras problemas:
1. Revisa la sección Troubleshooting
2. Verifica los logs de la aplicación
3. Consulta la documentación completa en `SISTEMA_RECORDATORIOS.md`
4. Verifica Firebase Console para ver el estado de los recordatorios
