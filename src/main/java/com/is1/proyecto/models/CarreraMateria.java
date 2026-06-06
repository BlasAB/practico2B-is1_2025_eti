package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Relacion entre una carrera y una materia dentro de su plan de estudios.
 * anio:  año del plan en que se dicta (1, 2, 3...). NULL = sin organizar por año.
 * orden: posicion dentro del año para ordenar la visualizacion.
 *
 * Preparado para futuros issues:
 *   - cuatrimestre INTEGER → 1 | 2
 *   - correlativa_id      → FK a materias(id) para correlatividades
 *   - obligatoria INTEGER  → 1 = obligatoria, 0 = electiva
 */
@Table("carrera_materias")
public class CarreraMateria extends Model {

    public Integer getCarreraId() { return getInteger("carrera_id"); }
    public void setCarreraId(Integer v) { set("carrera_id", v); }

    public Integer getMateriaId() { return getInteger("materia_id"); }
    public void setMateriaId(Integer v) { set("materia_id", v); }

    /** Anio del plan de estudios. Puede ser null. */
    public Integer getAnio() { return getInteger("anio"); }
    public void setAnio(Integer anio) { set("anio", anio); }

    /** Orden dentro del año para visualizacion. Puede ser null. */
    public Integer getOrden() { return getInteger("orden"); }
    public void setOrden(Integer orden) { set("orden", orden); }
}
