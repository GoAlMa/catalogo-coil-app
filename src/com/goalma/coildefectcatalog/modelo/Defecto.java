package com.goalma.coildefectcatalog.modelo;

/**
 * Representa un defecto de pintura del catálogo.
 * Relaciona con Categoria y PatronVisual guardando el objeto completo
 * (no solo el id), porque un Defecto "tiene una" Categoria, no solo
 * "conoce su número" - es más representativo del dominio real.
 */
public class Defecto {

    private int id;
    private String nombre;
    private Categoria categoria;
    private PatronVisual patronVisual;
    private String descripcion;
    private String codigoIso; // puede ser null, no todos los defectos tienen referencia ISO

    public Defecto() {
    }

    // Constructor para crear un defecto nuevo (sin id todavía)
    public Defecto(String nombre, Categoria categoria, PatronVisual patronVisual,
                    String descripcion, String codigoIso) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.patronVisual = patronVisual;
        this.descripcion = descripcion;
        this.codigoIso = codigoIso;
    }

    // Constructor completo, para reconstruir un defecto que ya existe en la BD
    public Defecto(int id, String nombre, Categoria categoria, PatronVisual patronVisual,
                    String descripcion, String codigoIso) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.patronVisual = patronVisual;
        this.descripcion = descripcion;
        this.codigoIso = codigoIso;
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

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public PatronVisual getPatronVisual() {
        return patronVisual;
    }

    public void setPatronVisual(PatronVisual patronVisual) {
        this.patronVisual = patronVisual;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCodigoIso() {
        return codigoIso;
    }

    public void setCodigoIso(String codigoIso) {
        this.codigoIso = codigoIso;
    }

    @Override
    public String toString() {
        return "Defecto{id=" + id + ", nombre='" + nombre + "', categoria=" + categoria
                + ", patronVisual=" + patronVisual + ", codigoIso='" + codigoIso + "'}";
    }
}