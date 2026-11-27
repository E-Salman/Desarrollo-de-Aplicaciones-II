package ar.edu.uade.inversorar.integracion;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;

/**
 * Lector de XML endurecido contra XXE (XML External Entity). Una respuesta con DOCTYPE se rechaza
 * antes de leer nada: así no se pueden declarar entidades que lean archivos locales o hagan pedidos a
 * otros servidores, ni expandir entidades en cadena ("billion laughs"). Recomendación OWASP para JAXP.
 */
public final class XmlSeguro {
    private XmlSeguro() { }

    public static Document leer(String xml) throws XmlInvalido {
        try {
            return constructor().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new XmlInvalido("XML rechazado: " + e.getMessage(), e);
        }
    }

    static DocumentBuilder constructor() throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Capa extra (JAXP 1.5). Algunos parsers que provee el servidor (Xerces en WildFly) no la reconocen;
        // la protección efectiva es disallow-doctype-decl, que es obligatoria y arriba corta si no se puede activar.
        for (String atributo : new String[]{XMLConstants.ACCESS_EXTERNAL_DTD, XMLConstants.ACCESS_EXTERNAL_SCHEMA}) {
            try { f.setAttribute(atributo, ""); } catch (IllegalArgumentException noSoportado) { /* opcional */ }
        }
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        DocumentBuilder b = f.newDocumentBuilder();
        // Sin el "[Fatal Error]" por consola del manejador por defecto: el error se informa con la excepción.
        b.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
            @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
        });
        return b;
    }

    /** Escapa un valor antes de insertarlo como texto en un XML. */
    public static String escapar(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static class XmlInvalido extends Exception {
        public XmlInvalido(String mensaje, Throwable causa) { super(mensaje, causa); }
    }
}
