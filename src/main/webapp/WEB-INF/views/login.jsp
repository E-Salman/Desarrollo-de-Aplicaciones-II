<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="es">

<head>

    <meta charset="UTF-8">

    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>InversorAR - Iniciar sesión</title>

    <link
        rel="stylesheet"
        href="<%= request.getContextPath() %>/assets/auth.css"
    >

</head>

<body>

    <main class="auth-container">

        <div class="brand">

            <div class="brand-icon">📈</div>

            <span>InversorAR</span>

        </div>


        <div class="tabs">

            <button
                type="button"
                class="tab active"
            >
                Iniciar sesión
            </button>

            <button
                type="button"
                class="tab"
                onclick="window.location.href='<%= request.getContextPath() %>/registro'"
            >
                Registrarse
            </button>

        </div>


        <form
            method="post"
            action="<%= request.getContextPath() %>/login"
        >

            <input type="hidden" name="csrf" value="${sessionScope.csrf}">
            <% if ("1".equals(request.getParameter("registrado"))) { %>
                <p>Cuenta creada. Iniciá sesión con tu email y contraseña.</p>
            <% } %>
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


            <label for="password">
                Contraseña
            </label>

            <div class="password-container">

                <input
                    id="password"
                    name="password"
                    type="password"
                    placeholder="••••••••"
                    required
                >

                <button
                    type="button"
                    class="show-password"
                    id="togglePassword"
                    aria-label="Mostrar contraseña"
                >

                    <svg
                        id="eyeIcon"
                        width="18"
                        height="18"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    >
                        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"></path>

                        <circle
                            cx="12"
                            cy="12"
                            r="3"
                        ></circle>
                    </svg>

                </button>

            </div>


            <div class="forgot-container">

                <a href="<%= request.getContextPath() %>/recuperar">
                    ¿Olvidaste tu contraseña?
                </a>

            </div>


            <%
                String error =
                    (String) request.getAttribute("error");

                if (error != null) {
            %>

                <p class="error">
                    <%= error %>
                </p>

            <%
                }
            %>


            <button
                type="submit"
                class="main-button"
            >
                Entrar
            </button>

        </form>

    </main>


    <script>

        const passwordInput =
            document.getElementById("password");

        const togglePassword =
            document.getElementById("togglePassword");

        const eyeIcon =
            document.getElementById("eyeIcon");


        togglePassword.addEventListener(
            "click",
            function () {

                if (passwordInput.type === "password") {

                    passwordInput.type = "text";

                    togglePassword.setAttribute(
                        "aria-label",
                        "Ocultar contraseña"
                    );

                    eyeIcon.innerHTML = `
                        <path d="M3 3l18 18"></path>

                        <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8"></path>

                        <path d="M9.9 4.2A10.8 10.8 0 0 1 12 4c6.5 0 10 8 10 8a18 18 0 0 1-2 3"></path>

                        <path d="M6.6 6.6C3.8 8.5 2 12 2 12s3.5 8 10 8a10 10 0 0 0 5.4-1.6"></path>
                    `;

                } else {

                    passwordInput.type = "password";

                    togglePassword.setAttribute(
                        "aria-label",
                        "Mostrar contraseña"
                    );

                    eyeIcon.innerHTML = `
                        <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"></path>

                        <circle
                            cx="12"
                            cy="12"
                            r="3"
                        ></circle>
                    `;

                }

            }
        );

    </script>

</body>

</html>
