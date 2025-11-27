# 🔧 Guía de Implementación - ChatBot Unipoli Firebase

## 📋 Resumen de cambios implementados

Esta guía te ayudará a implementar las correcciones y mejoras que resuelven el problema donde el bot devolvía respuestas hardcodeadas en lugar de consultar Firebase.

---

## ❌ Problema Original

**Síntoma:**
Cuando el usuario escribía "horario", recibía la respuesta corta hardcodeada:
```
📅 *Horario*
Tu horario se consulta en el portal de alumnos ➜ Mi horario.
```

**Causa:**
1. El código tenía FAQs hardcodeados que se ejecutaban ANTES de consultar Firebase
2. Los modelos usaban `LocalDateTime` incompatible con Firestore `Timestamp`
3. Las consultas a Firebase requerían índices compuestos que no existían

---

## ✅ Solución Implementada

### 1. Migración de LocalDateTime a Timestamp

**Archivos afectados:**
- `FAQ.java`
- `Procedure.java`
- `Reminder.java`

**Cambio:**
```java
// ❌ ANTES
public LocalDateTime createdAt;
public LocalDateTime updatedAt;

// ✅ DESPUÉS
public Timestamp createdAt;
public Timestamp updatedAt;
```

### 2. Eliminación de respuestas hardcodeadas

**Archivo:** `InfoService.java`

**Cambio:**
```java
// ❌ ANTES - Hardcoded que bloqueaba Firebase
addKeyword(
    new String[]{"horario", "mi horario"},
    "📅 *Horario*\nTu horario se consulta..."
);

// ✅ DESPUÉS - Comentado para usar Firebase
/*
addKeyword(...);
*/
```

### 3. Optimización de consultas Firestore

**Archivos afectados:**
- `FAQRepository.java`
- `ProcedureRepository.java`

**Cambio:**
```java
// ❌ ANTES - Requería índice compuesto
firestore.collection("faq")
    .whereEqualTo("active", true)
    .whereArrayContains("keywords", keyword)
    .orderBy("priority", Query.Direction.ASCENDING)  // ⚠️ Problema
    .get();

// ✅ DESPUÉS - Sin índice necesario
firestore.collection("faq")
    .whereEqualTo("active", true)
    .whereArrayContains("keywords", keyword)
    .get();

// Ordenar en memoria después
faqs.sort(Comparator.comparingInt(f -> f.priority));
```

---

## 📦 Archivos a reemplazar

### Paso 1: Descargar todos los archivos corregidos

| # | Archivo a reemplazar | Archivo nuevo |
|---|---------------------|---------------|
| 1 | `src/main/java/org/example/models/FAQ.java` | `FAQ_FINAL.java` |
| 2 | `src/main/java/org/example/models/Procedure.java` | `Procedure_FINAL.java` |
| 3 | `src/main/java/org/example/reminders/Reminder.java` | `Reminder.java` |
| 4 | `src/main/java/org/example/firebase/FAQRepository.java` | `FAQRepository.java` |
| 5 | `src/main/java/org/example/firebase/ProcedureRepository.java` | `ProcedureRepository.java` |
| 6 | `src/main/java/org/example/firebase/ReminderRepository.java` | `ReminderRepository_UPDATED.java` |
| 7 | `src/main/java/org/example/info/InfoService.java` | `InfoService.java` |
| 8 | `src/main/java/org/example/reminders/ReminderService.java` | `ReminderService.java` |
| 9 | `src/main/java/org/example/config/DataInitializer.java` | `DataInitializer.java` |

### Paso 2: Renombrar archivos

Después de copiar los archivos:
```bash
mv FAQ_FINAL.java FAQ.java
mv Procedure_FINAL.java Procedure.java
mv ReminderRepository_UPDATED.java ReminderRepository.java
```

---

## 🚀 Proceso de implementación

### 1. Backup del código actual
```bash
git add .
git commit -m "Backup antes de migración Firebase"
```

### 2. Detener la aplicación
```bash
# Detén el proceso de Spring Boot
# Ctrl+C en la terminal donde está corriendo
```

### 3. Reemplazar archivos

Copia los 9 archivos corregidos a sus ubicaciones correspondientes en tu proyecto.

### 4. Verificar estructura de Firebase

Asegúrate de que en Firestore Console tengas:

**Colección: `faq`**
```
✅ Campo: keywords (array)
✅ Campo: active (boolean)
✅ Campo: priority (number)
✅ Campo: answer (string)
✅ Campo: createdAt (timestamp)
✅ Campo: updatedAt (timestamp)
```

**Documento de ejemplo:**
```json
{
  "question": "¿Cómo consulto mi horario?",
  "answer": "📅 *Horario de Clases*\n\nPara consultar tu horario:\n1. Entra al portal de alumnos\n2. Ve a la sección 'Mi horario'\n3. Selecciona el periodo actual\n\n💡 También puedes descargarlo en PDF",
  "keywords": ["horario", "clases", "consultar horario", "ver horario", "mi horario"],
  "category": "ACADEMICO",
  "priority": 3,
  "active": true,
  "viewCount": 0,
  "createdAt": "24 de noviembre de 2025, 12:18:25 UTC-6",
  "updatedAt": "24 de noviembre de 2025, 12:18:25 UTC-6"
}
```

### 5. Compilar
```bash
mvn clean compile
```

Si hay errores, revisa que todos los archivos estén correctamente copiados.

### 6. Ejecutar
```bash
mvn spring-boot:run
```

### 7. Verificar logs

Deberías ver:
```
✅ Firebase initialized successfully
📄 Verificando datos iniciales en Firebase...
✅ Datos ya existen en Firebase. Saltando inicialización.
```

---

## 🧪 Pruebas

### Prueba 1: Consulta básica de horario

**Entrada (WhatsApp):**
```
horario
```

**Salida esperada:**
```
📅 *Horario de Clases*

Para consultar tu horario:
1. Entra al portal de alumnos
2. Ve a la sección 'Mi horario'
3. Selecciona el periodo actual

💡 También puedes descargarlo en PDF
```

**Logs esperados:**
```
✅ FAQ encontrado: ¿Cómo consulto mi horario?
```

### Prueba 2: Consulta de constancia

**Entrada (WhatsApp):**
```
¿Cómo saco mi constancia?
```

**Salida esperada:**
```
📄 *Constancia de Estudios*

Para solicitar tu constancia:
1. Entra al portal institucional
2. Ve a Servicios Escolares → Constancias
3. Lleva tu matrícula y una identificación oficial

💰 Costo: $50 MXN
⏰ Tiempo de entrega: 3 días hábiles
```

### Prueba 3: Palabra clave que no existe

**Entrada (WhatsApp):**
```
platillos voladores
```

**Salida esperada:**
```
No encontré esa información 🤔. ¿Quieres hablar con un asesor? 
Escríbeme *asesor* y te conecto al 521111111111.
```

---

## 🔍 Debugging

### Si no recibe respuestas de Firebase:

1. **Verifica logs:**
```
⚠️ No se encontró FAQ con keyword: horario
```

2. **Verifica Firestore:**
   - La colección se llama exactamente `faq` (minúsculas)
   - El campo `keywords` es un array
   - El campo `active` está en `true`
   - La palabra "horario" está en el array `keywords`

3. **Verifica conexión Firebase:**
```
✅ Firebase initialized successfully
```

Si ves:
```
⚠️ Firebase not initialized. Firestore will not be available.
```
Revisa que `firebase-credentials.json` esté en `src/main/resources/`.

### Si recibe error de Timestamp:

```
Can't convert object of type com.google.cloud.Timestamp to type java.time.LocalDateTime
```

**Solución:** Verifica que todos los modelos usen `Timestamp`:
```java
import com.google.cloud.Timestamp;

public class FAQ {
    public Timestamp createdAt;  // ✅ Correcto
    public Timestamp updatedAt;  // ✅ Correcto
}
```

### Si recibe error de índice:

```
The query requires an index. You can create it here: https://console.firebase.google.com/...
```

**Solución:** Ya está implementado. Verifica que uses la versión actualizada de los repositorios que NO tienen `orderBy()`.

---

## 📊 Verificación final

### Checklist de implementación:

- [ ] 9 archivos reemplazados
- [ ] Compilación exitosa sin errores
- [ ] Firebase inicializado correctamente
- [ ] Datos cargados en Firestore
- [ ] Prueba de "horario" devuelve respuesta larga de Firebase
- [ ] Prueba de "constancia" funciona
- [ ] Logs muestran "✅ FAQ encontrado"
- [ ] No hay errores de Timestamp en logs

---

## 🎯 Resultado final

**ANTES:**
```
Usuario: horario
Bot: 📅 *Horario* Tu horario se consulta en el portal de alumnos ➜ Mi horario.
```

**DESPUÉS:**
```
Usuario: horario
Bot: 📅 *Horario de Clases*

Para consultar tu horario:
1. Entra al portal de alumnos
2. Ve a la sección 'Mi horario'
3. Selecciona el periodo actual

💡 También puedes descargarlo en PDF
```

---

## 💡 Próximos pasos

1. **Agregar más FAQs en Firestore** sin tocar código
2. **Implementar sistema de feedback** (helpful/notHelpful)
3. **Panel de administración web** para gestionar FAQs
4. **Análisis de métricas** (viewCount)

---

## 📞 Soporte

Si encuentras problemas durante la implementación, verifica:

1. Logs de la aplicación (`mvn spring-boot:run`)
2. Firebase Console → Firestore → Verificar estructura de datos
3. Que los 9 archivos estén correctamente copiados
4. Que no haya archivos duplicados (FAQ.java vs FAQ_FINAL.java)

---

**✅ ¡Implementación completada!**

El bot ahora consulta Firebase correctamente y devuelve respuestas completas y actualizables.
