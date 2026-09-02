package com.goalma.coildefectcatalog.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.Executors;

public class ServidorWeb {

    private static final int PUERTO = Integer.parseInt(System.getenv().getOrDefault("PORT", "8085"));
    private static final Path ARCHIVO_JSON = Paths.get("defectos.json");
    private static final Path DIR_UPLOADS = Paths.get("web", "uploads"); // <- Forma segura multiplataforma

    public static void main(String[] args) throws IOException {
        // Asegurar que la estructura de carpetas web/uploads exista de forma segura
        Files.createDirectories(DIR_UPLOADS);

        // Si defectos.json no existe o está vacío, lo inicializa correctamente
        if (!Files.exists(ARCHIVO_JSON) || Files.size(ARCHIVO_JSON) == 0) {
            String jsonInicial = "[\n" +
                    "  {\n" +
                    "    \"id\": 1,\n" +
                    "    \"titulo\": \"Cráteres en Pintura\",\n" +
                    "    \"categoria\": \"Cráteres y Pintura\",\n" +
                    "    \"imagenUrl\": \"/uploads/foto_1788258786553.jpg\",\n" +
                    "    \"causa\": \"Presencia de aceites o siliconas en la superficie antes del pintado.\",\n" +
                    "    \"solucion\": \"Limpiar rodillos aplicadores y revisar la desengrasadora.\"\n" +
                    "  }\n" +
                    "]";
            Files.writeString(ARCHIVO_JSON, jsonInicial, StandardCharsets.UTF_8);
        }

        // FUERZA LA ESCUCHA EN 0.0.0.0 PARA QUE LA NUBE LO ACCEDA CORRECTAMENTE
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PUERTO), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/api/defectos", new DefectosHandler());
        server.createContext("/api/subir-foto", new SubirFotoHandler());
        server.createContext("/", new EstaticosHandler());

        server.start();
        System.out.println(">>> Servidor Catálogo COIL corriendo en puerto " + PUERTO);
    }

    static class DefectosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                byte[] response = Files.readAllBytes(ARCHIVO_JSON);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();

            } else if ("POST".equalsIgnoreCase(method)) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                if (!body.trim().isEmpty()) {
                    synchronized (ARCHIVO_JSON) {
                        Files.writeString(ARCHIVO_JSON, body, StandardCharsets.UTF_8);
                    }
                }

                byte[] resMsg = "{\"status\": \"ok\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resMsg.length);
                OutputStream os = exchange.getResponseBody();
                os.write(resMsg);
                os.close();

            } else if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
            }
        }
    }

    static class SubirFotoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String fileName = "foto_" + System.currentTimeMillis() + ".jpg";
                Path targetPath = DIR_UPLOADS.resolve(fileName);

                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);

                String jsonResponse = "{\"url\": \"/uploads/" + fileName + "\"}";
                byte[] resBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(resBytes);
                os.close();
            }
        }
    }

    static class EstaticosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            Path filePath = Paths.get("web", path);

            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                String mimeType = "text/html";
                if (path.endsWith(".css")) mimeType = "text/css";
                else if (path.endsWith(".js")) mimeType = "text/javascript";
                else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) mimeType = "image/jpeg";
                else if (path.endsWith(".png")) mimeType = "image/png";

                byte[] fileBytes = Files.readAllBytes(filePath);
                exchange.getResponseHeaders().set("Content-Type", mimeType + "; charset=UTF-8");
                exchange.sendResponseHeaders(200, fileBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(fileBytes);
                os.close();
            } else {
                String error = "404 Not Found";
                exchange.sendResponseHeaders(404, error.length());
                OutputStream os = exchange.getResponseBody();
                os.write(error.getBytes());
                os.close();
            }
        }
    }
}