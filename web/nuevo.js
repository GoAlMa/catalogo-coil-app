document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('defectoForm').addEventListener('submit', guardarNuevoDefecto);
});

// Cabeceras con el token guardado en este navegador (si existe)
function cabecerasAutorizadas(extra = {}) {
    const token = localStorage.getItem('coil_token');
    return token ? { ...extra, 'Authorization': 'Bearer ' + token } : { ...extra };
}

async function guardarNuevoDefecto(e) {
    e.preventDefault();

    if (!localStorage.getItem('coil_token')) {
        alert('Necesitas la clave de acceso para registrar defectos. Vuelve al catálogo y accede en modo Fernando.');
        return;
    }

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

        // 2. Obtener lista actual de defectos
        const resCat = await fetch('/api/defectos');
        if (!resCat.ok) throw new Error(`No se pudo leer el catálogo (${resCat.status}).`);
        const listaDefectos = await resCat.json();

        // 3. Crear nuevo objeto
        const nuevoDefecto = {
            id: Date.now(),
            titulo: document.getElementById('formTitulo').value,
            categoria: document.getElementById('categoria').value,
            subcategoria: document.getElementById('subcategoria').value,
            imagenUrl: imagenUrl,
            causa: document.getElementById('formCausa').value,
            solucion: document.getElementById('formSolucion').value
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
    }
}
