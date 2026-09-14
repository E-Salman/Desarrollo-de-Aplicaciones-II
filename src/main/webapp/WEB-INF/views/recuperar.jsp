<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>

<html lang="es">

<head>

    <meta charset="UTF-8">

    <meta
        name="viewport"
        content="width=device-width, initial-scale=1.0"
    >

    <title>
        InversorAR - Recuperar contraseña
    </title>

    <link
        rel="stylesheet"
        href="<%= request.getContextPath() %>/assets/auth.css"
    >

</head>

<body>

    <main class="auth-container">

        <div class="brand">

            <div class="brand-icon">
                📈
            </div>

            <span>
                InversorAR
            </span>

        </div>


        <div class="recovery-header">

            <h1>
                Recuperar contraseña
            </h1>

            <p>
                Ingresá tu email y te enviaremos instrucciones
                para restablecer tu contraseña.
            </p>

        </div>


        <form
            method="post"
            action="<%= request.getContextPath() %>/recuperar"
        >

            <label for="email">
                Email
            </label>

            <input
                id="email"
                name="email"
                type="email"
                placeholder="hola@ejemplo.com"
                required
            >


            <button
                type="submit"
                class="main-button"
            >
                Enviar instrucciones
            </button>

        </form>


        <%
            String error =
                    (String) request.getAttribute("error");

            String mensaje =
                    (String) request.getAttribute("mensaje");

            String enlaceRecuperacion =
                    (String) request.getAttribute(
                            "enlaceRecuperacion"
                    );
        %>


        <% if (error != null) { %>

            <p class="error">
                <%= error %>
            </p>

        <% } %>


        <% if (mensaje != null) { %>

            <p>
                <%= mensaje %>
            </p>

        <% } %>


        <% if (enlaceRecuperacion != null) { %>

            <p>
                <a href="<%= enlaceRecuperacion %>">
                    Restablecer contraseña
                </a>
            </p>

        <% } %>


        <div class="back-login">

            <a href="<%= request.getContextPath() %>/login">
                ← Volver a iniciar sesión
            </a>

        </div>

    </main>

</body>

</html>