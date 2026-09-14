package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.*;
import ar.edu.uade.inversorar.security.Passwords;
import jakarta.ejb.EJBException;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistroTest {
    UsuarioRepository repo = mock(UsuarioRepository.class);
    Passwords passwords = mock(Passwords.class);
    UsuarioServiceBean bean() throws Exception {
        var bean = new UsuarioServiceBean();
        ComponentesTest.inyectar(bean, "usuarioRepository", repo);
        ComponentesTest.inyectar(bean, "passwords", passwords);
        return bean;
    }
    @Test void registroNormalizaYGuardaHash() throws Exception {
        when(passwords.generar("secreto123")).thenReturn("hash-con-salt");
        var u = bean().registrar(" Ana ", " Perez ", " ANA@Example.COM ", "secreto123");
        assertEquals("ana@example.com", u.getEmail());
        assertEquals("hash-con-salt", u.getPassword());
        verify(repo).guardar(u);
    }
    @Test void validaAntesDePersistir() throws Exception {
        assertThrows(RegistroException.class, () -> bean().registrar("Ana", "Perez", "invalido", "secreto123"));
        assertThrows(RegistroException.class, () -> bean().registrar("Ana", "Perez", "a@b.com", "corta"));
        verifyNoInteractions(repo, passwords);
    }
    @Test void duplicadoConcurrenteSeTraduceDespuesDelRollback() throws Exception {
        when(repo.buscarPorEmail("a@b.com")).thenReturn(Optional.empty(), Optional.of(mock(Usuario.class)));
        doThrow(new EJBException("duplicate")).when(repo).guardar(any());
        assertNull(bean().registrar("Ana", "Perez", "a@b.com", "secreto123"));
    }
    @Test void loginNoAceptaMarcadorDePrueba() throws Exception {
        when(repo.buscarPorEmail("a@b.com")).thenReturn(Optional.of(new Usuario("Ana", "Perez", "a@b.com", "marcador")));
        assertNull(bean().login(" A@B.COM ", "marcador"));
        verify(passwords).verificar("marcador", "marcador");
    }
    @Test void carteraVinculadaConservaPropietarioAnterior() throws Exception {
        var bean = new PortfolioActualBean();
        var contexto = mock(jakarta.ejb.SessionContext.class);
        var portfolios = mock(PortfolioRepository.class);
        var usuario = mock(Usuario.class);
        var cartera = new Portfolio("Histórica", "principal-anterior");
        when(contexto.isCallerInRole("USUARIO")).thenReturn(true);
        when(contexto.getCallerPrincipal()).thenReturn(() -> "a@b.com");
        when(repo.buscarPorEmail("a@b.com")).thenReturn(Optional.of(usuario));
        when(usuario.getId()).thenReturn(7L);
        when(portfolios.porUsuarioId(7L)).thenReturn(Optional.of(cartera));
        ComponentesTest.inyectar(bean, "contexto", contexto);
        ComponentesTest.inyectar(bean, "portfolios", portfolios);
        ComponentesTest.inyectar(bean, "usuarios", repo);
        assertSame(cartera, bean.obtener());
        verify(portfolios, never()).porPropietario(any());
        verify(portfolios, never()).guardar(any());
    }
}
