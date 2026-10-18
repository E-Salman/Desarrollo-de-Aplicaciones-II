(() => {
    'use strict';
    const root = document.getElementById('simulador');
    if (!root) return;
    const $ = id => document.getElementById(id);
    const rows = [...root.querySelectorAll('[data-tipo]')];
    const fields = $('sim-fields'), status = $('sim-status');
    const endpoint = `${root.dataset.context}/api/portfolio/simulacion`;
    const limit = 1000000n; // Porcentajes con cuatro decimales: 100 %.
    let saved = null, busy = false;
    const parse = (value, decimals) => {
        if (!new RegExp(`^\\d+(?:\\.\\d{1,${decimals}})?$`).test(value)) throw new Error('Revisá los valores: capital con hasta dos decimales y porcentajes con hasta cuatro.');
        const [integer, fraction = ''] = value.split('.');
        return BigInt(integer) * 10n ** BigInt(decimals) + BigInt(fraction.padEnd(decimals, '0'));
    };
    const format = (value, decimals) => {
        const scale = 10n ** BigInt(decimals);
        const fraction = (value % scale).toString().padStart(decimals, '0').replace(/0+$/, '');
        return (value / scale).toLocaleString('es-AR') + (fraction ? ',' + fraction : '');
    };
    const inputPlan = () => {
        const capital = $('sim-capital').value;
        const c = parse(capital, 2);
        if (c > 99999999999999n) throw new Error('El capital máximo es 999.999.999.999,99.');
        const percentages = rows.map(row => parse(row.querySelector('input').value, 4));
        if (percentages.some(p => p > limit)) throw new Error('Cada porcentaje debe estar entre 0 y 100.');
        const sum = percentages.reduce((a, b) => a + b, 0n);
        if (sum > limit) throw new Error(`La distribución suma ${format(sum, 4)} %. Reducila hasta 100 % o menos.`);
        return { capital, c, percentages, sum,
            porcentajes: Object.fromEntries(rows.map(row => [row.dataset.tipo, row.querySelector('input').value])) };
    };
    const identity = plan => JSON.stringify([plan.c.toString(), plan.percentages.map(String)]);
    const message = (text, error = false) => { status.textContent = text; status.classList.toggle('sim-error', error); };
    const preview = () => {
        try {
            const plan = inputPlan();
            const dirty = saved !== identity(plan);
            $('sim-save').disabled = busy || !dirty;
            $('sim-result-state').textContent = dirty ? 'Vista previa · cambios sin aplicar' : 'Plan aplicado';
            $('sim-result').classList.remove('sim-invalid');
            $('sim-total').textContent = format(plan.c, 2);
            $('sim-remaining').textContent = format(plan.c * (limit - plan.sum), 8);
            $('sim-percent').textContent = `${format(limit - plan.sum, 4)} % disponible`;
            const breakdown = $('sim-breakdown'), bar = $('sim-bar');
            breakdown.replaceChildren(); bar.replaceChildren();
            rows.forEach((row, index) => {
                const term = document.createElement('dt'), value = document.createElement('dd');
                term.textContent = `${row.dataset.nombre} · ${format(plan.percentages[index], 4)} %`;
                term.dataset.tipo = row.dataset.tipo;
                value.textContent = format(plan.c * plan.percentages[index], 8);
                breakdown.append(term, value);
                const segment = document.createElement('span');
                segment.dataset.tipo = row.dataset.tipo;
                segment.style.width = `${Number(plan.percentages[index]) / 10000}%`;
                bar.append(segment);
            });
            if (!busy) message(dirty ? 'Ajustá la distribución y aplicá el plan para conservarlo en esta sesión.' : 'Tu plan está actualizado.');
            return plan;
        } catch (error) {
            $('sim-save').disabled = true;
            $('sim-result-state').textContent = 'Corregí los valores para actualizar la vista previa';
            $('sim-result').classList.add('sim-invalid');
            message(error.message, true);
            return null;
        }
    };
    const read = async (url, options = {}) => {
        const response = await fetch(url, { credentials: 'same-origin', ...options });
        if (response.status === 401 || response.status === 403) throw new Error('Tu sesión terminó. Volvé a iniciar sesión para continuar.');
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(body.error || body.message || 'No pudimos aplicar el plan. Intentá nuevamente.');
        }
        return response.status === 204 ? null : response.json();
    };
    const populate = dto => {
        $('sim-capital').value = dto.capital ?? 0;
        rows.forEach(row => { row.querySelector('input').value = dto.porcentajes?.[row.dataset.tipo] ?? 0; });
        saved = identity(inputPlan());
    };
    $('sim-form').addEventListener('input', () => { if (!busy) preview(); });
    $('sim-form').addEventListener('submit', async event => {
        event.preventDefault();
        const plan = preview();
        if (!plan || busy) return;
        busy = true; fields.disabled = true; message('Aplicando simulación…');
        try {
            const dto = await read(`${endpoint}/plan`, { method: 'PUT', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ capital: plan.capital, porcentajes: plan.porcentajes }) });
            populate(dto);
            busy = false; preview(); message('Simulación aplicada. Tu cartera no cambió.');
        } catch (error) { message(error.message + ' Tus cambios siguen en el formulario; podés volver a intentar.', true); }
        finally { busy = false; fields.disabled = false; $('sim-save').disabled = saved === identity(plan); }
    });
    $('sim-reset').addEventListener('click', async () => {
        if (busy) return;
        busy = true; fields.disabled = true; message('Reiniciando simulación…');
        try {
            await read(endpoint, { method: 'DELETE' });
            populate({ capital: 0, porcentajes: {} });
            busy = false; preview(); message('Simulación reiniciada. Tu cartera no cambió.');
        } catch (error) { message(error.message, true); }
        finally { busy = false; fields.disabled = false; }
    });
    // El catálogo informa disponibilidad. No restringe la planificación a lo ya cargado.
    read(`${root.dataset.context}/api/instrumentos`).then(items => {
        const counts = {};
        for (const item of items) counts[item.tipo] = (counts[item.tipo] || 0) + 1;
        rows.forEach(row => {
            const count = counts[row.dataset.tipo] || 0;
            row.querySelector('.sim-catalogo').textContent = count
                ? `${count.toLocaleString('es-AR')} instrumentos en el catálogo`
                : 'Sin instrumentos cargados · podés planificar este tipo';
        });
    }).catch(() => rows.forEach(row => { row.querySelector('.sim-catalogo').textContent = 'Catálogo no disponible · podés simular igualmente'; }));
    read(endpoint).then(dto => { populate(dto); fields.disabled = false; preview(); })
        .catch(error => { message(error.message + ' Recargá la página para volver a intentar.', true); });
})();
