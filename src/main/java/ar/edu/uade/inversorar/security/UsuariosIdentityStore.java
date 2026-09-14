package ar.edu.uade.inversorar.security;

import ar.edu.uade.inversorar.business.UsuarioService;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.*;
import java.util.Set;

/** Usa la misma unidad JPA (y datasource del perfil) que el registro. */
@ApplicationScoped
public class UsuariosIdentityStore implements IdentityStore {
    @EJB private UsuarioService usuarios;

    public CredentialValidationResult validate(UsernamePasswordCredential credential) {
        var usuario = usuarios.login(credential.getCaller(), credential.getPasswordAsString());
        return usuario == null ? CredentialValidationResult.INVALID_RESULT
                : new CredentialValidationResult(usuario.getEmail(), Set.of("USUARIO"));
    }
}
