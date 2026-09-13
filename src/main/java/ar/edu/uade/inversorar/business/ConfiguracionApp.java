package ar.edu.uade.inversorar.business;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.Properties;

@ApplicationScoped
public class ConfiguracionApp {
    private boolean catalogoMysql;
    @PostConstruct public void iniciar() {
        var properties = new Properties();
        try (var input = getClass().getResourceAsStream("/app.properties")) {
            if (input != null) properties.load(input);
        } catch (IOException e) { throw new IllegalStateException("No se pudo leer la configuración", e); }
        catalogoMysql = Boolean.parseBoolean(properties.getProperty("catalogo.mysql", "false"));
    }
    public boolean catalogoMysql() { return catalogoMysql; }
}
