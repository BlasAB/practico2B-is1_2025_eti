-- Elimina las tablas si ya existen para asegurar un inicio limpio
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS alumnos;
DROP TABLE IF EXISTS materias;
DROP TABLE IF EXISTS profesores;
DROP TABLE IF EXISTS examenes_finales;
DROP TABLE IF EXISTS mensajes;
DROP TABLE IF EXISTS periodos_inscripcion;
DROP TABLE IF EXISTS alumnos_materias;
DROP TABLE IF EXISTS carreras;
DROP TABLE IF EXISTS carrera_materias;

-- Tabla de usuarios (autenticacion)
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL
);

-- Tabla de profesores
CREATE TABLE profesores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    correo VARCHAR(50) NOT NULL UNIQUE,
    dni VARCHAR(50) NOT NULL UNIQUE
);

-- Tabla de alumnos
CREATE TABLE alumnos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    correo VARCHAR(50) NOT NULL UNIQUE,
    dni VARCHAR(50) NOT NULL UNIQUE
);

-- Tabla de materias
CREATE TABLE materias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    descripcion TEXT
);

-- Tabla de fechas de examenes finales
-- TODO: Cuando se implemente InscripcionExamen, agregar FK hacia esta tabla.
CREATE TABLE examenes_finales (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    materia_id  INTEGER NOT NULL,
    fecha       VARCHAR(10) NOT NULL,
    hora        VARCHAR(5),
    turno       VARCHAR(20),
    observaciones TEXT,
    FOREIGN KEY (materia_id) REFERENCES materias(id)
);

-- Tabla de mensajes internos entre usuarios autenticados (alumno <-> profesor)
-- TODO: Agregar columna leido INTEGER NOT NULL DEFAULT 0 cuando se implemente bandeja.
CREATE TABLE mensajes (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    remitente_id    INTEGER NOT NULL,
    destinatario_id INTEGER NOT NULL,
    asunto          VARCHAR(200) NOT NULL,
    contenido       TEXT NOT NULL,
    fecha_envio     VARCHAR(19) NOT NULL,
    FOREIGN KEY (remitente_id)    REFERENCES users(id),
    FOREIGN KEY (destinatario_id) REFERENCES users(id)
);

-- Tabla de periodos de inscripcion
-- tipo en {MATERIAS, EXAMENES}
CREATE TABLE periodos_inscripcion (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre       VARCHAR(100) NOT NULL,
    tipo         VARCHAR(20)  NOT NULL,
    fecha_inicio VARCHAR(10)  NOT NULL,
    fecha_fin    VARCHAR(10)  NOT NULL
);

-- Tabla de inscripciones alumno-materia (relacion N:M con datos de cursado).
-- Cada fila representa que un alumno esta inscripto en una materia.
-- El campo nota es NULL hasta que el profesor la cargue (0.0 a 10.0).
-- El campo fecha_inscripcion se completa al momento de la inscripcion (issue #5).
--
-- Preparado para futuros issues:
--   condicion  VARCHAR(10)  → REGULAR | LIBRE
--   estado     VARCHAR(15)  → CURSANDO | APROBADA | REPROBADA
--   periodo_id INTEGER      → FK a periodos_inscripcion
--
-- UNIQUE(alumno_id, materia_id) evita inscripciones duplicadas.
-- Si se requieren inscripciones por periodo, ampliar a UNIQUE(alumno_id, materia_id, periodo_id).
CREATE TABLE alumnos_materias (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    alumno_id         INTEGER NOT NULL,
    materia_id        INTEGER NOT NULL,
    nota              REAL,
    fecha_inscripcion VARCHAR(10),
    FOREIGN KEY (alumno_id)  REFERENCES alumnos(id),
    FOREIGN KEY (materia_id) REFERENCES materias(id),
    UNIQUE(alumno_id, materia_id)
);

-- Tabla de carreras (issue #4).
-- Preparada para futuros issues:
--   duracion_anios INTEGER  → cantidad de años del plan
--   descripcion    TEXT
--   multiple planes por carrera → agregar tabla planes_estudio con carrera_id
CREATE TABLE carreras (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    codigo VARCHAR(20)  NOT NULL UNIQUE
);

-- Tabla de relacion carrera-materia con organizacion por año (issue #4).
-- anio: año del plan en que se dicta la materia (1, 2, 3...). NULL = sin organizar.
-- orden: posicion dentro del año para ordenar la visualizacion. NULL = sin orden definido.
--
-- Preparado para futuros issues:
--   cuatrimestre INTEGER      → 1 | 2 (agregar columna)
--   correlativa_id INTEGER    → FK a materias(id) para correlatividades
--   obligatoria INTEGER       → 1 = obligatoria, 0 = electiva
CREATE TABLE carrera_materias (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    carrera_id INTEGER NOT NULL,
    materia_id INTEGER NOT NULL,
    anio       INTEGER,
    orden      INTEGER,
    FOREIGN KEY (carrera_id) REFERENCES carreras(id),
    FOREIGN KEY (materia_id) REFERENCES materias(id),
    UNIQUE(carrera_id, materia_id)
);
