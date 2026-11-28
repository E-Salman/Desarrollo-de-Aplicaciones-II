package ar.edu.uade.inversorar.presentation.api;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** Cálculo y comparación de ETag (RFC 9110 §8.8.3 y §13.1.2), sin dependencias de JAX-RS. */
public final class Etags {
    private Etags() { }

    /** ETag débil: el resumen identifica el contenido JSON, no los bytes exactos de la respuesta. */
    public static String calcular(byte[] representacion) {
        try {
            byte[] sha = MessageDigest.getInstance("SHA-256").digest(representacion);
            return "W/\"" + Base64.getUrlEncoder().withoutPadding().encodeToString(sha).substring(0, 27) + "\"";
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    /** If-None-Match usa comparación débil: acepta "*", listas separadas por coma y etiquetas con o sin W/. */
    public static boolean coincide(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || etag == null) return false;
        String propio = opaco(etag);
        for (String parte : ifNoneMatch.split(",")) {
            String p = parte.trim();
            if (p.equals("*") || opaco(p).equals(propio)) return true;
        }
        return false;
    }

    private static String opaco(String etag) {
        String e = etag.trim();
        return e.startsWith("W/") ? e.substring(2) : e;
    }
}
