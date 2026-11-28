// Panel del asistente de IA (POST /api/asistente). Todo el texto que llega del servidor se muestra con
// textContent (nunca como HTML) y los únicos enlaces son los de la fuente de noticias, siempre https.
(() => {
  const $ = id => document.getElementById(id);
  const dialogo = $("asistente"), abrir = $("abrirAsistente");
  if (!dialogo || !abrir) return;
  const contexto = window.APP_CONTEXT || location.pathname.split("/").slice(0, 2).join("/");
  const api = contexto + "/api/asistente";
  const textos = {
    ayuda: { descripcion: "Preguntá cómo usar InversorAR o qué significa un término.", etiqueta: "Tu pregunta",
             placeholder: "¿Cómo registro una venta?" },
    noticias: { descripcion: "Resumen de noticias de los últimos 7 días sobre un activo del catálogo.",
                etiqueta: "¿Qué querés saber? (opcional)", placeholder: "Resumí las noticias más importantes." }
  };
  const sugerencias = {
    ayuda: ["¿Cómo registro una compra?", "¿Cómo registro una venta?", "¿Por qué mi orden quedó rechazada?",
            "¿Cómo cancelo una orden pendiente?", "¿Qué es un CEDEAR?", "¿Qué es una stablecoin?",
            "¿Qué significa patrimonio total?", "¿Para qué sirve el simulador?"],
    noticias: ["Resumí las noticias más importantes.", "¿Qué pasó con el precio esta semana?",
               "¿Hay noticias sobre regulación?", "¿Hay alguna noticia negativa?",
               "Explicame en simple la noticia principal.", "¿Qué fuentes hablan del tema?"]
  };
  let modo = "ayuda", estadoConsultado = false;

  function mostrarSugerencias() {
    const zona = $("asistenteSugerencias");
    zona.replaceChildren(...sugerencias[modo].map(texto => {
      const boton = document.createElement("button");
      boton.type = "button"; boton.className = "asistente-sugerencia"; boton.textContent = texto;
      boton.addEventListener("click", () => {
        const campo = $("asistentePregunta");
        campo.value = texto;
        campo.dispatchEvent(new Event("input"));
        // Carga la pregunta para editarla o enviarla; en noticias, primero hace falta el activo.
        (modo === "noticias" && !$("asistenteActivo").value.trim() ? $("asistenteActivo") : $("enviarAsistente")).focus();
      });
      return boton;
    }));
  }

  // Autocompletar el activo con el catálogo (GET /api/catalogo?q=), con espera para no consultar en cada tecla.
  // Solo se acepta un instrumento del catálogo: el nombre o el símbolo de uno de los resultados.
  let esperaCatalogo = null, ultimaBusqueda = "";
  const validos = new Set();
  const clave = texto => texto.trim().toLowerCase();
  async function buscarCatalogo(texto) {
    const r = await fetch(contexto + "/api/catalogo?q=" + encodeURIComponent(texto), { headers: { Accept: "application/json" } });
    if (!r.ok) throw new Error("catálogo " + r.status);
    const lista = await r.json();
    for (const i of lista) { if (i.nombre) validos.add(clave(i.nombre)); if (i.simbolo) validos.add(clave(i.simbolo)); }
    return lista;
  }
  function sugerirActivos() {
    clearTimeout(esperaCatalogo);
    $("asistenteActivo").removeAttribute("aria-invalid");
    const texto = $("asistenteActivo").value.trim();
    if (texto.length < 2 || texto === ultimaBusqueda) return;
    esperaCatalogo = setTimeout(async () => {
      ultimaBusqueda = texto;
      try {
        const vistos = new Set();
        const opciones = (await buscarCatalogo(texto)).filter(i => i.nombre && !vistos.has(i.nombre) && vistos.add(i.nombre)).slice(0, 8)
          .map(i => { const o = document.createElement("option"); o.value = i.nombre; o.label = i.simbolo; return o; });
        $("asistenteActivos").replaceChildren(...opciones);
      } catch (e) { /* sin sugerencias */ }
    }, 300);
  }
  // true si el texto es exactamente un instrumento del catálogo (consulta el catálogo si todavía no se vio).
  async function enCatalogo(texto) {
    if (validos.has(clave(texto))) return true;
    try { await buscarCatalogo(texto); } catch (e) { return true; } // sin catálogo, decide el servidor
    return validos.has(clave(texto));
  }

  function elegirModo(nuevo) {
    modo = nuevo;
    for (const pestania of document.querySelectorAll(".asistente-pestanias [role=tab]")) {
      const activa = pestania.dataset.modo === modo;
      pestania.setAttribute("aria-selected", String(activa));
      pestania.tabIndex = activa ? 0 : -1;
    }
    $("formAsistente").setAttribute("aria-labelledby", modo === "ayuda" ? "pestaniaAyuda" : "pestaniaNoticias");
    $("campoActivo").hidden = modo !== "noticias";
    $("asistenteDescripcion").textContent = textos[modo].descripcion;
    $("etiquetaPregunta").textContent = textos[modo].etiqueta;
    $("asistentePregunta").placeholder = textos[modo].placeholder;
    $("asistenteError").textContent = "";
    mostrarSugerencias();
    (modo === "noticias" ? $("asistenteActivo") : $("asistentePregunta")).focus();
  }

  async function consultarEstado() {
    if (estadoConsultado) return;
    estadoConsultado = true;
    try {
      const r = await fetch(api + "/estado", { headers: { Accept: "application/json" } });
      if (r.ok) $("asistenteModoPrueba").hidden = (await r.json()).proveedor !== "SIMULADO";
    } catch (e) { estadoConsultado = false; }
  }

  function mostrar(respuesta) {
    const zona = $("asistenteRespuesta");
    zona.replaceChildren();
    const texto = document.createElement("p");
    texto.className = "asistente-texto";
    texto.textContent = respuesta.respuesta;
    zona.append(texto);
    const noticias = (respuesta.noticias || []).filter(n => typeof n.url === "string" && n.url.startsWith("https://"));
    if (noticias.length) {
      const titulo = document.createElement("h3");
      titulo.textContent = "Noticias";
      const lista = document.createElement("ul");
      lista.className = "asistente-noticias";
      for (const n of noticias) {
        const item = document.createElement("li");
        const enlace = document.createElement("a");
        enlace.href = n.url; enlace.target = "_blank"; enlace.rel = "noopener noreferrer";
        enlace.textContent = n.titulo;
        const detalle = document.createElement("small");
        detalle.textContent = [n.fuente, n.fecha].filter(Boolean).join(" · ");
        item.append(enlace, detalle);
        lista.append(item);
      }
      zona.append(titulo, lista);
    }
    if (respuesta.origen === "RESPALDO") {
      const aviso = document.createElement("small");
      aviso.className = "asistente-origen";
      aviso.textContent = "Respuesta de respaldo: el servicio externo no respondió a tiempo.";
      zona.append(aviso);
    }
    zona.hidden = false;
  }

  async function preguntar(evento) {
    evento.preventDefault();
    const pregunta = $("asistentePregunta").value.trim(), activo = $("asistenteActivo").value.trim();
    $("asistenteError").textContent = "";
    if (modo === "ayuda" && !pregunta) { $("asistenteError").textContent = "Escribí una pregunta."; $("asistentePregunta").focus(); return; }
    if (modo === "noticias" && !activo) { $("asistenteError").textContent = "Elegí un activo del catálogo."; $("asistenteActivo").focus(); return; }
    if (modo === "noticias" && !(await enCatalogo(activo))) {
      $("asistenteError").textContent = "«" + activo + "» no está en el catálogo. Escribí parte del nombre y elegí una opción de la lista.";
      $("asistenteActivo").setAttribute("aria-invalid", "true");
      $("asistenteActivo").focus();
      return;
    }
    const boton = $("enviarAsistente");
    boton.disabled = true; boton.textContent = "Pensando…";
    dialogo.setAttribute("aria-busy", "true");
    try {
      const r = await fetch(api, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify(modo === "ayuda" ? { modo, pregunta } : { modo, pregunta, activo })
      });
      const cuerpo = await r.json().catch(() => ({}));
      if (r.status === 401) throw new Error("Tu sesión venció. Iniciá sesión nuevamente.");
      if (!r.ok) throw new Error(cuerpo.detail || cuerpo.mensaje || "No se pudo consultar al asistente (" + r.status + ").");
      mostrar(cuerpo);
    } catch (e) {
      $("asistenteError").textContent = e instanceof TypeError ? "No hay conexión con el servidor." : e.message;
    } finally {
      boton.disabled = false; boton.textContent = "Preguntar";
      dialogo.removeAttribute("aria-busy");
    }
  }

  abrir.addEventListener("click", () => { dialogo.showModal(); consultarEstado(); elegirModo(modo); });
  $("cerrarAsistente").addEventListener("click", () => { dialogo.close(); abrir.focus(); });
  for (const pestania of document.querySelectorAll(".asistente-pestanias [role=tab]")) {
    pestania.addEventListener("click", () => elegirModo(pestania.dataset.modo));
    pestania.addEventListener("keydown", e => {
      if (e.key === "ArrowRight" || e.key === "ArrowLeft") { e.preventDefault(); elegirModo(modo === "ayuda" ? "noticias" : "ayuda"); }
    });
  }
  $("asistentePregunta").addEventListener("input", e => {
    $("contadorPregunta").textContent = e.target.value.length + " de 500 caracteres";
  });
  $("asistentePregunta").addEventListener("keydown", e => {
    if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) $("formAsistente").requestSubmit();
  });
  $("asistenteActivo").addEventListener("input", sugerirActivos);
  $("formAsistente").addEventListener("submit", preguntar);
})();
