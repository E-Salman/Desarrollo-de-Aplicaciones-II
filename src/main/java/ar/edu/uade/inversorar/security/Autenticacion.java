package ar.edu.uade.inversorar.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;

@ApplicationScoped
public class Autenticacion implements HttpAuthenticationMechanism {
    @Inject private IdentityStoreHandler identidades;
    @Inject private TokensAcceso tokens;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
                                               HttpMessageContext context) {
        var credential = context.getAuthParameters().getCredential();
        if (credential != null) {
            var result = identidades.validate(credential);
            if (result.getStatus() != CredentialValidationResult.Status.VALID)
                return AuthenticationStatus.SEND_FAILURE;
            // Rotar la sesión y la conversación Portfolio al iniciar con credenciales.
            var anterior = request.getSession(false);
            if (anterior != null) anterior.invalidate();
            request.getSession(true);
            tokens.iniciar(request, response, result.getCallerPrincipal().getName(), result.getCallerGroups());
            return context.notifyContainerAboutLogin(result);
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean api = path.equals("/api") || path.startsWith("/api/");
        String sitio = request.getHeader("Sec-Fetch-Site");
        var jwt = tokens.autenticar(request);
        if (jwt != null) {
            if (api && !"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())
                    && sitio != null && !"same-origin".equals(sitio)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return AuthenticationStatus.SEND_FAILURE;
            }
            return context.notifyContainerAboutLogin(jwt.getSubject(),
                    Set.copyOf(jwt.getClaim("roles").asList(String.class)));
        }

        if (context.isProtected()) {
            if (api) return context.responseUnauthorized();
            return context.redirect(request.getContextPath() + "/login");
        }
        return context.doNothing();
    }
}
