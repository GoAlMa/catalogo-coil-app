// Service worker: caché de la interfaz y última copia de los datos para uso sin conexión.
const VERSION = 'coil-v2';
const CACHE = `${VERSION}-app`;

const INTERFAZ = [
  '/index.html',
  '/nuevo.html',
  '/app.js',
  '/nuevo.js',
  '/styles.css',
  '/manifest.json',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/icons/apple-touch-icon.png'
];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.addAll(INTERFAZ))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((claves) => Promise.all(
        claves.filter((k) => k.startsWith('coil-') && k !== CACHE).map((k) => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  // La comprobación de clave nunca se guarda en caché
  if (url.pathname === '/api/sesion') return;

  if (url.pathname.startsWith('/uploads/')) {
    e.respondWith(primeroCaché(req));
    return;
  }

  // Datos e interfaz: primero la red (siempre lo último); si no hay conexión, la copia guardada
  e.respondWith(primeroRed(req));
});

async function primeroRed(req) {
  try {
    const res = await fetch(req);
    if (res.ok) {
      const copia = res.clone();
      caches.open(CACHE).then((cache) => cache.put(req, copia));
    }
    return res;
  } catch (err) {
    const guardado = await caches.match(req);
    if (guardado) return guardado;
    return new Response('Sin conexión', { status: 503, statusText: 'Sin conexión' });
  }
}

async function primeroCaché(req) {
  const guardado = await caches.match(req);
  if (guardado) return guardado;
  const res = await fetch(req);
  if (res.ok) {
    const copia = res.clone();
    caches.open(CACHE).then((cache) => cache.put(req, copia));
  }
  return res;
}
