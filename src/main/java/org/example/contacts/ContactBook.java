package org.example.contacts;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ContactBook {
    // nombre en minúsculas -> número E.164 (sin '+')
    private final Map<String, String> contacts = new HashMap<>();

    public ContactBook() {
        // DEMO: añade los tuyos aquí
        contacts.put("juan", "521234567890"); // MX: 52 + 10; si ya tienes 521... manténlo
        contacts.put("asesor", "521111111111");
    }

    public String find(String name) {
        if (name == null) return null;
        return contacts.get(name.trim().toLowerCase());
    }

    public void put(String name, String e164) {
        contacts.put(name.trim().toLowerCase(), e164);
    }
}
