package com.is1.proyecto.models;

import java.time.LocalDate;
import java.util.List;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("periodos_inscripcion")
public class PeriodoInscripcion extends Model {

    // ----------------------------------------------------------------
    // Getters y setters — mismo estilo que Materia / ExamenFinal
    // ----------------------------------------------------------------

    public String getNombre() { return getString("nombre"); }
    public void setNombre(String nombre) { set("nombre", nombre); }

    public String getTipo() { return getString("tipo"); }
    public void setTipo(String tipo) { set("tipo", tipo); }

    public String getFechaInicio() { return getString("fecha_inicio"); }
    public void setFechaInicio(String fechaInicio) { set("fecha_inicio", fechaInicio); }

    public String getFechaFin() { return getString("fecha_fin"); }
    public void setFechaFin(String fechaFin) { set("fecha_fin", fechaFin); }

    // ----------------------------------------------------------------
    // Estado calculado en base a la fecha actual
    // Devuelve: PENDIENTE | ACTIVO | FINALIZADO
    // ----------------------------------------------------------------

    public String getEstado() {
        try {
            LocalDate hoy      = LocalDate.now();
            LocalDate inicio   = LocalDate.parse(getFechaInicio());
            LocalDate fin      = LocalDate.parse(getFechaFin());

            if (hoy.isBefore(inicio)) return "PENDIENTE";
            if (hoy.isAfter(fin))     return "FINALIZADO";
            return "ACTIVO";
        } catch (Exception e) {
            return "DESCONOCIDO";
        }
    }

    // ----------------------------------------------------------------
    // Helpers de integración futura
    // Devuelven el período ACTIVO para cada tipo, o null si no hay ninguno.
    // Uso: PeriodoInscripcion p = PeriodoInscripcion.getPeriodoActivoMaterias();
    // ----------------------------------------------------------------

    public static PeriodoInscripcion getPeriodoActivoMaterias() {
        return getPeriodoActivo("MATERIAS");
    }

    public static PeriodoInscripcion getPeriodoActivoExamenes() {
        return getPeriodoActivo("EXAMENES");
    }

    // ----------------------------------------------------------------
    // Método privado compartido por los dos helpers anteriores
    // ----------------------------------------------------------------

    private static PeriodoInscripcion getPeriodoActivo(String tipo) {
        List<PeriodoInscripcion> periodos = PeriodoInscripcion.where("tipo = ?", tipo);
        for (PeriodoInscripcion p : periodos) {
            if ("ACTIVO".equals(p.getEstado())) {
                return p;
            }
        }
        return null;
    }
}
