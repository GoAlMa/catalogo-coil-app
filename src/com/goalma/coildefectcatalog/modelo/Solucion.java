package com.goalma.coildefectcatalog.modelo;

/**
 * Representa una solución/acción correctiva para un defecto.
 * Mismo patrón que Causa: guarda un único Defecto completo, no una lista,
 * y Defecto no guarda Solucion dentro de sí mismo (evita referencia circular).
 */
public class Solucion {

    private int id;
    private Defecto defecto;
    private String descripcion;

    public Solucion() {
    }

    public Solucion(Defecto defecto, String descripcion) {
        this.defecto = defecto;
        this.descripcion = descripcion;
    }

    public Solucion(int id, Defecto defecto, String descripcion) {
        this.id = id;
        this.defecto = defecto;
        this.descripcion = descripcion;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        String nombreDefecto = (defecto != null) ? defecto.getNombre() : "null";
        return "Solucion{id=" + id + ", defecto='" + nombreDefecto + "', descripcion='" + descripcion + "'}";
    }
}