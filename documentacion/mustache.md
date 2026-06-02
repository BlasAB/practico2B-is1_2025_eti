# Spark Java + Mustache

## Introducción

Mustache es un motor de plantillas que permite generar páginas HTML dinámicas utilizando datos enviados desde una aplicación Java.

En este proyecto se utiliza junto con Spark Java mediante la clase:

```java
MustacheTemplateEngine
```

---

## Dependencia utilizada

```xml
<dependency>
    <groupId>com.sparkjava</groupId>
    <artifactId>spark-template-mustache</artifactId>
    <version>2.7.1</version>
</dependency>
```

---

## Ubicación de las plantillas

Las vistas se almacenan en:

```text
src/main/resources/templates/
```

Ejemplos del proyecto:

```text
login.mustache
dashboard.mustache
alumnoForm.mustache
adminPanel.mustache
materiasList.mustache
```

---

## Renderizado de una vista

Ejemplo tomado de la estructura actual del proyecto:

```java
get("/dashboard", (req, res) -> {

    Map<String, Object> model = new HashMap<>();

    model.put("username", "Cristian");

    return new ModelAndView(
        model,
        "dashboard.mustache"
    );

}, new MustacheTemplateEngine());
```

---

## Paso de variables a la plantilla

Las variables se agregan al modelo mediante:

```java
model.put("username", "Cristian");
```

Dentro de la plantilla:

```html
<h1>Bienvenido {{username}}</h1>
```

Resultado renderizado:

```html
<h1>Bienvenido Cristian</h1>
```

---

## Ejemplo completo

### Ruta Spark

```java
get("/ejemplo", (req, res) -> {

    Map<String, Object> model = new HashMap<>();

    model.put("nombre", "Juan");
    model.put("apellido", "Perez");

    return new ModelAndView(
        model,
        "ejemplo.mustache"
    );

}, new MustacheTemplateEngine());
```

### Plantilla Mustache

```html
<!DOCTYPE html>
<html>

<body>

<h1>{{nombre}} {{apellido}}</h1>

</body>

</html>
```

### Resultado

```html
<h1>Juan Perez</h1>
```

---

## Listas

Es posible recorrer colecciones.

### Java

```java
List<String> materias = Arrays.asList(
    "Matemática",
    "Programación",
    "Base de Datos"
);

model.put("materias", materias);
```

### Mustache

```html
<ul>
{{#materias}}
    <li>{{.}}</li>
{{/materias}}
</ul>
```

---

## Ventajas

* Fácil de aprender.
* Integración sencilla con Spark Java.
* Separación entre lógica y presentación.
* Bajo consumo de recursos.

---

## Uso en este proyecto

Actualmente las vistas principales son renderizadas utilizando:

```java
new MustacheTemplateEngine()
```

y reciben datos mediante objetos `Map<String,Object>` enviados desde las rutas definidas en `App.java`.

