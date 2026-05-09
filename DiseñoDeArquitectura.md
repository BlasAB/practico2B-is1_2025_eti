graph TD

    Cliente["Cliente / usuario"]

    subgraph Spark["App.java — Spark framework"]
        subgraph Rutas["Rutas definidas en App.java"]
            Auth["Auth\nlogin · logout · registro"]
            Alumnos["Alumnos\nABM · dashboard"]
            Profesores["Profesores\nABM · lista alumnos"]
            Materias["Materias\nABM · plan estudio · buscador"]
            Inscripciones["Inscripciones\nmaterias · exámenes"]
            Notas["Notas\ncargar · consultar"]
            Examenes["Exámenes\ncalendario · fechas"]
        end
    end

    subgraph Models["Models — ActiveJDBC"]
        User["User"]
        Profesor["Profesor"]
        Carrera["Carrera"]
        Materia["Materia"]
        Comision["Comision"]
        ExamenFinal["ExamenFinal"]
        InscripcionMateria["InscripcionMateria"]
        InscripcionExamen["InscripcionExamen"]
        Nota["Nota"]
        Periodo["PeriodoInscripcion"]
        ProfesorComision["ProfesorComision"]
    end

    subgraph DB["SQLite"]
        DevDB["dev.db"]
        ProdDB["prod.db"]
        Config["DBConfigSingleton"]
    end

    Cliente --> Spark
    Spark --> Rutas
    Rutas --> Models
    Models --> DB