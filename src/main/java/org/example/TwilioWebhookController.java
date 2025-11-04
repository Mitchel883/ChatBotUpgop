// src/main/java/edu/uni/bot/TwilioWebhookController.java
package org.example;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.TwiMLException;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;

@RestController
@RequestMapping("/webhooks/twilio")
public class TwilioWebhookController {

    @PostMapping(
            value = "/whatsapp",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public ResponseEntity<String> onIncoming(@RequestParam MultiValueMap<String, String> form)
            throws TwiMLException {

        String from = val(form, "From");     // e.g. whatsapp:+52...
        String body = val(form, "Body");     // texto que envió el usuario
        String name = val(form, "ProfileName"); // a veces viene en WhatsApp

        // Router ultra simple (stub de intents):
        String reply;
        String text = body == null ? "" : body.trim().toLowerCase();

        if (text.startsWith("ping")) {
            reply = "pong ✅";
        } else if (text.startsWith("ayuda") || text.startsWith("menu")) {
            reply = "Soy el bot de la Uni.\nComandos:\n• ping\n• ejemplo recordatorio: \"recuérdame mañana 4pm enviar tarea\"\n• ejemplo enviar: \"envía a Juan: recuerda la tarea a las 3pm\"\n• ejemplo faq: \"¿Cómo saco la constancia?\"";
        } else {
            reply = String.format("Hola %s, recibí: \"%s\" \n(Escribe \"ayuda\" para ver comandos)",
                    (name.isEmpty() ? "👋" : name), body);
        }

        MessagingResponse twiml = new MessagingResponse.Builder()
                .message(new Message.Builder()
                        .body(new Body.Builder(reply).build())
                        .build())
                .build();

        return ResponseEntity.ok(twiml.toXml());
    }

    private static String val(MultiValueMap<String, String> form, String key) {
        return form.getFirst(key) == null ? "" : form.getFirst(key);
    }
    @GetMapping("/ping")
    public String ping() { return "ok"; }
}
