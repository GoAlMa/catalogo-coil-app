package com.goalma.coildefectcatalog.app;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServidorWeb {

    public static void main(String[] args) throws IOException {
        // Forzamos explícitamente a leer la variable o el volumen de Railway
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

        Path dirStorage = Paths.get(rutaElegida);
        Path dirUploads = dirStorage.resolve("uploads");
        Path archivoJson = dirStorage.resolve("defectos.json");

        // Creamos directorios si no existen
        Files.createDirectories(dirStorage);
        Files.createDirectories(dirUploads);

        // Si el archivo JSON no existe, lo inicializamos vacío
        if (!Files.exists(archivoJson)) {
            Files.writeString(archivoJson, "[]", StandardCharsets.UTF_8);
        }

        // Comprobación de permisos de escritura real en el volumen
        try {
            Path testFile = dirStorage.resolve("test_persistencia.tmp");
            Files.writeString(testFile, "test", StandardCharsets.UTF_8);
            Files.deleteIfExists(testFile);
            System.out.println(">>> [OK] ¡Escritura en el volumen persistente verificada con éxito!");
        } catch (Exception e) {
            System.err.println(">>> [ERROR CRITICO] NO SE PUEDE ESCRIBIR EN EL VOLUMEN: " + e.getMessage());
        }

        // Configuración del puerto del servidor web
        int puerto = 8080;
        String puertoEnv = System.getenv("PORT");
        if (puertoEnv != null && !puertoEnv.isEmpty()) {
            puerto = Integer.parseInt(puertoEnv);
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);

        // Aquí puedes registrar tus contextos y endpoints habituales...
        // Ejemplo: servidor.createContext("/api/defectos", new DefectosHandler(archivoJson, dirUploads));

        servidor.setExecutor(null); // crea un executor por defecto
        servidor.start();

        System.out.println(">>> [SERVIDOR] Servidor iniciado correctamente en el puerto " + puerto);
    }
}