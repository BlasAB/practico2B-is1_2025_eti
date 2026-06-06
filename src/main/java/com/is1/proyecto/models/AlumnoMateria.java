package com.is1.proyecto.models;

import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Representa la inscripcion de un alumno a una materia.
 * Una fila existe cuando el alumno se inscribio a la materia.
 * El campo "nota" es NULL hasta que el profesor la cargue.
 * El campo "fecha_inscripcion" se completa al momento de inscribirse (issue #5).
 *
 * Preparado para futuros issues:
 *   - condicion (REGULAR / LIBRE)              → agregar columna VARCHAR
 *   - estado (CURSANDO / APROBADA / REPROBADA) → agregar columna VARCHAR
 *   - periodo_id                               → agregar FK a periodos_inscripcion
 *   - promedio academico                       → usar getNota() sobre el conjunto filtrado
 *   - historial academico                      → findByAlumnoId()
 *   - actas de cursado                         → findByMateriaId()
 */
@Table("alumnos_materias")
public class AlumnoMateria extends Model {

    public Integer getAlumnoId()  { return getInteger("alumno_id"); }
    public void setAlumnoId(Integer v)  { set("alumno_id", v); }

    public Integer getMateriaId() { return getInteger("materia_id"); }
    public void setMateriaId(Integer v) { set("materia_id", v); }

    /**
     * Devuelve la nota como Double, o null si no fue cargada todavia.
     */
    public Double getNota() {
        Object v = get("nota");
        if (v == null) return null;
        try { return Double.parseDouble(v.toString()); }
        catch (NumberFormatException e) { return null; }
    }
    public void setNota(Double nota) { set("nota", nota); }

    /** Fecha en formato YYYY-MM-DD en que el alumno se inscribio. */
    public String getFechaInscripcion() { return getString("fecha_inscripcion"); }
    public void setFechaInscripcion(String fecha) { set("fecha_inscripcion", fecha); }

    // ----------------------------------------------------------------
    // Helpers estaticos para consultas frecuentes
    // ----------------------------------------------------------------

    public static List<AlumnoMateria> findByMateriaId(int materiaId) {
        return AlumnoMateria.where("materia_id = ?", materiaId);
    }

    public static List<AlumnoMateria> findByAlumnoId(int alumnoId) {
        return AlumnoMateria.where("alumno_id = ?", alumnoId);
    }

    public static AlumnoMateria findByAlumnoIdAndMateriaId(int alumnoId, int materiaId) {
        return AlumnoMateria.findFirst(
                "alumno_id = ? AND materia_id = ?", alumnoId, materiaId);
    }
}
