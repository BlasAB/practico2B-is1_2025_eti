package com.is1.proyecto.models;

import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Representa la inscripcion de un alumno a un examen final.
 * Solo puede crearse durante un periodo activo de tipo EXAMENES.
 * UNIQUE(alumno_id, examen_id) garantiza que no haya doble inscripcion.
 *
 * Preparado para futuros issues:
 *   - condicion VARCHAR(10)  → REGULAR | LIBRE (condicion con que rinde)
 *   - estado    VARCHAR(15)  → INSCRIPTO | PRESENTE | AUSENTE
 *   - nota_final REAL        → nota obtenida (actas de examen)
 *   - periodo_id INTEGER     → FK a periodos_inscripcion para trazabilidad
 */
@Table("inscripciones_examenes")
public class InscripcionExamen extends Model {

    public Integer getAlumnoId() { return getInteger("alumno_id"); }
    public void setAlumnoId(Integer v) { set("alumno_id", v); }

    public Integer getExamenId() { return getInteger("examen_id"); }
    public void setExamenId(Integer v) { set("examen_id", v); }

    public String getFechaInscripcion() { return getString("fecha_inscripcion"); }
    public void setFechaInscripcion(String fecha) { set("fecha_inscripcion", fecha); }

    // ----------------------------------------------------------------
    // Helpers estaticos para consultas frecuentes
    // ----------------------------------------------------------------

    /** Todas las inscripciones de un alumno. Util para "mis examenes". */
    public static List<InscripcionExamen> findByAlumnoId(int alumnoId) {
        return InscripcionExamen.where("alumno_id = ?", alumnoId);
    }

    /** Todas las inscripciones a un examen. Util para actas de examen. */
    public static List<InscripcionExamen> findByExamenId(int examenId) {
        return InscripcionExamen.where("examen_id = ?", examenId);
    }

    /**
     * Busca la inscripcion unica alumno-examen, o null si no existe.
     * Permite verificar doble inscripcion antes de insertar.
     */
    public static InscripcionExamen findByAlumnoIdAndExamenId(int alumnoId, int examenId) {
        return InscripcionExamen.findFirst(
                "alumno_id = ? AND examen_id = ?", alumnoId, examenId);
    }
}
