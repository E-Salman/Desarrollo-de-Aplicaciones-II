package ar.edu.uade.inversorar.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokensAccesoTest {
    private final TokensAcceso tokens = new TokensAcceso(new byte[32]);

    @Test void conservaIdentidadRolYExpiraEnQuinceMinutos() {
        var jwt = tokens.verificar(tokens.emitir("ana@example.com", Set.of("USUARIO"), "sesion", Instant.now()));
        assertEquals("ana@example.com", jwt.getSubject());
        assertEquals(List.of("USUARIO"), jwt.getClaim("roles").asList(String.class));
        assertEquals("sesion", jwt.getId());
        assertEquals(900, jwt.getExpiresAtAsInstant().getEpochSecond() - jwt.getIssuedAtAsInstant().getEpochSecond());
    }

    @Test void rechazaFirmaDistinta() {
        var otraClave = new byte[32];
        otraClave[0] = 1;
        var token = new TokensAcceso(otraClave).emitir("ana@example.com", Set.of("USUARIO"), "sesion", Instant.now());
        assertThrows(JWTVerificationException.class, () -> tokens.verificar(token));
    }

    @Test void rechazaExpiradoYAlgoritmoNone() {
        var expirado = tokens.emitir("ana@example.com", Set.of("USUARIO"), "sesion", Instant.now().minusSeconds(901));
        var sinFirma = JWT.create().withSubject("ana@example.com").sign(Algorithm.none());
        assertThrows(JWTVerificationException.class, () -> tokens.verificar(expirado));
        assertThrows(JWTVerificationException.class, () -> tokens.verificar(sinFirma));
    }
}
