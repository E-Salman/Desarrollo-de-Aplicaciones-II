<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>

<html lang="es">

<head>

    <meta charset="UTF-8">

    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>InversorAR - Recuperar contraseña</title>

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


        <div class="recovery-header">

            <h1>
                Recuperar contraseña
            </h1>

            <p>
                Ingresá tu email y te enviaremos instrucciones
                para restablecer tu contraseña.
            </p>

        </div>


        <form>

            <label for="email">
                Email
            </label>

            <input
                id="email"
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


        <div class="back-login">

            <a href="<%= request.getContextPath() %>/login">
                ← Volver a iniciar sesión
            </a>

        </div>

    </main>

</body>

</html>