package com.goalma.coildefectcatalog.app;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServidorWeb {

    private static String STORAGE_DIR;
    private static Path ARCHIVO_JSON;
    private static Path CARPETA_UPLOADS;

    public static void main(String[] args) throws IOException {
        // --- DIAGNOSTICO DE LA RAIZ DEL SERVIDOR ---
        try {
            java.io.File root = new java.io.File("/");
            java.io.File[] files = root.listFiles();
            System.out.println(">>> [DEBUG] Carpetas en la raíz del servidor (/):");
            if (files != null) {
                for (java.io.File f : files) {
                    System.out.println(" - " + f.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println(">>> [DEBUG ERROR] No se pudo listar la raíz: " + e.getMessage());
        }
        // -------------------------------------------

        // Diagnóstico y selección de la ruta persistente
        String storageEnv = System.getenv("STORAGE_DIR");
        String rutaElegida = "./data";

        if (storageEnv != null && !storageEnv.trim().isEmpty()) {
            rutaElegida = storageEnv.trim();
        } else if (Files.exists(Paths.get("/data"))) {
            rutaElegida = "/data";
        }

        System.out.println(">>> [DIAGNOSTICO] STORAGE_DIR de entorno: " + storageEnv);
        System.out.println(">>> [DIAGNOSTICO] ¿Existe /data en el sistema?: " + Files.exists(Paths.get("/data")));
        System.out.println(">>> [DIAGNOSTICO] RUTA FINAL SELECCIONADA: " + rutaElegida);

        STORAGE_DIR = rutaElegida;
        ARCHIVO_JSON = Paths.get(STORAGE_DIR, "defectos.json");
        CARPETA_UPLOADS = Paths.get(STORAGE_DIR, "uploads");

        Files.createDirectories(Paths.get(STORAGE_DIR));
        Files.createDirectories(CARPETA_UPLOADS);

        if (!Files.exists(ARCHIVO_JSON)) {
            Files.writeString(ARCHIVO_JSON, "[]", StandardCharsets.UTF_8);
        }

        // Prueba de escritura real en el volumen
        try {
            Path testFile = Paths.get(STORAGE_DIR).resolve("test_persistencia.tmp");
            Files.writeString(testFile, "test", StandardCharsets.UTF_8);
            Files.deleteIfExists(testFile);
            System.out.println(">>> [OK] ¡Escritura en el volumen persistente verificada con éxito!");
        } catch (Exception e) {
            System.err.println(">>> [ERROR CRITICO] NO SE PUEDE ESCRIBIR EN EL VOLUMEN: " + e.getMessage());
        }

        // Puerto dinámico para Railway o local (por defecto 8085 u 8080)
        int puerto = 8085;
        String puertoEnv = System.getenv("PORT");
        if (puertoEnv != null && !puertoEnv.isEmpty()) {
            puerto = Integer.parseInt(puertoEnv);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);

        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/defectos", new DefectosHandler());
        server.createContext("/api/subir-foto", new SubirFotoHandler());
        server.createContext("/uploads", new UploadsHandler());

        server.setExecutor(null);
        server.start();
        System.out.println(">>> Servidor iniciado en el puerto " + puerto);
    }

    static class DefectosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    synchronized (ARCHIVO_JSON) {
                        if (!Files.exists(ARCHIVO_JSON)) {
                            Files.writeString(ARCHIVO_JSON, "[]", StandardCharsets.UTF_8);
                        }
                        byte[] response = Files.readAllBytes(ARCHIVO_JSON);
                        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response);
                        }
                    }
                } else if ("POST".equalsIgnoreCase(method)) {
                    InputStream is = exchange.getRequestBody();
                    String nuevoJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                    if (!nuevoJson.trim().isEmpty()) {
                        synchronized (ARCHIVO_JSON) {
                            Path tmp = ARCHIVO_JSON.resolveSibling("defectos.json.tmp");
                            Files.writeString(tmp, nuevoJson, StandardCharsets.UTF_8);
                            Files.move(tmp, ARCHIVO_JSON, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        }
                        enviarBackupTelegram();
                    }

                    byte[] resMsg = "{\"status\": \"ok\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, resMsg.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resMsg);
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                System.err.println(">>> Error en DefectosHandler: " + e.getMessage());
                e.printStackTrace();
                String errorRes = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, errorRes.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorRes.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private static void limpiarFotosHuerfanas(String nuevoJson) {
        try {
            if (!Files.exists(ARCHIVO_JSON)) return;
            String viejoJson = Files.readString(ARCHIVO_JSON, StandardCharsets.UTF_8);

            java.util.Set<String> fotosAntiguas = extraerNombresDeFotos(viejoJson);
            java.util.Set<String> fotosNuevas = extraerNombresDeFotos(nuevoJson);

            for (String foto : fotosAntiguas) {
                if (!fotosNuevas.contains(foto)) {
                    Path rutaFoto = CARPETA_UPLOADS.resolve(foto);
                    if (Files.deleteIfExists(rutaFoto)) {
                        System.out.println(">>> [LIMPIEZA] Foto huérfana eliminada del disco: " + foto);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(">>> Error limpiando fotos huérfanas: " + e.getMessage());
        }
    }

    private static java.util.Set<String> extraerNombresDeFotos(String texto) {
        java.util.Set<String> set = new java.util.HashSet<>();
        Pattern pattern = Pattern.compile("foto_\\d+\\.(jpg|jpeg|png)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(texto);
        while (matcher.find()) {
            set.add(matcher.group());
        }
        return set;
    }

    static class SubirFotoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("POST".equalsIgnoreCase(method)) {
                    byte[] fileBytes = exchange.getRequestBody().readAllBytes();
                    String fileName = "foto_" + System.currentTimeMillis() + ".jpg";

                    Files.createDirectories(CARPETA_UPLOADS);
                    Path filePath = CARPETA_UPLOADS.resolve(fileName);
                    Files.write(filePath, fileBytes);
                    System.out.println(">>> Foto guardada físicamente en: " + filePath.toAbsolutePath());

                    String jsonRespuesta = "{\"url\": \"/uploads/" + fileName + "\"}";
                    byte[] resBytes = jsonRespuesta.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, resBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resBytes);
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            } catch (Exception e) {
                System.err.println(">>> Error al subir la foto: " + e.getMessage());
                e.printStackTrace();
                String errorRes = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(500, errorRes.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorRes.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    public static void enviarBackupTelegram() {
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        String chatId = System.getenv("TELEGRAM_CHAT_ID");

        if (token == null || token.trim().isEmpty() || chatId == null || chatId.trim().isEmpty()) {
            System.out.println(">>> Telegram OMITIDO: Faltan las variables TELEGRAM_BOT_TOKEN o TELEGRAM_CHAT_ID");
            return;
        }

        try {
            if (!Files.exists(ARCHIVO_JSON)) return;

            String cleanToken = token.trim();
            String cleanChatId = chatId.trim();

            String boundary = "Boundary-" + System.currentTimeMillis();
            byte[] fileBytes = Files.readAllBytes(ARCHIVO_JSON);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write((cleanChatId + "\r\n").getBytes(StandardCharsets.UTF_8));

            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"document\"; filename=\"defectos_backup.json\"\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Type: application/json\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(fileBytes);
            baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + cleanToken + "/sendDocument"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            System.out.println(">>> ¡ÉXITO! Backup de Telegram enviado correctamente.");
                        } else {
                            System.err.println(">>> Error en Telegram (Código " + response.statusCode() + "): " + response.body());
                        }
                    });

        } catch (Exception e) {
            System.err.println(">>> Excepción crítica al enviar backup a Telegram: " + e.getMessage());
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            File file = new File("web" + path);
            if (file.exists() && !file.isDirectory()) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String mimeType = obtenerMimeType(path);
                exchange.getResponseHeaders().set("Content-Type", mimeType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String notFound = "404 No encontrado";
                exchange.sendResponseHeaders(404, notFound.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
            }
        }
    }

    static class UploadsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String fileName = path.replace("/uploads/", "").trim();

            if (fileName.isEmpty() || fileName.equals("/")) {
                try {
                    Files.createDirectories(CARPETA_UPLOADS);
                    StringBuilder sb = new StringBuilder("<html><head><meta charset='UTF-8'><title>Fotos Guardadas</title></head><body style='font-family:sans-serif; padding:20px;'>");
                    sb.append("<h2>📂 Archivos en el volumen persistente (/uploads):</h2><ul>");

                    long totalFiles = 0;
                    try (var stream = Files.list(CARPETA_UPLOADS)) {
                        var files = stream.filter(p -> !Files.isDirectory(p)).toList();
                        totalFiles = files.size();
                        for (Path p : files) {
                            String name = p.getFileName().toString();
                            sb.append("<li><a href='/uploads/").append(name).append("' target='_blank'>").append(name).append("</a></li>");
                        }
                    }

                    sb.append("</ul><p><b>Total de fotos almacenadas de forma segura: </b>").append(totalFiles).append("</p>");
                    sb.append("</body></html>");

                    byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    String err = "Error al listar la carpeta: " + e.getMessage();
                    exchange.sendResponseHeaders(500, err.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(err.getBytes());
                    }
                }
                return;
            }

            Path filePath = CARPETA_UPLOADS.resolve(fileName);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", obtenerMimeType(fileName));
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }

    private static String obtenerMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        return "text/plain; charset=UTF-8";
    }
}