package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.Usuario;
import ar.edu.uade.inversorar.data.UsuarioRepository;
import ar.edu.uade.inversorar.data.PasswordResetToken;
import ar.edu.uade.inversorar.data.PasswordResetTokenRepository;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Stateless
public class UsuarioServiceBean implements UsuarioService {

    @EJB
    private UsuarioRepository usuarioRepository;

    @EJB
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public Usuario login(String email, String password) {

        Optional<Usuario> usuarioEncontrado =
                usuarioRepository.buscarPorEmail(email);

        if (usuarioEncontrado.isEmpty()) {
            return null;
        }

        Usuario usuario = usuarioEncontrado.get();

        if (!usuario.getPassword().equals(password)) {
            return null;
        }

        return usuario;
    }

    @Override
    public Usuario registrar(
            String nombre,
            String apellido,
            String email,
            String password
    ) {

        Optional<Usuario> usuarioExistente =
                usuarioRepository.buscarPorEmail(email);

        if (usuarioExistente.isPresent()) {
            return null;
        }

        Usuario nuevoUsuario =
                new Usuario(
                        nombre,
                        apellido,
                        email,
                        password
                );

        usuarioRepository.guardar(nuevoUsuario);

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