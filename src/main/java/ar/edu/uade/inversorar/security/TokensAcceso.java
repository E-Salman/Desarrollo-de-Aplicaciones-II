package ar.edu.uade.inversorar.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class TokensAcceso {
    static final String EMISOR = "InversorAR";
    static final String AUDIENCIA = "inversorar-web";
    static final int VIGENCIA = 15 * 60;
    private static final String COOKIE = "INVERSORAR_ACCESS";
    private static final String SESION = TokensAcceso.class.getName() + ".jti";

    // Un despliegue local, con sesiones propias. Un redespliegue invalida las credenciales anteriores.
    private final Algorithm algoritmo;
    private final JWTVerifier verificador;

    public TokensAcceso() {
        this(nuevaClave());
    }

    TokensAcceso(byte[] clave) {
        algoritmo = Algorithm.HMAC256(clave);
        verificador = JWT.require(algoritmo).withIssuer(EMISOR).withAudience(AUDIENCIA)
                .withClaimPresence("exp").withClaimPresence("iat").withClaimPresence("sub")
                .withClaimPresence("jti").withClaimPresence("roles").build();
    }

    private static byte[] nuevaClave() {
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        return clave;
    }

    void iniciar(HttpServletRequest request, HttpServletResponse response, String email, Set<String> grupos) {
        String id = UUID.randomUUID().toString();
        request.getSession().setAttribute(SESION, id);
        agregarCookie(request, response, emitir(email, grupos, id, Instant.now()), VIGENCIA);
    }

    String emitir(String email, Set<String> grupos, String id, Instant ahora) {
        return JWT.create().withIssuer(EMISOR).withAudience(AUDIENCIA).withSubject(email)
                .withJWTId(id).withIssuedAt(ahora).withExpiresAt(ahora.plusSeconds(VIGENCIA))
                .withArrayClaim("roles", grupos.toArray(String[]::new)).sign(algoritmo);
    }

    DecodedJWT verificar(String token) {
        return verificador.verify(token);
    }

    DecodedJWT autenticar(HttpServletRequest request) {
        var sesion = request.getSession(false);
        if (sesion == null || !(sesion.getAttribute(SESION) instanceof String id) || request.getCookies() == null)
            return null;
        var cookie = Arrays.stream(request.getCookies()).filter(c -> COOKIE.equals(c.getName())).findFirst();
        if (cookie.isEmpty()) return null;
        try {
            var jwt = verificar(cookie.get().getValue());
            return id.equals(jwt.getId()) ? jwt : null;
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    public void cerrar(HttpServletRequest request, HttpServletResponse response) {
        agregarCookie(request, response, "", 0);
    }

    private void agregarCookie(HttpServletRequest request, HttpServletResponse response, String valor, int edad) {
        var cookie = new Cookie(COOKIE, valor);
        cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Strict");
        cookie.setMaxAge(edad);
        response.addCookie(cookie);
    }
}
