package com.goalma.coildefectcatalog.modelo;

/**
 * Representa el patrón visual de un defecto (puntual, longitudinal, difuso).
 */
public class PatronVisual {

    private int id;
    private String nombre;

    public PatronVisual() {
    }

    public PatronVisual(String nombre) {
        this.nombre = nombre;
    }

    public PatronVisual(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String toString() {
        return "PatronVisual{id=" + id + ", nombre='" + nombre + "'}";
    }
}
