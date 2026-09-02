package com.goalma.coildefectcatalog.modelo;

import java.time.LocalDate;

/**
 * Representa una foto asociada a un defecto.
 * IMPORTANTE: defecto puede ser null - representa una foto subida
 * pero todavía sin clasificar (caso de uso real: cientos de fotos
 * que se suben primero y se asignan a un defecto más adelante).
 * Por eso el toString() comprueba null antes de leer datos del defecto.
 */
public class Foto {

    private int id;
    private Defecto defecto; // puede ser null: foto pendiente de clasificar
    private String rutaArchivo;
    private LocalDate fecha;
    private String notas; // puede ser null, no todas las fotos llevan nota

    public Foto() {
    }

    // Constructor para una foto nueva sin clasificar todavía (defecto = null)
    public Foto(String rutaArchivo, LocalDate fecha, String notas) {
        this.defecto = null;
        this.rutaArchivo = rutaArchivo;
        this.fecha = fecha;
        this.notas = notas;
    }

    // Constructor para una foto nueva ya clasificada (con defecto asignado)
    public Foto(Defecto defecto, String rutaArchivo, LocalDate fecha, String notas) {
        this.defecto = defecto;
        this.rutaArchivo = rutaArchivo;
        this.fecha = fecha;
        this.notas = notas;
    }

    // Constructor completo, para reconstruir una foto que ya existe en la BD
    public Foto(int id, Defecto defecto, String rutaArchivo, LocalDate fecha, String notas) {
        this.id = id;
        this.defecto = defecto;
        this.rutaArchivo = rutaArchivo;
        this.fecha = fecha;
        this.notas = notas;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Defecto getDefecto() {
        return defecto;
    }

    public void setDefecto(Defecto defecto) {
        this.defecto = defecto;
    }

    // Método de conveniencia: útil en el menú Scanner para saber
    // rápidamente si una foto está pendiente de clasificar o no.
    public boolean estaClasificada() {
        return defecto != null;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    @Override
    public String toString() {
        // Comprobación de null obligatoria: si no existiera, esta línea
        // reventaría con NullPointerException en cuanto imprimas una foto
        // sin clasificar (defecto = null).
        String nombreDefecto = (defecto != null) ? defecto.getNombre() : "SIN CLASIFICAR";
        return "Foto{id=" + id + ", defecto='" + nombreDefecto + "', ruta='" + rutaArchivo
                + "', fecha=" + fecha + "}";
    }
}