package ar.edu.uade.inversorar.presentation;

import ar.edu.uade.inversorar.business.CompraService;
import ar.edu.uade.inversorar.business.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import jakarta.ws.rs.ext.RuntimeDelegate;
import jakarta.ws.rs.core.Response;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortfolioSesionTest {
    private MockedStatic<RuntimeDelegate> jaxrs;
    @BeforeEach void respuestaHttpSinContenedor() {
        var delegate=mock(RuntimeDelegate.class);
        var builder=mock(Response.ResponseBuilder.class,RETURNS_SELF);
        var response=mock(Response.class);
        when(response.getStatus()).thenReturn(403);
        when(response.getStatusInfo()).thenReturn(Response.Status.FORBIDDEN);
        when(builder.build()).thenReturn(response);
        when(delegate.createResponseBuilder()).thenReturn(builder);
        jaxrs=mockStatic(RuntimeDelegate.class);
        jaxrs.when(RuntimeDelegate::getInstance).thenReturn(delegate);
    }
    @AfterEach void liberarRespuestaHttp() { jaxrs.close(); }
    private void inyectar(Object objeto, String campo, Object valor) throws Exception {
        var f=objeto.getClass().getDeclaredField(campo); f.setAccessible(true); f.set(objeto,valor);
    }
    @Test void identidadVinculadaYReferenciaUnica() throws Exception {
        var sesion=new PortfolioSesion(); var servicio=mock(PortfolioService.class);
        inyectar(sesion,"servicio",servicio);
        assertThrows(ForbiddenException.class,()->sesion.servicio(null));
        assertSame(servicio,sesion.servicio("ana"));
        assertSame(servicio,sesion.servicio("ana"));
        assertThrows(ForbiddenException.class,()->sesion.servicio("bruno"));
        sesion.destruir(); verify(servicio).cerrar();
    }
    @Test void cambioDeIdentidadNoPersisteCompraAntesDelRechazo() throws Exception {
        var sesion=new PortfolioSesion(); sesion.servicio("ana");
        var compra=mock(CompraService.class); var http=mock(HttpServletRequest.class);
        when(http.getUserPrincipal()).thenReturn(()->"bruno");
        var resource=new CompraResource(); inyectar(resource,"compraService",compra);
        inyectar(resource,"portfolioSesion",sesion); inyectar(resource,"httpRequest",http);
        assertThrows(ForbiddenException.class,()->resource.comprar(null));
        verifyNoInteractions(compra);
    }
}
