# Contexto del Proyecto

## Descripción General

Sistema de gestión académica desarrollado en Java utilizando Spark Java como framework web, ActiveJDBC como ORM, SQLite como base de datos y Mustache como motor de plantillas.

El objetivo del sistema es permitir la gestión de alumnos, profesores, materias y autenticación de usuarios dentro de un entorno académico.

---

# Stack Tecnológico

* Java
* Maven
* Spark Java
* ActiveJDBC
* SQLite
* Mustache
* BCrypt (hash de contraseñas)

---

# Arquitectura

El proyecto sigue una arquitectura MVC simplificada.

## Modelos

Ubicados en:

```text
src/main/java/com/is1/proyecto/models/
```

Modelos principales:

* Alumno
* Profesor
* Materia
* User

Todos heredan de ActiveJDBC.

---

## Controlador Principal

Ubicado en:

```text
src/main/java/com/is1/proyecto/App.java
```

Contiene todas las rutas HTTP del sistema.

Actualmente no existe una separación formal entre controllers y routes.

---

## Vistas

Ubicadas en:

```text
src/main/resources/templates/
```

Plantillas principales:

* login.mustache
* dashboard.mustache
* alumnoForm.mustache
* profesorForm.mustache
* adminPanel.mustache
* materiasList.mustache
* profesoresList.mustache
* user_form.mustache

---

## Base de Datos

Motor:

```text
SQLite
```

Configuración:

```java
jdbc:sqlite:./db/dev.db
```

Clase responsable:

```text
com.is1.proyecto.config.DBConfigSingleton
```

La URL de conexión puede modificarse mediante:

```bash
-Ddb.url=jdbc:sqlite:./db/prod.db
```

---

# Gestión de Conexiones

Antes de cada request:

```java
Base.open(...)
```

Después de cada request:

```java
Base.close()
```

La configuración se obtiene desde:

```java
DBConfigSingleton.getInstance()
```

---

# Funcionalidades Implementadas

## Autenticación

Rutas:

```text
GET  /
POST /login
GET  /logout
```

Características:

* Login con usuario y contraseña
* Contraseñas almacenadas con BCrypt
* Sesiones mediante Spark Session

---

## Gestión de Alumnos

Rutas:

```text
GET  /alumno/registrar
POST /alumno/registrar
POST /alumno/editar/:id
GET  /alumno/eliminar/:id
```

Permite:

* Alta
* Modificación
* Eliminación

---

## Gestión de Profesores

Rutas:

```text
GET  /profesor/registrar
POST /profesor/registrar
POST /profesor/editar/:id
GET  /profesor/eliminar/:id
GET  /profesores/listar
```

Permite:

* Alta
* Modificación
* Eliminación
* Listado

---

## Gestión de Materias

Rutas:

```text
POST /materia/registrar
POST /materia/editar/:id
GET  /materia/eliminar/:id
GET  /materias/listar
```

Permite:

* Alta
* Modificación
* Eliminación
* Listado

---

## Panel Administrativo

Ruta:

```text
GET /admin/panel
```

Permite visualizar:

* Alumnos
* Profesores

Y realizar operaciones de administración.

---

# Funcionalidades Pendientes

Según los requerimientos del proyecto:

* Inscripción a materias
* Correlatividades
* Inscripción a exámenes
* Diferenciación regular/libre
* Calendario de exámenes
* Mensajería interna
* Planes de estudio
* Buscador de materias
* Buscador de profesores
* Gestión de carreras
* Roles y permisos más completos

---

# Ejecución

Compilar:

```bash
mvn clean package
```

Ejecutar:

```bash
mvn exec:java
```

Abrir:

```text
http://localhost:8080
```

---

# Convenciones del Proyecto

* ActiveJDBC para acceso a datos.
* Mustache para renderizado de vistas.
* SQLite para persistencia.
* BCrypt para almacenamiento seguro de contraseñas.
* Configuración centralizada de base de datos mediante DBConfigSingleton.
* Las rutas están concentradas en App.java.

