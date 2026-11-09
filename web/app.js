// Comprobar si ya estás logueado como Fernando en este navegador
let esFernando = false; // Se confirma con el servidor al cargar (ver verificarSesion)
let listaDefectos = [];
let defectoSeleccionado = null;

document.addEventListener('DOMContentLoaded', () => {
    verificarSesion().then(() => {
        aplicarModoSegunRol();
        cargarDefectos();
    });

    // Eventos de búsqueda y filtros
    document.getElementById('searchInput').addEventListener('input', filtrarDefectos);
    document.getElementById('categoryFilter').addEventListener('change', filtrarDefectos);
    document.getElementById('subCategoryFilter').addEventListener('change', filtrarDefectos);

    document.getElementById('editForm').addEventListener('submit', guardarEdicionDefecto);

    document.getElementById('btnEditarModal').addEventListener('click', () => {
        if (esFernando && defectoSeleccionado) {
            cerrarModal('modalDetalle');
            abrirModalEditar(defectoSeleccionado);
        }
    });

    document.getElementById('btnBorrarModal').addEventListener('click', () => {
        if (esFernando && defectoSeleccionado) {
            borrarDefecto(defectoSeleccionado.id);
        }
    });
});

// Función que oculta o muestra los elementos de administración según el rol
function aplicarModoSegunRol() {
    const btnAcceso = document.getElementById('btnAccesoFernando');
    const elementosAdmin = document.querySelectorAll('.admin-only');

    if (esFernando) {
        if (btnAcceso) {
            btnAcceso.textContent = '🔓 Modo Fernando (Activo)';
            btnAcceso.style.borderColor = '#16a34a';
            btnAcceso.style.color = '#16a34a';
        }
        elementosAdmin.forEach(el => el.style.display = 'inline-block');
    } else {
        if (btnAcceso) {
            btnAcceso.textContent = '🔐 Acceso Fernando';
        }
        elementosAdmin.forEach(el => el.style.display = 'none');
    }
}

// Ventana para pedir la contraseña
async function pedirAccesoFernando() {
    if (esFernando) {
        if (confirm("¿Deseas salir del modo Fernando y volver a Modo Lector?")) {
            localStorage.removeItem('coil_token');
            location.reload();
        }
        return;
    }

    const token = prompt("Introduce la clave de acceso:");
    if (token === null) return;

    localStorage.setItem('coil_token', token.trim());
    if (await verificarSesion()) {
        alert("¡Acceso concedido!");
        location.reload();
    } else {
        localStorage.removeItem('coil_token');
        alert("Clave incorrecta.");
    }
}

// Cabeceras con el token guardado en este navegador (si existe)
function cabecerasAutorizadas(extra = {}) {
    const token = localStorage.getItem('coil_token');
    return token ? { ...extra, 'Authorization': 'Bearer ' + token } : { ...extra };
}

// Pregunta al servidor si el token es valido. El servidor es quien decide, no el cliente.
async function verificarSesion() {
    try {
        const res = await fetch('/api/sesion', { headers: cabecerasAutorizadas() });
        esFernando = res.ok;
    } catch (err) {
        esFernando = false;
    }
    return esFernando;
}


async function cargarDefectos() {
    try {
        const res = await fetch('/api/defectos');
        listaDefectos = await res.json();

        // Poblar los desplegables automáticamente con lo que haya guardado
        poblarFiltrosDinamicos(listaDefectos);

        renderGrid(listaDefectos);
    } catch (err) {
        console.error("Error al cargar defectos:", err);
    }
}

// Función para rellenar los desplegables con valores únicos de la base de datos
function poblarFiltrosDinamicos(defectos) {
    const catSelect = document.getElementById('categoryFilter');
    const subCatSelect = document.getElementById('subCategoryFilter');

    // Mantener la opción por defecto limpia
    catSelect.innerHTML = '<option value="">Todas las Categorías</option>';
    subCatSelect.innerHTML = '<option value="">Todas las Subcategorías</option>';

    // Extraer categorías y subcategorías únicas (evitando vacíos o nulos)
    const categoriasUnicas = [...new Set(defectos.map(d => d.categoria).filter(Boolean))];
    const subcategoriasUnicas = [...new Set(defectos.map(d => d.subcategoria).filter(Boolean))];

    categoriasUnicas.forEach(cat => {
        const opt = document.createElement('option');
        opt.value = cat;
        opt.textContent = cat;
        catSelect.appendChild(opt);
    });

    subcategoriasUnicas.forEach(sub => {
        const opt = document.createElement('option');
        opt.value = sub;
        opt.textContent = sub;
        subCatSelect.appendChild(opt);
    });
}

function renderGrid(defectos) {
    const grid = document.getElementById('gridDefectos');
    grid.innerHTML = '';

    if (defectos.length === 0) {
        const vacio = document.createElement('p');
        vacio.style.cssText = 'grid-column: 1/-1; text-align:center; color:#666;';
        vacio.textContent = 'No se encontraron defectos.';
        grid.appendChild(vacio);
        return;
    }

    const SIN_IMAGEN = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='400' height='300'><rect width='100%' height='100%' fill='%23e2e8f0'/><text x='50%' y='50%' text-anchor='middle' fill='%2364748b' font-family='sans-serif'>Sin imagen</text></svg>";

    defectos.forEach(def => {
        const card = document.createElement('div');
        card.className = 'card';

        const img = document.createElement('img');
        img.src = def.imagenUrl || SIN_IMAGEN;
        img.alt = def.titulo || 'Defecto';
        img.onerror = () => { img.src = SIN_IMAGEN; };

        const body = document.createElement('div');
        body.className = 'card-body';

        const titulo = document.createElement('div');
        titulo.className = 'card-title';
        titulo.textContent = def.titulo || '';

        const badge = document.createElement('span');
        badge.className = 'badge';
        badge.textContent = `${def.categoria || ''}${def.subcategoria ? ' / ' + def.subcategoria : ''}`;

        body.appendChild(titulo);
        body.appendChild(badge);
        card.appendChild(img);
        card.appendChild(body);

        card.onclick = () => abrirModalDetalle(def);
        grid.appendChild(card);
    });
}

function abrirModalDetalle(def) {
    defectoSeleccionado = def;
    document.getElementById('detailImg').src = def.imagenUrl;
    document.getElementById('detailTitle').textContent = def.titulo;
    document.getElementById('detailBadge').textContent = `${def.categoria} ${def.subcategoria ? ' / ' + def.subcategoria : ''}`;
    document.getElementById('detailCausa').textContent = def.causa;
    document.getElementById('detailSolucion').textContent = def.solucion;

    document.getElementById('modalDetalle').style.display = 'flex';
}

function abrirModalEditar(def) {
    document.getElementById('editId').value = def.id;
    document.getElementById('editTitulo').value = def.titulo;
    document.getElementById('editCategoria').value = def.categoria || '';
    document.getElementById('editSubcategoria').value = def.subcategoria || '';
    document.getElementById('editCausa').value = def.causa;
    document.getElementById('editSolucion').value = def.solucion;

    document.getElementById('modalEditar').style.display = 'flex';
}

function cerrarModal(id) {
    document.getElementById(id).style.display = 'none';
}

function filtrarDefectos() {
    const text = document.getElementById('searchInput').value.toLowerCase();
    const cat = document.getElementById('categoryFilter').value;
    const subCat = document.getElementById('subCategoryFilter').value;

    const filtrados = listaDefectos.filter(d => {
        const coincideTexto = d.titulo.toLowerCase().includes(text) ||
                              d.causa.toLowerCase().includes(text) ||
                              d.solucion.toLowerCase().includes(text);

        const coincideCat = cat === '' || d.categoria === cat;
        const coincideSubCat = subCat === '' || d.subcategoria === subCat;

        return coincideTexto && coincideCat && coincideSubCat;
    });

    renderGrid(filtrados);
}

async function guardarEdicionDefecto(e) {
    e.preventDefault();
    if (!esFernando) return; // Seguridad extra

    const id = parseInt(document.getElementById('editId').value);
    const index = listaDefectos.findIndex(d => d.id === id);

    if (index === -1) return;

    const fotoInput = document.getElementById('editFoto');
    let imagenUrl = listaDefectos[index].imagenUrl;

    // Si seleccionó nueva foto
    if (fotoInput.files.length > 0) {
        const file = fotoInput.files[0];
        const uploadRes = await fetch('/api/subir-foto', {
            method: 'POST',
            headers: cabecerasAutorizadas(),
            body: file
        });
        if (!uploadRes.ok) {
            alert('No se pudo subir la foto. Comprueba la clave de acceso.');
            return;
        }
        const uploadData = await uploadRes.json();
        imagenUrl = uploadData.url;
    }

    // Actualizar datos incluyendo subcategoría
    listaDefectos[index] = {
        id: id,
        titulo: document.getElementById('editTitulo').value,
        categoria: document.getElementById('editCategoria').value,
        subcategoria: document.getElementById('editSubcategoria').value,
        imagenUrl: imagenUrl,
        causa: document.getElementById('editCausa').value,
        solucion: document.getElementById('editSolucion').value
    };

    try {
        await guardarJSON();
    } catch (err) {
        alert('No se pudo guardar: ' + err.message);
        return;
    }

    // Actualizar también los desplegables por si cambió la categoría o subcategoría
    poblarFiltrosDinamicos(listaDefectos);

    cerrarModal('modalEditar');
    renderGrid(listaDefectos);
}

async function borrarDefecto(id) {
    if (!esFernando) return; // Seguridad extra

    if (confirm("¿Estás seguro de que deseas eliminar este defecto del catálogo?")) {
        listaDefectos = listaDefectos.filter(d => d.id !== id);
        try {
            await guardarJSON();
        } catch (err) {
            alert('No se pudo borrar: ' + err.message);
            return;
        }

        // Actualizar desplegables tras borrar
        poblarFiltrosDinamicos(listaDefectos);

        cerrarModal('modalDetalle');
        renderGrid(listaDefectos);
    }
}

async function guardarJSON() {
    const res = await fetch('/api/defectos', {
        method: 'POST',
        headers: cabecerasAutorizadas({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(listaDefectos)
    });
    if (res.status === 401) throw new Error('Clave no válida o caducada. Vuelve a acceder en modo Fernando.');
    if (!res.ok) throw new Error(`El servidor respondió ${res.status}`);
}
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js')
            .catch(err => console.error('SW error:', err));
    });
}
