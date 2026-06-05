package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("examenes_finales")
public class ExamenFinal extends Model {

    public Integer getMateriaId() { return getInteger("materia_id"); }
    public void setMateriaId(Integer materiaId) { set("materia_id", materiaId); }

    public String getFecha() { return getString("fecha"); }
    public void setFecha(String fecha) { set("fecha", fecha); }

    public String getHora() { return getString("hora"); }
    public void setHora(String hora) { set("hora", hora); }

    public String getTurno() { return getString("turno"); }
    public void setTurno(String turno) { set("turno", turno); }

    public String getObservaciones() { return getString("observaciones"); }
    public void setObservaciones(String observaciones) { set("observaciones", observaciones); }

}
