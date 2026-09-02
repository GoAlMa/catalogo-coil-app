package com.goalma.coildefectcatalog.modelo;

/**
 * Representa una categoría de defecto (adherencia, brillo, cráteres...).
 * Clase modelo simple (POJO): solo datos, sin lógica de negocio ni de BD.
 */
public class Categoria {

    private int id;
    private String nombre;

    // Constructor vacío, útil cuando el DAO va a rellenar los datos
    // después de leerlos de la base de datos.
    public Categoria() {
    }

    // Constructor sin id, para cuando creas una categoría nueva
    // que todavía no existe en la base de datos (el id lo genera Oracle).
    public Categoria(String nombre) {
        this.nombre = nombre;
    }

    // Constructor completo, para cuando reconstruyes el objeto
    // a partir de una fila ya existente en la base de datos.
    public Categoria(int id, String nombre) {
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
        return "Categoria{id=" + id + ", nombre='" + nombre + "'}";
    }
}