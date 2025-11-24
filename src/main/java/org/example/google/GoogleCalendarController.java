package org.example.google;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/google/calendar")
public class GoogleCalendarController {
    //a
    @Autowired
    private GoogleCalendarService calendarService;

    @GetMapping("/create-test")
    public ResponseEntity<?> createEventTest(@RequestParam String phone) {
        try {
            String link = calendarService.crearEventoPrueba(phone);
            return ResponseEntity.ok("Evento creado: " + link);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creando evento: " + e.getMessage());
        }
    }
}
