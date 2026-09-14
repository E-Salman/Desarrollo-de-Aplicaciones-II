package ar.edu.uade.inversorar.security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import java.util.Map;

@ApplicationScoped
public class Passwords {
    @Inject private Pbkdf2PasswordHash hash;

    @PostConstruct
    public void iniciar() {
        hash.initialize(Map.of("Pbkdf2PasswordHash.Algorithm", "PBKDF2WithHmacSHA256",
                "Pbkdf2PasswordHash.Iterations", "600000",
                "Pbkdf2PasswordHash.SaltSizeBytes", "32",
                "Pbkdf2PasswordHash.KeySizeBytes", "32"));
    }

    public String generar(String password) { return hash.generate(password.toCharArray()); }

    public boolean verificar(String password, String encoded) {
        if (password == null || password.length() > 128 || encoded == null) return false;
        try { return hash.verify(password.toCharArray(), encoded); }
        catch (IllegalArgumentException e) { return false; } // Marcadores antiguos no son credenciales.
    }
}
