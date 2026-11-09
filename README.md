# Catálogo Técnico de Defectos COIL

Aplicación web para consultar un catálogo de defectos de pintura en bobina (coil coating). Cada defecto tiene una fotografía, un título, una categoría y subcategoría, la causa y la solución.

Funciona en móvil, tablet y ordenador. La consulta es pública; añadir o editar requiere una clave de acceso.

## Funcionalidades

- Cuadrícula de defectos con búsqueda y filtros por categoría y subcategoría.
- Detalle de cada defecto con foto, causa y solución.
- Formulario para añadir defectos, con sugerencias de categorías y subcategorías ya existentes.
- Edición y borrado, protegidos por clave.
- Aplicación instalable (PWA) con service worker.

## Arquitectura

- **Backend:** Java 17 con `com.sun.net.httpserver.HttpServer`, sin frameworks. Empaquetado en un único JAR con Maven.
- **Frontend:** HTML, CSS y JavaScript sin frameworks. Los textos de usuario se insertan con la API del DOM, no con `innerHTML`.
- **Persistencia:** los defectos se guardan en un fichero JSON y las fotos en una carpeta `uploads/`, ambos dentro de `STORAGE_DIR`. La escritura del JSON es atómica (fichero temporal y renombrado).
- **Seguridad:** las operaciones de escritura exigen una cabecera `Authorization: Bearer <clave>`. La comparación es de tiempo constante. Si la variable `ACCESO_TOKEN` no está definida, las escrituras quedan bloqueadas.

### Endpoints

| Método | Ruta | Descripción | Requiere clave |
|---|---|---|---|
| GET | `/api/defectos` | Lista de defectos | No |
| POST | `/api/defectos` | Guarda la lista completa de defectos | Sí |
| POST | `/api/subir-foto` | Sube una fotografía | Sí |
| GET | `/api/sesion` | Comprueba si la clave es válida (200 o 401) | Sí |
| GET | `/uploads/...` | Sirve las fotografías | No |
| GET | `/...` | Sirve la web (HTML, CSS, JS) desde el JAR | No |

## Configuración

| Variable | Obligatoria | Descripción | Valor por defecto |
|---|---|---|---|
| `ACCESO_TOKEN` | Sí, para escribir | Clave de acceso de edición | Ninguno (escrituras bloqueadas) |
| `STORAGE_DIR` | No | Carpeta de datos y fotos | `./data` |
| `PORT` | No | Puerto HTTP | `8085` |
| `TELEGRAM_BOT_TOKEN` | No | Envía una copia de seguridad del JSON por Telegram | Ninguno |
| `TELEGRAM_CHAT_ID` | No | Chat de destino de la copia de seguridad | Ninguno |

## Ejecución en local

Requisitos: Java 17 y Maven.

```bash
mvn clean package -DskipTests
ACCESO_TOKEN=cambia-esta-clave STORAGE_DIR=./data java -jar target/app.jar
```

Después abre `http://localhost:8085`.

## Despliegue

El proyecto está preparado para Railway (Nixpacks, según `railway.json`):

1. Crear un servicio desde este repositorio, rama `main`.
2. Añadir un volumen montado en `/data`.
3. Definir `STORAGE_DIR=/data` y `ACCESO_TOKEN`.
4. Asignar el dominio público al puerto `8085`.

Sin volumen, los datos se pierden en cada reinicio, porque el sistema de ficheros del contenedor es efímero.

## Estructura

```
src/                      Código Java (servidor y acceso a datos)
web/                      Interfaz (se empaqueta dentro del JAR)
  index.html, nuevo.html  Páginas
  app.js, nuevo.js        Lógica del cliente
  styles.css              Estilos
  sw.js, manifest.json    PWA
pom.xml                   Build con Maven
railway.json              Configuración de despliegue
```

## Limitaciones conocidas

- Los datos se guardan en un fichero JSON. Varias escrituras simultáneas pueden pisarse. Para más usuarios convendría migrar a SQLite o PostgreSQL.
- No hay miniaturas: la cuadrícula carga las fotografías originales.
- No hay pruebas automatizadas todavía.
