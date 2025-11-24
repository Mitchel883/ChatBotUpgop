package org.example.google;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google")
public class GoogleAuthController {

    private final GoogleOAuthService oauth;

    public GoogleAuthController(GoogleOAuthService oauth) {
        this.oauth = oauth;
    }
    //a
    // Paso 1: generar URL de autorización (redirige automáticamente)
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(@RequestParam String phone) {
        String url = oauth.buildAuthUrl(phone);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", url); // Redirección HTTP real

        return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect
    }

    // Paso 2: recibir el "code" desde Google
    @GetMapping("/oauth/callback")
    public String callback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        // state ES el phone
        String phone = state;

        oauth.exchangeCode(phone, code);
        return "Google OAuth completado para " + phone;
    }


}
