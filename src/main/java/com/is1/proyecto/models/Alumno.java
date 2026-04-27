package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("alumnos")
public class Alumno extends Model {
    public String getNombre()   { return getString("nombre"); }
    public void setNombre(String v)   { set("nombre", v); }
    public String getApellido() { return getString("apellido"); }
    public void setApellido(String v) { set("apellido", v); }
    public String getCorreo()   { return getString("correo"); }
    public void setCorreo(String v)   { set("correo", v); }
    public String getDni()      { return getString("dni"); }
    public void setDni(String v)      { set("dni", v); }
}