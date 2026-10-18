
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<link rel="stylesheet"
      href="<%= request.getContextPath() %>/assets/operaciones.css">

<!-- MODAL COMPRAR -->

<dialog id="modalCompraCompartido" class="operacion-modal">

    <form id="formCompraCompartido">

        <div class="modalTitulo">

            <h2>Comprar instrumento</h2>

            <button type="button"
                    id="cerrarCompraCompartido"
                    class="cerrar">
                ×
            </button>

        </div>

        <label>
            Tipo de instrumento

            <select id="tipoCompraCompartido">

                <option value="">Todos los tipos</option>
                <option value="ACCION">Acciones</option>
                <option value="BONO">Bonos</option>
                <option value="CEDEAR">CEDEARs</option>
                <option value="CRIPTO">Criptomonedas</option>
                <option value="MONEDA">Monedas</option>

            </select>
        </label>

        <label>
            Instrumento

            <select id="instrumentoCompraCompartido" required>
                <option value="">
                    Cargando instrumentos...
                </option>
            </select>
        </label>

        <p class="sub" id="disponibilidadCompraCompartida"></p>

        <div class="dos">

            <label>
                Ticker

                <input
                    id="tickerCompraCompartido"
                    readonly
                >
            </label>

            <label>
                Cantidad

                <input
                    id="cantidadCompraCompartida"
                    type="number"
                    min="0"
                    step="any"
                    required
                >
            </label>

        </div>

        <div class="dos">

            <label>
                Precio por unidad
                <span id="monedaCompraCompartida"></span>

                <input
                    id="precioCompraCompartido"
                    type="number"
                    min="0"
                    step="any"
                    required
                >
            </label>

            <label>
                Total invertido

                <input
                    id="totalCompraCompartido"
                    readonly
                >
            </label>

        </div>

        <p id="referenciaCompraCompartida"
           class="aviso"></p>

        <label>
            Fecha de compra

            <input
                id="fechaCompraCompartida"
                type="date"
                required
            >
        </label>

        <p id="errorCompraCompartida"
           class="error"
           role="alert"></p>

        <button type="submit"
                id="guardarCompraCompartida"
                class="comprar">
            Agregar al portfolio
        </button>

    </form>
</dialog>


<!-- MODAL VENDER -->

<dialog id="modalVentaCompartido" class="operacion-modal">

    <form id="formVentaCompartido">

        <div class="modalTitulo">

            <h2>Vender instrumento</h2>

            <button type="button"
                    id="cerrarVentaCompartida"
                    class="cerrar">
                ×
            </button>

        </div>

        <p id="disponibilidadVentaCompartida"
           class="sub"></p>

        <label>
            Instrumento de tu portfolio

            <select id="instrumentoVentaCompartido" required>

                <option value="">
                    Seleccioná un instrumento...
                </option>

            </select>
        </label>

        <div class="dos">

            <label>
                Ticker

                <input
                    id="tickerVentaCompartido"
                    readonly
                >
            </label>

            <label>
                Cantidad

                <button type="button"
                        id="venderTodoCompartido"
                        class="enlace">
                    Vender todo
                </button>

                <input
                    id="cantidadVentaCompartida"
                    type="number"
                    min="0"
                    step="any"
                    required
                >
            </label>

        </div>

        <div class="dos">

            <label>
                Precio por unidad
                <span id="monedaVentaCompartida"></span>

                <input
                    id="precioVentaCompartido"
                    type="number"
                    min="0"
                    step="any"
                    required
                >
            </label>

            <label>
                Total de la venta

                <input
                    id="totalVentaCompartido"
                    readonly
                >
            </label>

        </div>

        <label>
            Fecha de venta

            <input
                id="fechaVentaCompartida"
                type="date"
                required
            >
        </label>

        <p id="errorVentaCompartida"
           class="error"
           role="alert"></p>

        <button type="submit"
                id="guardarVentaCompartida"
                class="comprar">
            Registrar venta
        </button>

    </form>
</dialog>

<script defer
        src="<%= request.getContextPath() %>/assets/operaciones.js?v=1">
</script>
