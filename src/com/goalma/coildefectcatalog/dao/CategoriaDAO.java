package com.goalma.coildefectcatalog.dao;
import com.goalma.coildefectcatalog.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

    /**
     * DAO (Data Access Object) para la entidad Categoria.
     * Aquí vive todo el SQL relacionado con categorías - ninguna otra
     * clase del proyecto debería escribir SQL directamente para esta tabla.
     */
    public class CategoriaDAO {

        /**
         * Devuelve todas las categorías guardadas en la base de datos.
         */
        public List<Categoria> listarTodas() throws SQLException {
            List<Categoria> categorias = new ArrayList<>();
            String sql = "SELECT id, nombre FROM categoria ORDER BY nombre";

            // try-with-resources: cierra automáticamente la conexión y el
            // statement al terminar, aunque salte una excepción por el camino.
            try (Connection conexion = Conexion.obtenerConexion();
                 PreparedStatement stmt = conexion.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    categorias.add(new Categoria(id, nombre));
                }
            }

            return categorias;
        }

        /**
         * Guarda una categoría nueva en la base de datos.
         * Devuelve el mismo objeto Categoria pero con el id ya asignado
         * por Oracle (útil si necesitas usarlo justo después, por ejemplo
         * para crear un Defecto que apunte a esta categoría recién creada).
         */
        public Categoria guardar(Categoria categoria) throws SQLException {
            String sql = "INSERT INTO categoria (nombre) VALUES (?)";

            try (Connection conexion = Conexion.obtenerConexion();
                 // Statement.RETURN_GENERATED_KEYS: le pide a Oracle que nos
                 // devuelva el id que acaba de generar automáticamente.
                 PreparedStatement stmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, categoria.getNombre());
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        categoria.setId(generatedKeys.getInt(1));
                    }
                }
            }

            return categoria;
        }
    }

