document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('defectoForm').addEventListener('submit', guardarNuevoDefecto);

    const categoriaInput = document.getElementById('categoria');
    categoriaInput.addEventListener('input', actualizarSugerenciasSubcategoria);
    categoriaInput.addEventListener('change', actualizarSugerenciasSubcategoria);

    cargarSugerencias();
});

let defectosExistentes = [];

// Cabeceras con el token guardado en este navegador (si existe)
function cabecerasAutorizadas(extra = {}) {
    const token = localStorage.getItem('coil_token');
    return token ? { ...extra, 'Authorization': 'Bearer ' + token } : { ...extra };
}

// Carga el catálogo para ofrecer las categorías y subcategorías ya usadas
async function cargarSugerencias() {
    try {
        const res = await fetch('/api/defectos');
        if (!res.ok) return;
        defectosExistentes = await res.json();
    } catch (err) {
        console.error('No se pudieron cargar las sugerencias:', err);
    }
    rellenarDatalist('listaCategoriasSugeridas', valoresUnicos(defectosExistentes.map(d => d.categoria)));
    actualizarSugerenciasSubcategoria();
}

// Valores únicos, sin repetir por mayúsculas o espacios, ordenados
function valoresUnicos(valores) {
    const vistos = new Map();
    valores.forEach(v => {
        const t = (v || '').trim();
        if (t && !vistos.has(t.toLowerCase())) vistos.set(t.toLowerCase(), t);
    });
    return [...vistos.values()].sort((a, b) => a.localeCompare(b, 'es'));
}

function rellenarDatalist(id, valores) {
    const lista = document.getElementById(id);
    lista.textContent = '';
    valores.forEach(v => {
        const opcion = document.createElement('option');
        opcion.value = v;
        lista.appendChild(opcion);
    });
}

// Si hay una categoría escrita, las subcategorías sugeridas son las de esa categoría
function actualizarSugerenciasSubcategoria() {
    const cat = document.getElementById('categoria').value.trim().toLowerCase();
    const delCat = defectosExistentes
        .filter(d => (d.categoria || '').trim().toLowerCase() === cat)
        .map(d => d.subcategoria);
    const todas = defectosExistentes.map(d => d.subcategoria);
    rellenarDatalist('listaSubcategoriasSugeridas', valoresUnicos(delCat.length ? delCat : todas));
}

// Si el texto escrito ya existe (ignorando mayúsculas), usa la forma que ya está en el catálogo
function valorCanonico(texto, existentes) {
    const t = (texto || '').trim();
    const coincidencia = existentes.find(v => v.toLowerCase() === t.toLowerCase());
    return coincidencia || t;
}

async function guardarNuevoDefecto(e) {
    e.preventDefault();

    if (!localStorage.getItem('coil_token')) {
        alert('Necesitas la clave de acceso para registrar defectos. Vuelve al catálogo y accede en modo Fernando.');
        return;
    }

    const boton = e.submitter || document.querySelector('#defectoForm button[type=submit]');
    const textoOriginal = boton.textContent;
    boton.disabled = true;
    boton.textContent = 'Guardando…';

    const fotoInput = document.getElementById('formFoto');
    let imagenUrl = null;

    try {
        // 1. Subir la fotografía
        if (fotoInput.files.length > 0) {
            const file = fotoInput.files[0];
            const uploadRes = await fetch('/api/subir-foto', {
                method: 'POST',
                headers: cabecerasAutorizadas(),
                body: file
            });
            if (!uploadRes.ok) throw new Error(uploadRes.status === 401
                ? 'Clave no válida o caducada.'
                : `No se pudo subir la foto (${uploadRes.status}).`);
            const uploadData = await uploadRes.json();
            imagenUrl = uploadData.url;
        }

        // 2. Obtener la lista actual de defectos
        const resCat = await fetch('/api/defectos');
        if (!resCat.ok) throw new Error(`No se pudo leer el catálogo (${resCat.status}).`);
        const listaDefectos = await resCat.json();

        // 3. Categoría y subcategoría: reutilizar la forma existente si ya la hay
        const categorias = valoresUnicos(listaDefectos.map(d => d.categoria));
        const subcategorias = valoresUnicos(listaDefectos.map(d => d.subcategoria));

        const nuevoDefecto = {
            id: Date.now(),
            titulo: document.getElementById('formTitulo').value.trim(),
            categoria: valorCanonico(document.getElementById('categoria').value, categorias),
            subcategoria: valorCanonico(document.getElementById('subcategoria').value, subcategorias),
            imagenUrl: imagenUrl,
            causa: document.getElementById('formCausa').value.trim(),
            solucion: document.getElementById('formSolucion').value.trim()
        };

        listaDefectos.push(nuevoDefecto);

        // 4. Guardar la lista actualizada
        const res = await fetch('/api/defectos', {
            method: 'POST',
            headers: cabecerasAutorizadas({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(listaDefectos)
        });
        if (!res.ok) throw new Error(res.status === 401
            ? 'Clave no válida o caducada.'
            : `El servidor respondió ${res.status}.`);

        // 5. Volver al catálogo
        window.location.href = 'index.html';

    } catch (err) {
        alert('Error al guardar el defecto: ' + err.message);
        console.error(err);
        boton.disabled = false;
        boton.textContent = textoOriginal;
    }
}
