package com.goalma.coildefectcatalog.app;

import com.goalma.coildefectcatalog.dao.CategoriaDAO;
import com.goalma.coildefectcatalog.modelo.Categoria;

import java.util.List;

/**
 * Clase temporal SOLO para probar que CategoriaDAO conecta bien con Oracle.
 * No es el menú final del proyecto - eso vendrá después con Scanner.
 * Se puede borrar o reemplazar más adelante.
 */
public class PruebaConexion {

    public static void main(String[] args) {
        try {
            CategoriaDAO dao = new CategoriaDAO();
            List<Categoria> categorias = dao.listarTodas();

            System.out.println("Conexión OK. Categorías encontradas: " + categorias.size());
            for (Categoria c : categorias) {
                System.out.println(c);
            }

        } catch (Exception e) {
            System.out.println("ERROR al conectar o leer datos:");
            e.printStackTrace();
        }
    }
}