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

public class ServidorWeb {

    // Detección automática del volumen de Railway o local
    private static final String STORAGE_DIR = obtenerRutaInteligente();

    private static final Path ARCHIVO_JSON = Paths.get(STORAGE_DIR, "defectos.json");
    private static final Path CARPETA_UPLOADS = Paths.get(STORAGE_DIR, "uploads");

    private static String obtenerRutaInteligente() {
        String env = System.getenv("STORAGE_DIR");
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        if (Files.exists(Paths.get("/data"))) {
            return "/data";
        }
        return "./data";
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(STORAGE_DIR));
        Files.createDirectories(CARPETA_UPLOADS);

        if (!Files.exists(ARCHIVO_JSON)) {
            Files.writeString(ARCHIVO_JSON, "[]", StandardCharsets.UTF_8);
        }

        int puerto = 8085;
        HttpServer server = HttpServer.create(new InetSocketAddress(puerto), 0);

        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/defectos", new DefectosHandler());
        server.createContext("/api/subir-foto", new SubirFotoHandler());
        server.createContext("/uploads", new UploadsHandler());

        server.setExecutor(null);
        server.start();
        System.out.println(">>> Servidor iniciado en el puerto " + puerto);
        System.out.println(">>> STORAGE_DIR activo en: " + STORAGE_DIR);
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
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                    if (!body.trim().isEmpty()) {
                        synchronized (ARCHIVO_JSON) {
                            Files.writeString(ARCHIVO_JSON, body, StandardCharsets.UTF_8);
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

                    // Asegurar que la carpeta uploads existe antes de escribir
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

        if (token == null || chatId == null || token.trim().isEmpty() || chatId.trim().isEmpty()) {
            return;
        }

        try {
            if (!Files.exists(ARCHIVO_JSON)) return;

            String boundary = "Boundary-" + System.currentTimeMillis();
            byte[] fileBytes = Files.readAllBytes(ARCHIVO_JSON);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write((chatId.trim() + "\r\n").getBytes(StandardCharsets.UTF_8));

            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"document\"; filename=\"defectos_backup.json\"\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Type: application/json\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(fileBytes);
            baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + token.trim() + "/sendDocument"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            System.out.println(">>> Backup de Telegram enviado correctamente.");
                        } else {
                            System.err.println(">>> Error en Telegram (Código " + response.statusCode() + "): " + response.body());
                        }
                    });

        } catch (Exception e) {
            System.err.println(">>> Excepción al enviar backup a Telegram: " + e.getMessage());
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
            String fileName = path.replace("/uploads/", "");

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
