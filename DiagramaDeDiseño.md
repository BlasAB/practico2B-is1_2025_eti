classDiagram

    class User {
        +int id
        +String name
        +String password
        +String correo
        +String dni
        +getName()
        +setName()
        +getPassword()
        +setPassword()
        +inscribirseAMateria()
        +inscribirseARendir()
        +consultarNotas()
        +consultarPlanDeEstudio()
        +buscarMaterias()
        +buscarProfesores()
        +consultarCalendarioExamenes()
    }

    class Profesor {
        +int id
        +String nombre
        +String apellido
        +String correo
        +String dni
        +getNombre()
        +setNombre()
        +getApellido()
        +setApellido()
        +getCorreo()
        +setCorreo()
        +getDni()
        +setDni()
        +cargarNota()
        +verListaAlumnos()
        +altaMateria()
        +modificarMateria()
        +eliminarMateria()
    }

    class Carrera {
        +int id
        +String nombre
        +String descripcion
        +getPlanDeEstudio()
    }

    class Materia {
        +int id
        +String nombre
        +String codigo
        +int anio
        +int cuatrimestre
        +tieneCorrelativasAprobadas()
        +estaDisponibleEnPeriodo()
        +alta()
        +modificar()
        +eliminar()
    }

    class Comision {
        +int id
        +String nombre
        +int anio
        +int cuatrimestre
        +getAlumnosOrdenadosAlfabeticamente()
    }

    class ExamenFinal {
        +int id
        +String tipo
        +String turno
        +Date fecha
        +estaDisponibleEnPeriodo()
        +publicarFecha()
    }

    class PeriodoInscripcion {
        +int id
        +String tipo
        +Date inicio
        +Date fin
        +int anio
        +int cuatrimestre
        +estaActivo()
    }

    class InscripcionMateria {
        +int id
        +String estado
        +Date fecha_inscripcion
        +confirmar()
        +cancelar()
    }

    class InscripcionExamen {
        +int id
        +String condicion
        +Date fecha_inscripcion
        +confirmar()
        +cancelar()
    }

    class Nota {
        +int id
        +float calificacion
        +Date fecha_carga
        +registrar()
        +modificar()
    }

    class ProfesorComision {
        +int profesor_id
        +int comision_id
    }

    Carrera "1" --> "1..*" User : pertenece
    Carrera "1" --> "1..*" Materia : incluye
    Materia "0..*" --> "0..*" Materia : correlativa de
    Materia "1" --> "1..*" Comision : tiene
    Materia "1" --> "1..*" ExamenFinal : tiene
    Profesor "1" --> "0..*" ProfesorComision : asignado
    Comision "1" --> "0..*" ProfesorComision : asignada
    User "1" --> "0..*" InscripcionMateria : realiza
    Materia "1" --> "0..*" InscripcionMateria : recibe
    PeriodoInscripcion "1" --> "0..*" InscripcionMateria : controla
    User "1" --> "0..*" InscripcionExamen : rinde
    ExamenFinal "1" --> "0..*" InscripcionExamen : agrupa
    PeriodoInscripcion "1" --> "0..*" InscripcionExamen : controla
    User "1" --> "0..*" Nota : recibe
    Materia "1" --> "0..*" Nota : tiene
    Profesor "1" --> "0..*" Nota : carga
    ExamenFinal "1" --> "0..*" Nota : genera