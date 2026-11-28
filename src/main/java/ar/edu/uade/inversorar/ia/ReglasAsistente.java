package ar.edu.uade.inversorar.ia;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas del gateway que se aplican en código, antes y después del modelo. No dependen de que el modelo
 * obedezca las instrucciones: son la primera y la última barrera.
 */
public final class ReglasAsistente {
    private ReglasAsistente() { }

    public static final int MAXIMO_PREGUNTA = 500;
    static final int MAXIMO_RESPUESTA = 1500;
    /** Frase del bloque de reglas de las instrucciones (en minúsculas y sin tildes). */
    static final String TITULO_REGLAS = "reglas que no cambian";

    public static final String CONSEJO = "No puedo darte recomendaciones de inversión ni opinar sobre qué comprar o vender. "
            + "Puedo explicarte cómo usar InversorAR, qué significa un término o resumirte noticias de un activo del catálogo.";
    public static final String FUERA_DE_TEMA = "Solo puedo ayudarte con el uso de InversorAR, términos financieros básicos "
            + "y noticias de activos del catálogo.";
    public static final String NO_DISPONIBLE = "El asistente no está disponible en este momento. Probá de nuevo en unos minutos; "
            + "mientras tanto, el resto de la aplicación funciona normalmente.";
    public static final String BLOQUEADA = "No puedo responder esa pregunta. Probá reformularla.";
    public static final String SIN_NOTICIAS = "No encontré noticias de los últimos 7 días sobre %s.";
    public static final String NOTICIAS_NO_DISPONIBLES = "No pude consultar las noticias en este momento. Probá de nuevo en unos minutos.";
    public static final String NO_EN_CATALOGO = "Solo puedo buscar noticias de activos del catálogo de InversorAR. "
            + "Escribí el símbolo o el nombre tal como aparece al comprar (por ejemplo, BTC o Bitcoin).";
    public static final String NOTICIAS_SIN_RESUMEN = "El asistente no pudo resumirlas ahora, pero estas son las noticias encontradas.";

    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]|[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    /** Pedidos de consejo o predicción. Una pregunta de uso ("¿cómo compro?") no coincide. */
    private static final Pattern CONSEJO_PEDIDO = Pattern.compile(
            "\\b(me conviene|conviene (comprar|vender|invertir)|deberia (comprar|vender|invertir)|"
            + "recomenda(s|me|rias)?\\b|recomiend|aconseja|en que (invierto|invertir|me conviene)|donde invierto|"
            + "(?<!lo )que (compro|vendo)\\b|(compro|vendo) (o no|ahora|ya|hoy)\\b|va a (subir|bajar)|(subira|bajara)\\b|"
            + "precio objetivo|vale la pena (comprar|invertir|vender)|es (buena|mala) (inversion|idea)|buen momento para|"
            + "(should i|is it a good time to) (buy|sell|invest))");
    private static final Pattern URL = Pattern.compile("(?i)\\b(https?://|www\\.)\\S+");
    private static final Pattern ENLACE_MARKDOWN = Pattern.compile("\\[([^\\]]{1,200})]\\([^)]*\\)");
    private static final Pattern SIMBOLO_O_NOMBRE = Pattern.compile("[\\p{L}\\p{N} .&'/-]{1,60}");

    /** Quita caracteres de control y de formato invisibles (sirven para esconder instrucciones). */
    public static String sinControl(String texto) {
        return texto == null ? "" : CONTROL.matcher(texto).replaceAll("");
    }

    /** Pregunta limpia, o IllegalArgumentException si está vacía o es demasiado larga. */
    public static String pregunta(String texto) {
        String limpia = ESPACIOS.matcher(sinControl(texto)).replaceAll(" ").trim();
        if (limpia.isEmpty()) throw new IllegalArgumentException("Escribí una pregunta.");
        if (limpia.length() > MAXIMO_PREGUNTA)
            throw new IllegalArgumentException("La pregunta puede tener hasta " + MAXIMO_PREGUNTA + " caracteres.");
        return limpia;
    }

    /** Nombre o símbolo de un activo para buscar en el catálogo, o IllegalArgumentException. */
    public static String activo(String texto) {
        String limpio = ESPACIOS.matcher(sinControl(texto)).replaceAll(" ").trim();
        if (!SIMBOLO_O_NOMBRE.matcher(limpio).matches() || limpio.length() < 2)
            throw new IllegalArgumentException("Escribí el símbolo o el nombre de un activo (por ejemplo, BTC o Bitcoin).");
        return limpio;
    }

    public static boolean pideConsejo(String pregunta) { return CONSEJO_PEDIDO.matcher(comparable(pregunta)).find(); }

    /** Minúsculas y sin tildes, para comparar. */
    static String comparable(String texto) {
        return MARCAS.matcher(Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("");
    }

    /**
     * Prepara la respuesta del modelo para mostrarla: sin enlaces (los únicos enlaces que ve el usuario son los
     * de la fuente de noticias), sin caracteres de control y con largo máximo. Devuelve null si la respuesta
     * contiene la marca secreta de las instrucciones, es decir, si el modelo las filtró.
     */
    public static String respuesta(String texto, String marcaSecreta) {
        if (texto == null) return null;
        if (marcaSecreta != null && texto.contains(marcaSecreta)) return null;
        if (comparable(texto).contains(TITULO_REGLAS)) return null; // copió el bloque de reglas sin la marca
        String limpia = ENLACE_MARKDOWN.matcher(sinControl(texto)).replaceAll("$1");
        limpia = URL.matcher(limpia).replaceAll("").replaceAll("[ \\t]{2,}", " ").trim();
        if (limpia.length() > MAXIMO_RESPUESTA) limpia = limpia.substring(0, MAXIMO_RESPUESTA - 1).trim() + "…";
        return limpia.isEmpty() ? null : limpia;
    }
}
