package ar.edu.uade.inversorar.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.*;
import jakarta.security.enterprise.identitystore.*;
import jakarta.servlet.http.*;

@ApplicationScoped
@AutoApplySession
public class Autenticacion implements HttpAuthenticationMechanism {
    @Inject private IdentityStoreHandler identidades;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
                                               HttpMessageContext context) {
        var credential = context.getAuthParameters().getCredential();
        if (credential != null) {
            var result = identidades.validate(credential);
            if (result.getStatus() != CredentialValidationResult.Status.VALID)
                return AuthenticationStatus.SEND_FAILURE;
            // No conservar una conversación Portfolio de otra identidad ni un ID de sesión previo.
            var anterior = request.getSession(false);
            if (anterior != null) anterior.invalidate();
            request.getSession(true);
            return context.notifyContainerAboutLogin(result);
        }
        if (context.isProtected()) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (path.equals("/api") || path.startsWith("/api/")) return context.responseUnauthorized();
            return context.redirect(request.getContextPath() + "/login");
        }
        return context.doNothing();
    }
}
