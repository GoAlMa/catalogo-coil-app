document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('defectoForm').addEventListener('submit', guardarNuevoDefecto);
});

async function guardarNuevoDefecto(e) {
    e.preventDefault();

    const fotoInput = document.getElementById('formFoto');
    let imagenUrl = "https://via.placeholder.com/400x300?text=Sin+Imagen";

    try {
        // 1. Subir la fotografía
        if (fotoInput.files.length > 0) {
            const file = fotoInput.files[0];
            const uploadRes = await fetch('/api/subir-foto', {
                method: 'POST',
                body: file
            });
            const uploadData = await uploadRes.json();
            imagenUrl = uploadData.url;
        }

        // 2. Obtener lista actual de defectos
        const resCat = await fetch('/api/defectos');
        const listaDefectos = await resCat.json();

        // 3. Crear nuevo objeto (añadiendo subcategoria)
        const nuevoDefecto = {
            id: Date.now(),
            titulo: document.getElementById('formTitulo').value,
            categoria: document.getElementById('formCategoria').value,
            subcategoria: document.getElementById('formSubcategoria').value, // <-- NUEVO
            imagenUrl: imagenUrl,
            causa: document.getElementById('formCausa').value,
            solucion: document.getElementById('formSolucion').value
        };

        listaDefectos.push(nuevoDefecto);

        // 4. Guardar lista actualizada en el archivo JSON
        await fetch('/api/defectos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(listaDefectos)
        });

        // 5. Redirigir al catálogo principal
        window.location.href = 'index.html';

    } catch (err) {
        alert("Error al guardar el defecto: " + err.message);
        console.error(err);
    }
}