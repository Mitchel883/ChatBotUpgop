ChatBot Unipoli — WhatsApp Cloud API + Spring Boot

Proyecto universitario que implementa un chatbot inteligente para WhatsApp utilizando la API de Meta (WhatsApp Cloud API) y un backend en Spring Boot.
El sistema permite automatizar respuestas, generar recordatorios y comunicarse con asesores a través del chat.

🚀 Características principales

📅 Recordatorios personalizados
Ejemplo:

Recuérdame mañana a las 4pm que debo enviar la tarea


El bot registra el recordatorio y envía un mensaje a la hora indicada.

🤖 Automatización de mensajes
Ejemplo:

Envía mensaje a Juan que debe enviar la tarea a las 3pm


El bot toma el contacto y programa el envío automático del mensaje.

🏫 Consulta de información institucional
Ejemplo:

¿Cómo saco mi constancia?


El chatbot responde con información oficial y, si no entiende, ofrece conectarte con un asesor.

🧭 Menú interactivo de opciones
El bot puede mostrar un menú con botones (en desarrollo) para guiar al usuario por opciones predefinidas.

⚙️ Arquitectura general
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
        │      ├─ Procesa evento
        │      ├─ Genera respuesta
        │      └─ Llama al servicio CloudApiSender
        ▼
[CloudApiSender → Meta Graph API]
        │
        ▼
[WhatsApp → Usuario]

🧩 Configuración paso a paso
1️⃣ Crear la App en Meta Developers

Entra en https://developers.facebook.com

Crea una nueva App tipo “Negocio” o “Otro”

Agrega el producto “WhatsApp”

Copia:

Token de acceso

Phone Number ID

WhatsApp Business Account ID

2️⃣ Configurar el Webhook

En Productos → Webhooks, selecciona WhatsApp Business Account

Configura:

URL de devolución de llamada:

https://<tu-ngrok>.ngrok-free.app/webhooks/whatsapp


Token de verificación:

unipoli-verify-token


Clic en Verificar y guardar

Suscríbete al campo messages

3️⃣ Levantar tu API local
mvn clean spring-boot:run


Por defecto corre en:

http://localhost:8080


Luego levanta ngrok:

ngrok http 8080


Copia la URL HTTPS y pégala como callback en Meta.

4️⃣ Validación del Webhook

Meta hará un GET a tu endpoint con:

hub.mode=subscribe
hub.verify_token=unipoli-verify-token
hub.challenge=12345


Tu API debe devolver 12345 (el challenge) con código 200 OK.

5️⃣ Flujo de Mensajes

Usuario envía un mensaje por WhatsApp.

Meta lo reenvía vía POST a tu endpoint.

Tu API procesa el JSON recibido.

Se genera una respuesta con CloudApiSender.

Meta entrega el mensaje de vuelta al usuario.

🧱 Estructura del proyecto
src/main/java/mx/unipoli/chatbot/
│
├── MainApplication.java         → Clase principal Spring Boot
├── controller/
│   └── CloudApiWebhookController.java  → Maneja eventos entrantes
├── service/
│   └── CloudApiSender.java      → Envío de mensajes a Graph API
└── util/
    └── ChatLogic.java           → Lógica para respuestas inteligentes

🔐 Variables de entorno

Crea un archivo .env (o configúralas manualmente):

Variable	Descripción
WA_VERIFY_TOKEN	Token de verificación del webhook
WA_ACCESS_TOKEN	Token de acceso de Meta
WA_PHONE_NUMBER_ID	ID del número de WhatsApp
WA_API_URL	https://graph.facebook.com/v20.0

Ejemplo:

WA_VERIFY_TOKEN=unipoli-verify-token
WA_ACCESS_TOKEN=EAAxxxxx...
WA_PHONE_NUMBER_ID=821827761015750
WA_API_URL=https://graph.facebook.com/v20.0

🧰 Pruebas

Para probar manualmente el webhook:

curl -X POST https://<tu-ngrok>.ngrok-free.app/webhooks/whatsapp \
  -H "Content-Type: application/json" \
  -d '{"object":"whatsapp_business_account","entry":[{"changes":[{"value":{"messages":[{"from":"528717558771","id":"wamid.TEST1","type":"text","text":{"body":"Hola"}}]}}]}]}'

🧩 Errores comunes
Error	Causa	Solución
(#131030) Recipient phone number not in allowed list	Número no autorizado	Agrega el número en Meta (lista de prueba)
Webhook no recibe nada	URL vieja de ngrok o suscripción incorrecta	Rehacer “Verificar y guardar”
Mensajes duplicados	Meta reintenta al no recibir 200 OK	Responder 200 antes de procesar
Falla de envío	Token vencido	Regenerar token en Meta y actualizar variable
🧠 Consejos útiles

Siempre responde 200 antes de procesar (ACK inmediato)

Usa ConcurrentHashMap o Redis para evitar duplicados (wamid)

Meta reintenta hasta 8 veces si tu API no responde bien

Los mensajes desde el botón “Probar” no generan duplicados

Si dejas de recibir eventos, revisa que el webhook no esté pausado

📚 Referencias oficiales

Documentación WhatsApp Cloud API

Webhook Setup Guide (Meta)

Ngrok Setup# ChatBotUpgop
