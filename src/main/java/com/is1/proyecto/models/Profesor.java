package com.is1.proyecto.models;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("profesores")
public class Profesor extends Model {
    public String getNombre() { return getString("nombre"); }
    public void setNombre(String nombre) { set("nombre", nombre); }
    public String getApellido() { return getString("apellido"); }
    public void setApellido(String apellido) { set("apellido", apellido); }
    public String getCorreo() { return getString("correo"); }
    public void setCorreo(String correo) { set("correo", correo); }
    public String getDni() { return getString("dni"); }
    public void setDni(String dni) { set("dni", dni); }
}