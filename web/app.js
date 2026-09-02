let listaDefectos = [];
let defectoSeleccionado = null;

document.addEventListener('DOMContentLoaded', () => {
    cargarDefectos();

    document.getElementById('searchInput').addEventListener('input', filtrarDefectos);
    document.getElementById('categoryFilter').addEventListener('change', filtrarDefectos);

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
        renderGrid(listaDefectos);
    } catch (err) {
        console.error("Error al cargar defectos:", err);
    }
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
                <span class="badge">${def.categoria}</span>
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
    document.getElementById('detailBadge').textContent = def.categoria;
    document.getElementById('detailCausa').textContent = def.causa;
    document.getElementById('detailSolucion').textContent = def.solucion;

    document.getElementById('modalDetalle').style.display = 'flex';
}

function abrirModalEditar(def) {
    document.getElementById('editId').value = def.id;
    document.getElementById('editTitulo').value = def.titulo;
    document.getElementById('editCategoria').value = def.categoria;
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

    const filtrados = listaDefectos.filter(d => {
        const coincideTexto = d.titulo.toLowerCase().includes(text) ||
                              d.causa.toLowerCase().includes(text) ||
                              d.solucion.toLowerCase().includes(text);
        const coincideCat = cat === '' || d.categoria === cat;
        return coincideTexto && coincideCat;
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

    // Actualizar datos
    listaDefectos[index] = {
        id: id,
        titulo: document.getElementById('editTitulo').value,
        categoria: document.getElementById('editCategoria').value,
        imagenUrl: imagenUrl,
        causa: document.getElementById('editCausa').value,
        solucion: document.getElementById('editSolucion').value
    };

    await guardarJSON();
    cerrarModal('modalEditar');
    renderGrid(listaDefectos);
}

async function borrarDefecto(id) {
    if (confirm("¿Estás seguro de que deseas eliminar este defecto del catálogo?")) {
        listaDefectos = listaDefectos.filter(d => d.id !== id);
        await guardarJSON();
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

