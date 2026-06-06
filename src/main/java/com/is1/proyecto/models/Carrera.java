package com.is1.proyecto.models;

import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Representa una carrera universitaria.
 * Preparado para futuros issues:
 *   - multiples planes de estudio → agregar tabla planes_estudio con FK carrera_id
 *   - duracion en anios           → agregar columna duracion_anios INTEGER
 *   - descripcion                 → agregar columna descripcion TEXT
 */
@Table("carreras")
public class Carrera extends Model {

    public String getNombre() { return getString("nombre"); }
    public void setNombre(String nombre) { set("nombre", nombre); }

    public String getCodigo() { return getString("codigo"); }
    public void setCodigo(String codigo) { set("codigo", codigo); }

    /**
     * Devuelve todas las relaciones carrera-materia de esta carrera,
     * ordenadas por anio y orden.
     * Util para construir el plan de estudios.
     */
    public static List<CarreraMateria> getMateriasDe(int carreraId) {
        return CarreraMateria.where(
                "carrera_id = ? ORDER BY anio ASC, orden ASC", carreraId);
    }
}
