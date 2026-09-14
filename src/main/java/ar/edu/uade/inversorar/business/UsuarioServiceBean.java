package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.Usuario;
import ar.edu.uade.inversorar.data.UsuarioRepository;
import ar.edu.uade.inversorar.data.PasswordResetToken;
import ar.edu.uade.inversorar.data.PasswordResetTokenRepository;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJBException;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import ar.edu.uade.inversorar.security.Passwords;
import java.util.Locale;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class UsuarioServiceBean implements UsuarioService {
    @Inject private Passwords passwords;

    private String normalizarEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    @EJB
    private UsuarioRepository usuarioRepository;

    @EJB
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public Usuario login(String email, String password) {

        Optional<Usuario> usuarioEncontrado =
                usuarioRepository.buscarPorEmail(normalizarEmail(email));

        if (usuarioEncontrado.isEmpty()) {
            return null;
        }

        Usuario usuario = usuarioEncontrado.get();

        if (!passwords.verificar(password, usuario.getPassword())) {
            return null;
        }

        return usuario;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Usuario registrar(
            String nombre,
            String apellido,
            String email,
            String password
    ) {
        email = normalizarEmail(email);
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 100
                || apellido == null || apellido.isBlank() || apellido.strip().length() > 100)
            throw new RegistroException("Completá nombre y apellido (hasta 100 caracteres cada uno).");
        if (email.length() > 255 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new RegistroException("Ingresá un email válido.");
        if (password == null || password.length() < 8 || password.length() > 128)
            throw new RegistroException("La contraseña debe tener entre 8 y 128 caracteres.");

        Optional<Usuario> usuarioExistente =
                usuarioRepository.buscarPorEmail(email);

        if (usuarioExistente.isPresent()) {
            return null;
        }

        Usuario nuevoUsuario =
                new Usuario(
                        nombre.strip(),
                        apellido.strip(),
                        email,
                        passwords.generar(password)
                );

        try {
            usuarioRepository.guardar(nuevoUsuario);
        } catch (EJBException e) {
            // La inserción ya terminó/retrocedió en una transacción independiente.
            if (usuarioRepository.buscarPorEmail(email).isPresent()) return null;
            throw e;
        }

        return nuevoUsuario;
    }

    @Override
    public String generarTokenRecuperacion(String email) {

        Optional<Usuario> usuarioEncontrado =
                usuarioRepository.buscarPorEmail(email);

        if (usuarioEncontrado.isEmpty()) {
            return null;
        }

        Usuario usuario = usuarioEncontrado.get();

        passwordResetTokenRepository.eliminarPorUsuario(
                usuario.getId()
        );

        String token = UUID.randomUUID().toString();

        LocalDateTime vencimiento =
                LocalDateTime.now().plusMinutes(30);

        PasswordResetToken passwordResetToken =
                new PasswordResetToken(
                        usuario.getId(),
                        token,
                        vencimiento
                );

        passwordResetTokenRepository.guardar(
                passwordResetToken
        );

        return token;
    }
}
