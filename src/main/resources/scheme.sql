-- Elimina las tablas si ya existen para asegurar un inicio limpio
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS alumnos;
DROP TABLE IF EXISTS materias;
DROP TABLE IF EXISTS profesores;
DROP TABLE IF EXISTS examenes_finales;
DROP TABLE IF EXISTS mensajes;

-- Tabla de usuarios (autenticación)
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

-- Tabla de fechas de exámenes finales
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

-- Tabla de mensajes internos entre usuarios autenticados (alumno ↔ profesor)
-- remitente_id y destinatario_id referencian users.id (tabla de autenticación unificada).
-- TODO: Cuando se implemente Bandeja de Entrada, agregar columna:
--       leido INTEGER NOT NULL DEFAULT 0
--       para soportar el estado de lectura de cada mensaje.
CREATE TABLE mensajes (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    remitente_id    INTEGER NOT NULL,
    destinatario_id INTEGER NOT NULL,
    asunto          VARCHAR(200) NOT NULL,
    contenido       TEXT NOT NULL,
    fecha_envio     VARCHAR(19) NOT NULL,  -- Formato: YYYY-MM-DD HH:MM:SS
    FOREIGN KEY (remitente_id)    REFERENCES users(id),
    FOREIGN KEY (destinatario_id) REFERENCES users(id)
);
