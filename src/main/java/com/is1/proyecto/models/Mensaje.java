package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("mensajes")
public class Mensaje extends Model {

    public Integer getRemitenteId() { return getInteger("remitente_id"); }
    public void setRemitenteId(Integer id) { set("remitente_id", id); }

    public Integer getDestinatarioId() { return getInteger("destinatario_id"); }
    public void setDestinatarioId(Integer id) { set("destinatario_id", id); }

    public String getAsunto() { return getString("asunto"); }
    public void setAsunto(String asunto) { set("asunto", asunto); }

    public String getContenido() { return getString("contenido"); }
    public void setContenido(String contenido) { set("contenido", contenido); }

    public String getFechaEnvio() { return getString("fecha_envio"); }
    public void setFechaEnvio(String fechaEnvio) { set("fecha_envio", fechaEnvio); }

    // TODO: Cuando se implemente Bandeja de Entrada, agregar campo leido (0/1) y
    //       métodos marcarLeido() / esLeido() para soportar el estado de lectura.

}
