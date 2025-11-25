# ChatBot Unipoli — WhatsApp Cloud API + Spring Boot + Firebase

Proyecto universitario que implementa un chatbot inteligente para WhatsApp utilizando la API de Meta (WhatsApp Cloud API), un backend en Spring Boot y Firebase Firestore como base de datos.

El sistema permite automatizar respuestas, gestionar información institucional, generar recordatorios y comunicarse con asesores a través del chat.

---

## 🚀 Características principales

### 📅 Recordatorios personalizados
**Ejemplo:**
```
Recuérdame mañana a las 4pm que debo enviar la tarea
```
El bot registra el recordatorio en Firestore y envía un mensaje a la hora indicada.

### 🤖 Automatización de mensajes
**Ejemplo:**
```
Envía mensaje a Juan que debe enviar la tarea a las 3pm
```
El bot toma el contacto y programa el envío automático del mensaje.

### 🏫 Consulta de información institucional (Firebase)
**Ejemplo:**
```
¿Cómo consulto mi horario?
```
El chatbot busca en Firestore y responde con información oficial actualizable:

```
📅 *Horario de Clases*

Para consultar tu horario:
1. Entra al portal de alumnos
2. Ve a la sección 'Mi horario'
3. Selecciona el periodo actual

💡 También puedes descargarlo en PDF
```

**Otros ejemplos:**
- `¿Cómo saco mi constancia?`
- `¿Cómo genero mi ficha?`
- `¿Cómo obtengo mi kardex?`

### 🔄 Sistema de fallback inteligente
1. **Prioridad 1:** Busca en Firebase (información actualizable)
2. **Prioridad 2:** Busca en FAQs hardcodeados (fallback local)
3. **Prioridad 3:** Ofrece contacto con asesor humano

### 🧭 Menú interactivo de opciones
El bot puede mostrar un menú con opciones predefinidas para guiar al usuario.

---

## ⚙️ Arquitectura general

```
[Usuario WhatsApp]
        │
        ▼
[Meta WhatsApp Cloud API]
        │  (Webhook)
        ▼
[Spring Boot API - /webhooks/whatsapp]
        │
        ├─ Verifica token (GET)
        ├─ Recibe mensajes (POST)
        │      ├─ CloudApiWebhookController
        │      ├─ ChatBotService
        │      ├─ InfoService
        │      │     ├─ Busca en Firebase (FAQRepository/ProcedureRepository)
        │      │     └─ Fallback a FAQs hardcodeados
        │      └─ ReminderService
        ▼
[Firebase Firestore]
        │
        ├─ Colección: faq
        ├─ Colección: procedures
        └─ Colección: reminders
        
[CloudApiSender → Meta Graph API]
        │
        ▼
[WhatsApp → Usuario]
```

---

## 🗄️ Estructura de Firebase Firestore

### Colección: `faq`
```json
{
  "id": "auto-generated",
  "category": "ACADEMICO",
  "question": "¿Cómo consulto mi horario?",
  "answer": "📅 *Horario de Clases*\n\nPara consultar tu horario:\n1. Entra al portal de alumnos\n2. Ve a la sección 'Mi horario'\n3. Selecciona el periodo actual\n\n💡 También puedes descargarlo en PDF",
  "keywords": ["horario", "clases", "consultar horario", "ver horario", "mi horario"],
  "priority": 3,
  "active": true,
  "viewCount": 0,
  "helpfulCount": 0,
  "notHelpfulCount": 0,
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

### Colección: `procedures`
```json
{
  "id": "auto-generated",
  "name": "Solicitar Constancia de Estudios",
  "category": "DOCUMENTOS",
  "description": "Documento oficial que acredita tu inscripción actual",
  "keywords": ["constancia", "estudios", "documento"],
  "requirements": ["Identificación oficial", "Comprobante de pago"],
  "steps": [
    {
      "order": 1,
      "title": "Solicitar formato",
      "detail": "Acude a Control Escolar"
    }
  ],
  "cost": {
    "amount": 50.0,
    "currency": "MXN"
  },
  "deliveryTime": "3 días hábiles",
  "department": {
    "name": "Control Escolar",
    "location": "Edificio A, Planta Baja",
    "phone": "8711234567"
  },
  "active": true,
  "priority": 1,
  "createdAt": "Timestamp",
  "updatedAt": "Timestamp"
}
```

### Colección: `reminders`
```json
{
  "id": "auto-generated",
  "fromE164": "528711532215",
  "toE164": "528711532215",
  "text": "Enviar la tarea",
  "when": "Timestamp",
  "status": "PENDING",
  "method": "WHATSAPP",
  "createdAt": "Timestamp",
  "sentAt": null,
  "googleEventId": null
}
```

---

## 🧩 Configuración paso a paso

### 1️⃣ Crear la App en Meta Developers

1. Entra en https://developers.facebook.com
2. Crea una nueva App tipo "Negocio" o "Otro"
3. Agrega el producto "WhatsApp"
4. Copia:
   - Token de acceso
   - Phone Number ID
   - WhatsApp Business Account ID

### 2️⃣ Configurar Firebase

1. Ve a https://console.firebase.google.com
2. Crea un nuevo proyecto
3. Agrega Firestore Database
4. Descarga el archivo de credenciales JSON:
   - Project Settings → Service Accounts → Generate New Private Key
5. Guarda el archivo como `firebase-credentials.json` en `src/main/resources/`

### 3️⃣ Configurar el Webhook

En Productos → Webhooks, selecciona WhatsApp Business Account:

- **URL de devolución de llamada:**
  ```
  https://<tu-ngrok>.ngrok-free.app/webhooks/whatsapp
  ```

- **Token de verificación:**
  ```
  unipoli-verify-token
  ```

- Clic en "Verificar y guardar"
- Suscríbete al campo `messages`

### 4️⃣ Configurar variables de entorno

Crea un archivo `application.properties` en `src/main/resources/`:

```properties
# WhatsApp Cloud API
whatsapp.verify.token=unipoli-verify-token
whatsapp.access.token=EAAxxxxx...
whatsapp.phone.number.id=821827761015750
whatsapp.api.url=https://graph.facebook.com/v20.0

# Firebase
firebase.credentials.path=classpath:firebase-credentials.json

# Asesor
advisor.phone=521111111111
```

### 5️⃣ Levantar tu API local

```bash
mvn clean spring-boot:run
```

Por defecto corre en: `http://localhost:8080`

Luego levanta ngrok:
```bash
ngrok http 8080
```

Copia la URL HTTPS y pégala como callback en Meta.

### 6️⃣ Inicializar datos en Firebase

Al iniciar por primera vez, el sistema automáticamente cargará datos de ejemplo en Firestore a través de `DataInitializer.java`.

---

## 🧱 Estructura del proyecto

```
src/main/java/org/example/
│
├── MainApplication.java                    → Clase principal Spring Boot
│
├── config/
│   ├── FirebaseConfig.java                → Configuración de Firebase
│   └── DataInitializer.java               → Carga inicial de datos
│
├── controller/
│   └── CloudApiWebhookController.java     → Maneja eventos entrantes
│
├── service/
│   └── CloudApiSender.java                → Envío de mensajes a Graph API
│
├── bot/
│   └── ChatBotService.java                → Lógica principal del bot
│
├── info/
│   └── InfoService.java                   → Gestión de FAQs (Firebase + Hardcoded)
│
├── firebase/
│   ├── FAQRepository.java                 → Repositorio de FAQs
│   ├── ProcedureRepository.java           → Repositorio de Procedures
│   └── ReminderRepository.java            → Repositorio de Reminders
│
├── models/
│   ├── FAQ.java                           → Modelo de FAQ
│   ├── Procedure.java                     → Modelo de Procedure
│   └── Reminder.java                      → Modelo de Reminder
│
└── reminders/
    ├── ReminderService.java               → Gestión de recordatorios
    ├── ReminderParser.java                → Parser de lenguaje natural
    └── NLP.java                           → Procesamiento de lenguaje
```

---

## 🔐 Variables de entorno

| Variable | Descripción |
|----------|-------------|
| `whatsapp.verify.token` | Token de verificación del webhook |
| `whatsapp.access.token` | Token de acceso de Meta |
| `whatsapp.phone.number.id` | ID del número de WhatsApp |
| `whatsapp.api.url` | URL base de Graph API |
| `firebase.credentials.path` | Ruta al archivo de credenciales Firebase |
| `advisor.phone` | Número del asesor para derivar consultas |

---

## 🧰 Pruebas

### Prueba manual del webhook:

```bash
curl -X POST https://<tu-ngrok>.ngrok-free.app/webhooks/whatsapp \
  -H "Content-Type: application/json" \
  -d '{
    "object":"whatsapp_business_account",
    "entry":[{
      "changes":[{
        "value":{
          "messages":[{
            "from":"528717558771",
            "id":"wamid.TEST1",
            "type":"text",
            "text":{"body":"¿Cómo consulto mi horario?"}
          }]
        }
      }]
    }]
  }'
```

### Ejemplos de consultas:

**Información institucional:**
- `horario` → Busca en Firebase
- `constancia` → Busca en Firebase
- `ficha` → Busca en Firebase

**Recordatorios:**
- `recuérdame mañana a las 4pm que debo enviar la tarea`
- `recuérdame el lunes a las 10am que tengo junta`

**Asesor:**
- `asesor` → Conecta con asesor humano

---

## 🧩 Errores comunes

| Error | Causa | Solución |
|-------|-------|----------|
| `(#131030) Recipient phone number not in allowed list` | Número no autorizado | Agrega el número en Meta (lista de prueba) |
| Webhook no recibe nada | URL vieja de ngrok | Rehacer "Verificar y guardar" |
| Mensajes duplicados | Meta reintenta al no recibir 200 OK | Responder 200 antes de procesar |
| `Can't convert Timestamp to LocalDateTime` | Modelos usan tipos incompatibles | Usar `com.google.cloud.Timestamp` en modelos |
| `The query requires an index` | Falta índice compuesto en Firestore | Crear índice o eliminar `orderBy()` |
| Falla de envío | Token vencido | Regenerar token en Meta |

---

## 🔄 Migraciones realizadas

### ✅ De LocalDateTime a Timestamp

Todos los modelos (`FAQ`, `Procedure`, `Reminder`) fueron migrados de `java.time.LocalDateTime` a `com.google.cloud.Timestamp` para compatibilidad completa con Firestore.

### ✅ Eliminación de índices compuestos

Los repositorios fueron optimizados para no requerir índices compuestos en Firestore, ordenando resultados en memoria cuando es necesario.

### ✅ Sistema de prioridades

El bot ahora busca primero en Firebase (datos actualizables) y solo usa FAQs hardcodeados como fallback.

---

## 🧠 Consejos útiles

- ✅ Siempre responde 200 antes de procesar (ACK inmediato)
- ✅ Usa `ConcurrentHashMap` para evitar duplicados (wamid)
- ✅ Meta reintenta hasta 8 veces si tu API no responde bien
- ✅ Los mensajes desde el botón "Probar" no generan duplicados
- ✅ Si dejas de recibir eventos, revisa que el webhook no esté pausado
- ✅ Actualiza información en Firestore sin necesidad de redesplegar el código

---

## 📊 Métricas y Analytics

El sistema registra automáticamente:
- `viewCount` - Cuántas veces se consultó un FAQ
- `helpfulCount` - Cuántas veces fue útil
- `notHelpfulCount` - Cuántas veces no fue útil

Estos datos se pueden usar para mejorar las respuestas.

---

## 🚀 Próximas mejoras

- [ ] Implementar botones interactivos de WhatsApp
- [ ] Sistema de feedback por FAQ
- [ ] Panel de administración web para gestionar FAQs
- [ ] Integración con Google Calendar para recordatorios
- [ ] Análisis de sentimiento en mensajes
- [ ] Soporte multiidioma

---

## 📚 Referencias oficiales

- [Documentación WhatsApp Cloud API](https://developers.facebook.com/docs/whatsapp/cloud-api)
- [Webhook Setup Guide (Meta)](https://developers.facebook.com/docs/graph-api/webhooks)
- [Firebase Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Ngrok Setup](https://ngrok.com/docs)

---

## 👥 Contribuidores

Proyecto desarrollado como parte del programa académico de la Universidad Politécnica.

---

## 📄 Licencia

Este proyecto es de uso académico.
