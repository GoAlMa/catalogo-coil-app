let listaDefectos = [];
let defectoSeleccionado = null;

document.addEventListener('DOMContentLoaded', () => {
    cargarDefectos();

    // Eventos de búsqueda y filtros
    document.getElementById('searchInput').addEventListener('input', filtrarDefectos);
    document.getElementById('categoryFilter').addEventListener('change', filtrarDefectos);
    document.getElementById('subCategoryFilter').addEventListener('change', filtrarDefectos); // <-- NUEVO

    document.getElementById('editForm').addEventListener('submit', guardarEdicionDefecto);

    document.getElementById('btnEditarModal').addEventListener('click', () => {
        if (defectoSeleccionado) {
            cerrarModal('modalDetalle');
            abrirModalEditar(defectoSeleccionado);
        }
    });

    document.getElementById('btnBorrarModal').addEventListener('click', () => {
        if (defectoSeleccionado) {
            borrarDefecto(defectoSeleccionado.id);
        }
    });
});

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
        grid.innerHTML = '<p style="grid-column: 1/-1; text-align:center; color:#666;">No se encontraron defectos.</p>';
        return;
    }

    defectos.forEach(def => {
        const card = document.createElement('div');
        card.className = 'card';

        card.innerHTML = `
            <img src="${def.imagenUrl}" alt="${def.titulo}" onerror="this.src='https://via.placeholder.com/400x300?text=Sin+Imagen'">
            <div class="card-body">
                <div class="card-title">${def.titulo}</div>
                <span class="badge">${def.categoria} ${def.subcategoria ? ' / ' + def.subcategoria : ''}</span>
            </div>
        `;

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
    document.getElementById('editSubcategoria').value = def.subcategoria || ''; // <-- NUEVO
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
    const subCat = document.getElementById('subCategoryFilter').value; // <-- NUEVO

    const filtrados = listaDefectos.filter(d => {
        const coincideTexto = d.titulo.toLowerCase().includes(text) ||
                              d.causa.toLowerCase().includes(text) ||
                              d.solucion.toLowerCase().includes(text);

        const coincideCat = cat === '' || d.categoria === cat;
        const coincideSubCat = subCat === '' || d.subcategoria === subCat; // <-- NUEVO

        return coincideTexto && coincideCat && coincideSubCat;
    });

    renderGrid(filtrados);
}

async function guardarEdicionDefecto(e) {
    e.preventDefault();

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
            body: file
        });
        const uploadData = await uploadRes.json();
        imagenUrl = uploadData.url;
    }

    // Actualizar datos incluyendo subcategoría
    listaDefectos[index] = {
        id: id,
        titulo: document.getElementById('editTitulo').value,
        categoria: document.getElementById('editCategoria').value,
        subcategoria: document.getElementById('editSubcategoria').value, // <-- NUEVO
        imagenUrl: imagenUrl,
        causa: document.getElementById('editCausa').value,
        solucion: document.getElementById('editSolucion').value
    };

    await guardarJSON();

    // Actualizar también los desplegables por si cambió la categoría o subcategoría
    poblarFiltrosDinamicos(listaDefectos);

    cerrarModal('modalEditar');
    renderGrid(listaDefectos);
}

async function borrarDefecto(id) {
    if (confirm("¿Estás seguro de que deseas eliminar este defecto del catálogo?")) {
        listaDefectos = listaDefectos.filter(d => d.id !== id);
        await guardarJSON();

        // Actualizar desplegables tras borrar
        poblarFiltrosDinamicos(listaDefectos);

        cerrarModal('modalDetalle');
        renderGrid(listaDefectos);
    }
}

async function guardarJSON() {
    await fetch('/api/defectos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(listaDefectos)
    });

    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js').catch(err => console.log('SW error:', err));
      });
    }
}