# Sistema de Gestión Académica

Proyecto desarrollado con Java, Spark Java, ActiveJDBC, SQLite y Mustache.

---

# Requisitos

Instalar:

* Java 17 o superior
* Maven 3.9 o superior
* Git

Verificar instalación:

```bash
java --version
mvn --version
git --version
```

---

# Clonar el proyecto

```bash
git clone <url-del-repositorio>
cd practico2B-is1_2025_eti
```

---

# Instalar dependencias

Maven descargará automáticamente las dependencias definidas en el `pom.xml`.

Para compilar:

```bash
mvn clean package
```

---

# Ejecutar el proyecto

Iniciar servidor:

```bash
mvn exec:java
```

La aplicación quedará disponible en:

```text
http://localhost:8080
```

---

# Base de Datos

El proyecto utiliza SQLite.

Base de desarrollo:

```text
db/dev.db
```

Base de producción:

```text
db/prod.db
```

La configuración se encuentra centralizada en:

```text
src/main/java/com/is1/proyecto/config/DBConfigSingleton.java
```

---

# Cambiar entre entornos

## Desarrollo

```bash
mvn exec:java
```

Utiliza:

```text
jdbc:sqlite:./db/dev.db
```

## Producción

```bash
mvn exec:java -Ddb.url=jdbc:sqlite:./db/prod.db
```

Utiliza:

```text
jdbc:sqlite:./db/prod.db
```

---

# Estructura principal

```text
src/
├── main/
│   ├── java/
│   │   └── com/is1/proyecto/
│   └── resources/
│       └── templates/
├── test/
└── db/
```

---

# Solución de errores comunes

## Error: mvn: command not found

Instalar Maven:

```bash
sudo apt update
sudo apt install maven
```

Verificar:

```bash
mvn --version
```

---

## Error: java: command not found

Instalar Java:

```bash
sudo apt install openjdk-17-jdk
```

Verificar:

```bash
java --version
```

---

## Error: Address already in use :8080

Existe otro proceso utilizando el puerto.

Finalizar proceso:

```bash
lsof -i :8080
kill -9 <PID>
```

---

## Error al conectar SQLite

Verificar existencia de:

```text
db/dev.db
```

o

```text
db/prod.db
```

según el entorno utilizado.

---

# Tecnologías utilizadas

* Java
* Maven
* Spark Java
* ActiveJDBC
* SQLite
* Mustache
* BCrypt
* JUnit (próximamente)

```
```

