(() => {
    'use strict';
    const root = document.getElementById('simulador');
    if (!root) return;
    const $ = id => document.getElementById(id);
    const rows = [...root.querySelectorAll('[data-tipo]')];
    const fields = $('sim-fields'), status = $('sim-status');
    const endpoint = `${root.dataset.context}/api/portfolio/simulacion`;
    const limit = 1000000n; // Porcentajes con cuatro decimales: 100 %.
    let saved = null, savedRaw = null, busy = false, allowNavigation = false;
    let pendingDestination = null;
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
        return { capital, c, percentages, sum, moneda: $('sim-currency').value,
            porcentajes: Object.fromEntries(rows.map(row => [row.dataset.tipo, row.querySelector('input').value])) };
    };
    const identity = plan => JSON.stringify([plan.moneda, plan.c.toString(), plan.percentages.map(String)]);
    const rawIdentity = () => JSON.stringify([$('sim-currency').value, $('sim-capital').value,
        rows.map(row => row.querySelector('input').value)]);
    const pendingChanges = () => {
        if (busy) return true;
        if (saved === null) return false;
        try { return identity(inputPlan()) !== saved; }
        catch { return rawIdentity() !== savedRaw; }
    };
    window.addEventListener('beforeunload', event => {
        if (!allowNavigation && pendingChanges()) { event.preventDefault(); event.returnValue = ''; }
    });
    document.addEventListener('click', event => {
        const link = event.target.closest('a[href]');
        if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey
                || event.shiftKey || event.altKey || link.target === '_blank' || link.hasAttribute('download')) return;
        const destination = new URL(link.href, window.location.href);
        if (destination.pathname === window.location.pathname && destination.search === window.location.search && destination.hash) return;
        if (!pendingChanges()) return;
        event.preventDefault();
        pendingDestination = link.href;
        $('sim-leave-description').textContent = busy
            ? 'Hay una operación en curso. Esperá a que termine para confirmar que el plan se aplicó.'
            : 'Los cambios que todavía no aplicaste se perderán. Tu último plan aplicado seguirá disponible durante esta sesión.';
        $('sim-leave').showModal();
    });
    $('sim-stay').addEventListener('click', () => { pendingDestination = null; $('sim-leave').close(); });
    $('sim-leave').addEventListener('cancel', () => { pendingDestination = null; });
    $('sim-leave-confirm').addEventListener('click', () => {
        if (!pendingDestination) return;
        allowNavigation = true;
        window.location.assign(pendingDestination);
    });
    const message = (text, error = false, success = false) => {
        for (const target of [status, $('sim-feedback')]) {
            target.textContent = text;
            target.classList.toggle('sim-error', error);
            target.classList.toggle('sim-success', success);
        }
    };
    const buttonState = state => {
        const button = $('sim-save');
        const states = {
            ready: ['Aplicar simulación', false, 'Aplicar los cambios de este plan.'],
            applied: ['Simulación aplicada ✓', true, 'El plan ya está aplicado. Cambiá un valor para simular de nuevo.'],
            empty: ['Aplicar simulación', true, 'Ingresá el capital y la distribución para comenzar.'],
            invalid: ['Revisá los valores', true, 'Corregí los valores indicados para continuar.'],
            saving: ['Aplicando…', true, 'Guardando el plan de esta sesión.'],
            resetting: ['Reiniciando…', true, 'Reiniciando el plan de esta sesión.']
        };
        const [label, disabled, title] = states[state];
        button.textContent = label; button.disabled = disabled; button.title = title;
        button.dataset.state = state;
        button.setAttribute('aria-busy', String(state === 'saving' || state === 'resetting'));
    };
    const settledButton = plan => buttonState(saved !== identity(plan) ? 'ready'
        : (plan.c > 0n || plan.sum > 0n ? 'applied' : 'empty'));
    const preview = () => {
        try {
            const plan = inputPlan();
            const dirty = saved !== identity(plan);
            if (!busy) settledButton(plan);
            const applied = !dirty && (plan.c > 0n || plan.sum > 0n);
            $('sim-result-state').textContent = dirty ? 'Vista previa · cambios sin aplicar' : (applied ? '✓ Simulación aplicada' : 'Comenzá ingresando tu capital');
            $('sim-result-state').classList.toggle('sim-success', applied);
            $('sim-result').classList.remove('sim-invalid');
            $('sim-total').textContent = `${plan.moneda} ${format(plan.c, 2)}`;
            $('sim-remaining').textContent = `${plan.moneda} ${format(plan.c * (limit - plan.sum), 8)}`;
            $('sim-percent').textContent = `${format(limit - plan.sum, 4)} % disponible`;
            const breakdown = $('sim-breakdown'), bar = $('sim-bar');
            breakdown.replaceChildren(); bar.replaceChildren();
            rows.forEach((row, index) => {
                const term = document.createElement('dt'), value = document.createElement('dd');
                term.textContent = `${row.dataset.nombre} · ${format(plan.percentages[index], 4)} %`;
                term.dataset.tipo = row.dataset.tipo;
                value.textContent = `${plan.moneda} ${format(plan.c * plan.percentages[index], 8)}`;
                breakdown.append(term, value);
                const segment = document.createElement('span');
                segment.dataset.tipo = row.dataset.tipo;
                segment.style.width = `${Number(plan.percentages[index]) / 10000}%`;
                bar.append(segment);
            });
            if (!busy) message(dirty ? 'Ajustá la distribución y aplicá el plan para conservarlo en esta sesión.'
                : (applied ? '✓ Simulación aplicada. Los resultados ya están disponibles. Cambiá un valor para simular de nuevo.'
                    : 'Ingresá el capital y los porcentajes para comenzar.'), false, applied);
            return plan;
        } catch (error) {
            buttonState('invalid');
            $('sim-result-state').classList.remove('sim-success');
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
        $('sim-currency').value = dto.moneda ?? 'ARS';
        $('sim-capital').value = dto.capital ?? 0;
        rows.forEach(row => { row.querySelector('input').value = dto.porcentajes?.[row.dataset.tipo] ?? 0; });
        saved = identity(inputPlan());
        savedRaw = rawIdentity();
    };
    $('sim-form').addEventListener('input', () => { if (!busy) preview(); });
    $('sim-currency').addEventListener('change', () => { if (!busy) preview(); });
    $('sim-form').addEventListener('submit', async event => {
        event.preventDefault();
        const plan = preview();
        if (!plan || busy) return;
        busy = true; fields.disabled = true; buttonState('saving'); message('Aplicando simulación…');
        try {
            const dto = await read(`${endpoint}/plan`, { method: 'PUT', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ capital: plan.capital, porcentajes: plan.porcentajes, moneda: plan.moneda }) });
            populate(dto);
            busy = false; preview(); message('✓ Simulación aplicada. Los resultados ya están disponibles. Tu cartera no cambió.', false, true);
        } catch (error) { message(error.message + ' Tus cambios siguen en el formulario; podés volver a intentar.', true); }
        finally { busy = false; fields.disabled = false; settledButton(plan); }
    });
    $('sim-reset').addEventListener('click', async () => {
        if (busy) return;
        busy = true; fields.disabled = true; buttonState('resetting'); message('Reiniciando simulación…');
        try {
            await read(endpoint, { method: 'DELETE' });
            populate({ capital: 0, porcentajes: {} });
            busy = false; preview(); message('Simulación reiniciada. Tu cartera no cambió.');
        } catch (error) { message(error.message, true); }
        finally {
            busy = false; fields.disabled = false;
            try { settledButton(inputPlan()); } catch { buttonState('invalid'); }
        }
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
