package com.is1.proyecto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.AlumnoMateria;
import com.is1.proyecto.models.Carrera;
import com.is1.proyecto.models.CarreraMateria;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.InscripcionExamen;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Mensaje;
import com.is1.proyecto.models.PeriodoInscripcion;
import com.is1.proyecto.models.Profesor;
import com.is1.proyecto.models.User;

import spark.ModelAndView;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.post;
import spark.template.mustache.MustacheTemplateEngine;

public class App {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        port(8080);

        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        before((req, res) -> {
            if (!Base.hasConnection()) {
                Base.open(
                    dbConfig.getDriver(),
                    dbConfig.getDbUrl(),
                    dbConfig.getUser(),
                    dbConfig.getPass()
                );
                System.out.println("[DB] ActiveJDBC conectado correctamente");
            }
        });

        after((req, res) -> {
            if (Base.hasConnection()) {
                try {
                    Base.close();
                    System.out.println("[DB] Conexión cerrada correctamente");
                } catch (Exception e) {
                    System.err.println("Error al cerrar conexion: " + e.getMessage());
                }
            }
        });


        before((req, res) -> {
            String path = req.pathInfo();
            if (path.equals("/")
                    || path.equals("/login")
                    || path.equals("/admin/login")
                    || path.equals("/admin/registrar")   // ✅ agregalo acá
                    || path.equals("/logout")
                    || path.equals("/alumno/registrar")
                    || path.equals("/profesor/registrar")
                    || path.startsWith("/materia/")
                    || path.startsWith("/materias/")
                    || path.startsWith("/alumnos/")
                    || path.startsWith("/profesores/")) {
                return;
            }

            Boolean loggedIn = req.session().attribute("loggedIn");
            String username  = req.session().attribute("currentUserUsername");
            Object userId    = req.session().attribute("userId");
            if (loggedIn == null || !loggedIn || username == null || userId == null) {
                res.redirect("/?error=" + java.net.URLEncoder.encode("Debes iniciar sesion para acceder a esta pagina.", "UTF-8"));
                halt();
            }
        });


        // ============================================================
        // AUTENTICACIÓN
        // ============================================================

        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String error = req.queryParams("error");
            String msg   = req.queryParams("message");
            String tipo  = req.queryParams("tipo");
            if (error != null && !error.isEmpty())
                model.put("errorMessage", error);
            if (msg != null && !msg.isEmpty())
                model.put("successMessage", msg);
            if (tipo != null && !tipo.isEmpty()) {
                model.put("tipo", tipo);
                if ("alumno".equals(tipo))   model.put("esAlumno", true);
                if ("profesor".equals(tipo)) model.put("esProfesor", true);
            }
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine());

        get("/login", (req, res) -> {
            res.redirect("/");
            return null;
        });




        get("/admin/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String err = req.queryParams("error");
            String msg = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("errorMessage", err);
            if (msg != null && !msg.isEmpty()) model.put("successMessage", msg);
            model.put("tipo", "admin");
            model.put("esAdmin", true);
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine());

        post("/admin/login", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            Map<String, Object> model = new HashMap<>();
            model.put("tipo", "admin");
            model.put("esAdmin", true);

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                model.put("errorMessage", "El nombre de usuario y la contraseña son requeridos.");
                return new ModelAndView(model, "login.mustache");
            }

            User user = User.findFirst("name = ? AND perfil_tipo = ?", username, "admin");
            if (user != null && BCrypt.checkpw(password, user.getString("password"))) {
                req.session(true).attribute("currentUserUsername", user.getString("name"));
                req.session().attribute("userId", user.getId());
                req.session().attribute("loggedIn", true);
                req.session().attribute("userTipo", "admin");
                res.redirect("/admin/panel");
                return null;
            }

            res.status(401);
            model.put("errorMessage", "Usuario o contraseña incorrectos.");
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine());


        // 🔹 Registro admin
        get("/admin/registrar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("errorMessage", err);
            return new ModelAndView(model, "adminForm.mustache");
        }, new MustacheTemplateEngine());

        post("/admin/registrar", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.redirect("/admin/registrar?error=" + URLEncoder.encode(
                    "El nombre de usuario y la contraseña son requeridos.", StandardCharsets.UTF_8));
                return null;
            }

            try {
                User existing = User.findFirst("name = ? AND perfil_tipo = ?", username, "admin");
                if (existing != null) {
                    res.redirect("/admin/registrar?error=" + URLEncoder.encode(
                        "Ya existe un admin con ese nombre.", StandardCharsets.UTF_8));
                    return null;
                }

                User admin = new User();
                admin.set("name", username);
                admin.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
                admin.set("perfil_tipo", "admin");
                admin.saveIt();

                res.redirect("/admin/login?message=" + URLEncoder.encode(
                    "Admin registrado correctamente. Ya podés iniciar sesión.", StandardCharsets.UTF_8));
            } catch (Exception e) {
                System.err.println("ERROR al registrar admin: " + e.getMessage());
                res.redirect("/admin/registrar?error=" + URLEncoder.encode(
                    "No se pudo registrar el admin.", StandardCharsets.UTF_8));
            }
            return null;
        });

        post("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String tipo     = req.queryParams("tipo");
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                model.put("errorMessage", "El nombre de usuario y la contraseña son requeridos.");
                addTipo(model, tipo);
                return new ModelAndView(model, "login.mustache");
            }

            User ac = User.findFirst("name = ?", username);
            if (ac == null) {
                res.status(401);
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                addTipo(model, tipo);
                return new ModelAndView(model, "login.mustache");
            }

            // Validar contraseña con BCrypt
            if (BCrypt.checkpw(password, ac.getString("password"))) {
                Integer perfilId = null;
                Object perfilIdObj = ac.get("perfil_id");
                if (perfilIdObj instanceof Number) {
                    perfilId = ((Number) perfilIdObj).intValue();
                } else if (perfilIdObj instanceof String) {
                    try { perfilId = Integer.parseInt((String) perfilIdObj); } catch (NumberFormatException ignored) {}
                }

                // Comparación flexible del tipo de perfil
                String perfilTipo = ac.getString("perfil_tipo");
                if (perfilTipo != null && tipo != null && !perfilTipo.trim().equalsIgnoreCase(tipo.trim())) {
                    res.status(401);
                    model.put("errorMessage", "Usuario o contraseña incorrectos.");
                    addTipo(model, tipo);
                    return new ModelAndView(model, "login.mustache");
                }

                // Crear sesión
                req.session(true).attribute("currentUserUsername", username);
                req.session().attribute("userId", ac.getId());
                if (perfilId != null) req.session().attribute("profileId", perfilId);
                req.session().attribute("loggedIn", true);
                req.session().attribute("userTipo", tipo != null ? tipo : "user");

                // Redirigir al dashboard
                res.redirect("/dashboard?message=" + URLEncoder.encode(
                    "Inicio de sesión correcto.", StandardCharsets.UTF_8));
                return null;
            } else {
                res.status(401);
                model.put("errorMessage", "Usuario o contraseña incorrectos.");
                addTipo(model, tipo);
                return new ModelAndView(model, "login.mustache");
            }
        }, new MustacheTemplateEngine());

        get("/dashboard", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String currentUsername = req.session().attribute("currentUserUsername");
            String tipo = req.session().attribute("userTipo");
            model.put("username", currentUsername);
            addTipo(model, tipo);
            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine());

        get("/logout", (req, res) -> {
            req.session().invalidate();
            res.redirect("/?message=Sesion cerrada correctamente.");
            return null;
        });

        // ============================================================
        // ABM DE ALUMNOS
        // ============================================================

        get("/alumno/registrar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String err = req.queryParams("error");
            String ok  = req.queryParams("successMessage");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);
            return new ModelAndView(model, "alumnoForm.mustache");
        }, new MustacheTemplateEngine());

        post("/alumno/registrar", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String nombre   = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String correo   = req.queryParams("correo");
            String dni      = req.queryParams("dni");
            String carreraIdStr = req.queryParams("carrera_id"); // nuevo

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.redirect("/alumno/registrar?error=El nombre de usuario y la contrasena son requeridos.");
                return null;
            }
            if (nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty()) {
                res.redirect("/alumno/registrar?error=El nombre y apellido son requeridos.");
                return null;
            }
            if (correo == null || correo.isEmpty()) {
                res.redirect("/alumno/registrar?error=El correo electronico es requerido.");
                return null;
            }
            if (dni == null || !dni.matches("^[0-9]{7,8}$")) {
                res.redirect("/alumno/registrar?error=El DNI debe tener entre 7 y 8 digitos numericos.");
                return null;
            }
            if (carreraIdStr == null || carreraIdStr.isEmpty()) {
                res.redirect("/alumno/registrar?error=Debes seleccionar una carrera.");
                return null;
            }

            try {
                int carreraId = Integer.parseInt(carreraIdStr);

                Alumno a = new Alumno();
                a.setNombre(nombre);
                a.setApellido(apellido);
                a.setCorreo(correo);
                a.setDni(dni);
                a.set("carrera_id", carreraId); // guardar carrera
                a.saveIt();

                User u = new User();
                u.set("name", username);
                u.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
                u.set("perfil_id", a.getId());
                u.set("perfil_tipo", "alumno");
                u.saveIt();

                res.redirect("/?tipo=alumno&message=Alumno registrado correctamente. Ya podes iniciar sesion.");
            } catch (Exception e) {
                System.err.println("ERROR al registrar alumno: " + e.getMessage());
                res.redirect("/alumno/registrar?error=No se pudo registrar el alumno. El correo o el DNI pueden estar en uso.");
            }
            return null;
        });


        get("/alumno/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Alumno a = Alumno.findById(id);
            if (a == null) {
                res.redirect("/admin/panel?error=Alumno no encontrado.");
                return null;
            }
            Map<String, Object> model = new HashMap<>();
            model.put("esEdicion", true);
            model.put("id",        a.getId());
            model.put("nombre",    a.getNombre());
            model.put("apellido",  a.getApellido());
            model.put("correo",    a.getCorreo());
            model.put("dni",       a.getDni());
            model.put("carreraId", a.getInteger("carrera_id")); // nuevo
            return new ModelAndView(model, "alumnoForm.mustache");
        }, new MustacheTemplateEngine());

        post("/alumno/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Alumno a = Alumno.findById(id);
            if (a == null) {
                res.redirect("/admin/panel?error=Alumno no encontrado.");
                return null;
            }
            String nombre   = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String correo   = req.queryParams("correo");
            String dni      = req.queryParams("dni");
            String carreraIdStr = req.queryParams("carrera_id");

            if (nombre == null || nombre.isEmpty() || apellido == null || apellido.isEmpty()) {
                res.redirect("/alumno/editar/" + id + "?error=El nombre y apellido son requeridos.");
                return null;
            }
            if (correo == null || correo.isEmpty()) {
                res.redirect("/alumno/editar/" + id + "?error=El correo electronico es requerido.");
                return null;
            }
            if (dni == null || !dni.matches("^[0-9]{7,8}$")) {
                res.redirect("/alumno/editar/" + id + "?error=El DNI debe tener entre 7 y 8 digitos numericos.");
                return null;
            }

            try {
                a.set("nombre", nombre);
                a.set("apellido", apellido);
                a.set("correo", correo);
                a.set("dni", dni);
                if (carreraIdStr != null && !carreraIdStr.isEmpty()) {
                    a.set("carrera_id", Integer.parseInt(carreraIdStr));
                }
                a.saveIt();
                res.redirect("/admin/panel?message=Alumno actualizado correctamente.");
            } catch (Exception e) {
                System.err.println("ERROR al editar alumno: " + e.getMessage());
                res.redirect("/alumno/editar/" + id + "?error=No se pudo actualizar el alumno. El correo o el DNI pueden estar en uso.");
            }
            return null;
        });


        get("/alumno/eliminar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Alumno a = Alumno.findById(id);
            if (a != null) a.delete();

            res.redirect("/admin/panel?message=Alumno eliminado correctamente.");
            return null;
        });

        get("/admin/alumno/nuevo", (req, res) -> {
            res.redirect("/alumno/registrar");
            return null;
        });

        // ============================================================
        // PANEL ADMIN
        // ============================================================

        // ============================================================
        // ABM DE CARRERAS (admin)
        // ============================================================

        // ALTA — formulario
        get("/admin/carrera/nueva", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("error", err);
            return new ModelAndView(model, "carreraForm.mustache");
        }, new MustacheTemplateEngine());

        // ALTA — procesamiento
        post("/admin/carrera/nueva", (req, res) -> {
            String nombre = req.queryParams("nombre");
            String codigo = req.queryParams("codigo");
            if (nombre == null || nombre.trim().isEmpty()) {
                res.redirect("/admin/carrera/nueva?error=El nombre es obligatorio.");
                return null;
            }
            if (codigo == null || codigo.trim().isEmpty()) {
                res.redirect("/admin/carrera/nueva?error=El codigo es obligatorio.");
                return null;
            }
            try {
                Carrera c = new Carrera();
                c.setNombre(nombre.trim());
                c.setCodigo(codigo.trim().toUpperCase());
                c.saveIt();
                res.redirect("/admin/panel?message=Carrera creada correctamente.");
            } catch (Exception e) {
                System.err.println("ERROR al crear carrera: " + e.getMessage());
                res.redirect("/admin/carrera/nueva?error=No se pudo crear la carrera. El nombre o codigo pueden estar en uso.");
            }
            return null;
        });

        // EDICIÓN — formulario
        get("/admin/carrera/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Carrera c = Carrera.findById(id);
            if (c == null) {
                res.redirect("/admin/panel?error=Carrera no encontrada.");
                return null;
            }
            Map<String, Object> model = new HashMap<>();
            model.put("esEdicion", true);
            model.put("id",     c.getId());
            model.put("nombre", c.getNombre());
            model.put("codigo", c.getCodigo());
            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("error", err);
            return new ModelAndView(model, "carreraForm.mustache");
        }, new MustacheTemplateEngine());

        // EDICIÓN — procesamiento
        post("/admin/carrera/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Carrera c = Carrera.findById(id);
            if (c == null) {
                res.redirect("/admin/panel?error=Carrera no encontrada.");
                return null;
            }
            String nombre = req.queryParams("nombre");
            String codigo = req.queryParams("codigo");
            if (nombre == null || nombre.trim().isEmpty()) {
                res.redirect("/admin/carrera/editar/" + id + "?error=El nombre es obligatorio.");
                return null;
            }
            if (codigo == null || codigo.trim().isEmpty()) {
                res.redirect("/admin/carrera/editar/" + id + "?error=El codigo es obligatorio.");
                return null;
            }
            try {
                c.setNombre(nombre.trim());
                c.setCodigo(codigo.trim().toUpperCase());
                c.saveIt();
                res.redirect("/admin/panel?message=Carrera actualizada correctamente.");
            } catch (Exception e) {
                System.err.println("ERROR al editar carrera: " + e.getMessage());
                res.redirect("/admin/carrera/editar/" + id + "?error=No se pudo actualizar la carrera. El nombre o codigo pueden estar en uso.");
            }
            return null;
        });

        // BAJA — eliminar carrera y sus relaciones
        get("/admin/carrera/eliminar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Carrera c = Carrera.findById(id);
            if (c != null) {
                // Eliminar primero las relaciones carrera_materias
                List<CarreraMateria> relaciones = CarreraMateria.where("carrera_id = ?", id);
                for (CarreraMateria cm : relaciones) {
                    cm.delete();
                }
                c.delete();
                res.redirect("/admin/panel?message=Carrera eliminada correctamente.");
            } else {
                res.redirect("/admin/panel?error=Carrera no encontrada.");
            }
            return null;
        });

        // PLAN DE ESTUDIOS — vista de gestión (admin)
        get("/admin/carrera/:id/plan", (req, res) -> {
            int carreraId = Integer.parseInt(req.params(":id"));
            Carrera carrera = Carrera.findById(carreraId);
            if (carrera == null) {
                res.redirect("/admin/panel?error=Carrera no encontrada.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("carreraId",     carrera.getId());
            model.put("carreraNombre", carrera.getNombre());
            model.put("carreraCodigo", carrera.getCodigo());

            // Materias ya asignadas a la carrera
            List<CarreraMateria> relaciones = Carrera.getMateriasDe(carreraId);
            List<Map<String, Object>> materiasAsignadas = new ArrayList<>();
            for (CarreraMateria cm : relaciones) {
                Materia m = Materia.findById(cm.getMateriaId());
                if (m != null) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("cmId",    cm.getId());
                    row.put("id",      m.getId());
                    row.put("nombre",  m.getNombre());
                    row.put("codigo",  m.getCodigo());
                    row.put("anio",    cm.getAnio() != null ? cm.getAnio() : "");
                    materiasAsignadas.add(row);
                }
            }
            model.put("materiasAsignadas", materiasAsignadas);
            model.put("tieneMaterias", !materiasAsignadas.isEmpty());

            // IDs de materias ya asignadas para excluirlas del select
            java.util.Set<Object> asignadasIds = new java.util.HashSet<>();
            for (CarreraMateria cm : relaciones) asignadasIds.add(cm.getMateriaId());

            // Materias disponibles (no asignadas aún)
            List<Materia> todasLasMaterias = Materia.findAll();
            List<Map<String, Object>> materiasDisponibles = new ArrayList<>();
            for (Materia m : todasLasMaterias) {
                if (!asignadasIds.contains(((Number) m.getId()).intValue())) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id",     m.getId());
                    row.put("nombre", m.getNombre());
                    row.put("codigo", m.getCodigo());
                    materiasDisponibles.add(row);
                }
            }
            model.put("materiasDisponibles", materiasDisponibles);
            model.put("hayMateriasDisponibles", !materiasDisponibles.isEmpty());

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            return new ModelAndView(model, "planEstudiosAdmin.mustache");
        }, new MustacheTemplateEngine());

        // PLAN DE ESTUDIOS — agregar materia
        post("/admin/carrera/:id/plan/agregar", (req, res) -> {
            int carreraId = Integer.parseInt(req.params(":id"));
            String materiaIdStr = req.queryParams("materia_id");
            String anioStr      = req.queryParams("anio");

            if (materiaIdStr == null || materiaIdStr.isEmpty()) {
                res.redirect("/admin/carrera/" + carreraId + "/plan?error=Debe seleccionar una materia.");
                return null;
            }

            int materiaId = Integer.parseInt(materiaIdStr);
            Materia mat = Materia.findById(materiaId);
            if (mat == null) {
                res.redirect("/admin/carrera/" + carreraId + "/plan?error=La materia seleccionada no existe.");
                return null;
            }

            // Verificar que no esté ya asignada
            List<CarreraMateria> existe = CarreraMateria.where("carrera_id = ? AND materia_id = ?", carreraId, materiaId);
            if (!existe.isEmpty()) {
                res.redirect("/admin/carrera/" + carreraId + "/plan?error=La materia ya está asignada a esta carrera.");
                return null;
            }

            try {
                CarreraMateria cm = new CarreraMateria();
                cm.setCarreraId(carreraId);
                cm.setMateriaId(materiaId);
                if (anioStr != null && !anioStr.trim().isEmpty()) {
                    try { cm.setAnio(Integer.parseInt(anioStr.trim())); }
                    catch (NumberFormatException ignored) {}
                }
                cm.saveIt();
                res.redirect("/admin/carrera/" + carreraId + "/plan?message=Materia agregada al plan de estudios correctamente.");
            } catch (Exception e) {
                System.err.println("ERROR al agregar materia al plan: " + e.getMessage());
                res.redirect("/admin/carrera/" + carreraId + "/plan?error=No se pudo agregar la materia al plan.");
            }
            return null;
        });

        // PLAN DE ESTUDIOS — quitar materia
        get("/admin/carrera/:id/plan/quitar/:cmid", (req, res) -> {
            int carreraId = Integer.parseInt(req.params(":id"));
            int cmId      = Integer.parseInt(req.params(":cmid"));
            CarreraMateria cm = CarreraMateria.findById(cmId);
            if (cm != null && cm.getCarreraId().equals(carreraId)) {
                cm.delete();
                res.redirect("/admin/carrera/" + carreraId + "/plan?message=Materia quitada del plan de estudios correctamente.");
            } else {
                res.redirect("/admin/carrera/" + carreraId + "/plan?error=No se encontró la relacion a eliminar.");
            }
            return null;
        });

        get("/admin/panel", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("alumnos",   Alumno.findAll());
            model.put("profesores", Profesor.findAll());

            // Carreras para la sección de gestión
            List<Carrera> carrerasRaw = Carrera.findAll();
            List<Map<String, Object>> carreras = new ArrayList<>();
            for (Carrera c : carrerasRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",     c.getId());
                row.put("nombre", c.getNombre());
                row.put("codigo", c.getCodigo());
                carreras.add(row);
            }
            model.put("carreras", carreras);

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);
            return new ModelAndView(model, "adminPanel.mustache");
        }, new MustacheTemplateEngine());

        // ============================================================
        // ABM DE PROFESORES
        // ============================================================

        get("/profesor/registrar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String err = req.queryParams("error");
            String ok  = req.queryParams("successMessage");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);
            return new ModelAndView(model, "profesorForm.mustache");
        }, new MustacheTemplateEngine());

        post("/profesor/registrar", (req, res) -> {
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String nombre   = req.queryParams("nombre");
            String apellido = req.queryParams("apellido");
            String correo   = req.queryParams("correo");
            String dni      = req.queryParams("dni");
            String carreraIdStr = req.queryParams("carrera_id");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.redirect("/profesor/registrar?error=El nombre de usuario y la contrasena son requeridos.");
                return null;
            }

            try {
                int carreraId = Integer.parseInt(carreraIdStr);

                Profesor p = new Profesor();
                p.setNombre(nombre);
                p.setApellido(apellido);
                p.setCorreo(correo);
                p.setDni(dni);
                p.set("carrera_id", carreraId);
                p.saveIt();

                User u = new User();
                u.set("name", username);
                u.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
                u.set("perfil_id", p.getId());
                u.set("perfil_tipo", "profesor");
                u.saveIt();

                res.redirect("/?tipo=profesor&message=Profesor registrado correctamente. Ya podes iniciar sesion.");
            } catch (Exception e) {
                System.err.println("ERROR al registrar profesor: " + e.getMessage());
                res.redirect("/profesor/registrar?error=No se pudo registrar el profesor. El correo o el DNI pueden estar en uso.");
            }
            return null;
        });


        post("/profesor/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Profesor p = Profesor.findById(id);
            if (p != null) {
                p.set("nombre",   req.queryParams("nombre"));
                p.set("apellido", req.queryParams("apellido"));
                p.set("correo",   req.queryParams("correo"));
                p.set("dni",      req.queryParams("dni"));
                p.saveIt();
            }
            res.redirect("/admin/panel");
            return null;
        });

        get("/profesor/eliminar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Profesor p = Profesor.findById(id);
            if (p != null) p.delete();
            res.redirect("/admin/panel");
            return null;
        });

        get("/profesores/listar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("profesores", Profesor.findAll());
            return new ModelAndView(model, "profesoresList.mustache");
        }, new MustacheTemplateEngine());

        // ============================================================
        // USUARIOS GENÉRICO (legacy)
        // ============================================================

        get("/user/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String ok  = req.queryParams("message");
            String err = req.queryParams("error");
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);
            if (err != null && !err.isEmpty()) model.put("errorMessage", err);
            return new ModelAndView(model, "user_form.mustache");
        }, new MustacheTemplateEngine());

        get("/user/new", (req, res) -> new ModelAndView(new HashMap<>(), "user_form.mustache"),
                new MustacheTemplateEngine());

        post("/user/new", (req, res) -> {
            String name = req.queryParams("name");
            String pw   = req.queryParams("password");
            if (name == null || name.isEmpty() || pw == null || pw.isEmpty()) {
                res.redirect("/user/create?error=Nombre_y_contrasena_son_requeridos");
                return "";
            }
            try {
                User u = new User();
                u.set("name", name);
                u.set("password", BCrypt.hashpw(pw, BCrypt.gensalt()));
                u.saveIt();
                res.redirect("/user/create?message=Usuario_creado_exitosamente_para_" + name);
            } catch (Exception e) {
                res.redirect("/user/create?error=Error_interno_al_crear_la_cuenta");
            }
            return "";
        });

        // ============================================================
        // ABM DE MATERIAS
        // ============================================================

                
        get("/materia/registrar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);
            return new ModelAndView(model, "materiaForm.mustache");
        }, new MustacheTemplateEngine());

        post("/materia/registrar", (req, res) -> {
            String nombre      = req.queryParams("nombre");
            String codigo      = req.queryParams("codigo");
            String descripcion = req.queryParams("descripcion");

            if (nombre == null || nombre.trim().isEmpty()) {
                res.redirect("/materia/registrar?error=El nombre es obligatorio.");
                return null;
            }
            if (codigo == null || codigo.trim().isEmpty()) {
                res.redirect("/materia/registrar?error=El codigo es obligatorio.");
                return null;
            }

            try {
                Materia m = new Materia();
                m.set("nombre",      nombre.trim());
                m.set("codigo",      codigo.trim());
                m.set("descripcion", (descripcion != null && !descripcion.trim().isEmpty()) ? descripcion.trim() : null);
                m.saveIt();
                res.redirect("/materias/listar?message=Materia registrada correctamente.");
            } catch (Exception e) {
                System.err.println("ERROR al registrar materia: " + e.getMessage());
                res.redirect("/materia/registrar?error=No se pudo registrar la materia. El nombre o codigo pueden estar en uso.");
            }
            return null;
        });

        get("/materias/listar", (req, res) -> {
            Integer profileId = getSessionProfileId(req);
            if (profileId == null) {
                res.redirect("/dashboard?error=Solo los profesores pueden acceder a esta sección.");
                return null;
            }

            Profesor profesor = Profesor.findById(profileId);
            if (profesor == null) {
                res.redirect("/dashboard?error=Profesor no encontrado.");
                return null;
            }

            // Obtener la carrera del profesor
            Integer carreraId = profesor.getInteger("carrera_id");

            // Traer solo las materias de esa carrera
            List<Materia> materias = Materia.find(
                "id IN (SELECT materia_id FROM carrera_materias WHERE carrera_id = ?)", carreraId
            );

            Map<String, Object> model = new HashMap<>();
            model.put("materias", materias);
            model.put("profesor", profesor);

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            return new ModelAndView(model, "materiasList.mustache");
        }, new MustacheTemplateEngine());


        get("/materia/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Materia materia = Materia.findById(id);
            if (materia == null) {
                res.redirect("/materias/listar?error=Materia no encontrada.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("esEdicion",   true);
            model.put("id",          materia.getId());
            model.put("nombre",      materia.getNombre());
            model.put("codigo",      materia.getCodigo());
            model.put("descripcion", materia.getDescripcion());

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            return new ModelAndView(model, "materiaForm.mustache");
        }, new MustacheTemplateEngine());



        post("/materia/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Materia m = Materia.findById(id);
            if (m != null) {
                m.set("nombre",      req.queryParams("nombre"));
                m.set("codigo",      req.queryParams("codigo"));
                m.set("descripcion", req.queryParams("descripcion"));
                m.saveIt();
            }
            res.redirect("/materias/listar");
            return null;
        });

        get("/materia/eliminar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Materia m = Materia.findById(id);
            if (m != null) m.delete();
            res.redirect("/materias/listar");
            return null;
        });

        // ============================================================
        // ABM DE EXÁMENES FINALES
        // ============================================================

        // LISTADO
        get("/examenes/listar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            // Enriquecer cada examen con el nombre de la materia para mostrar en la vista
            List<ExamenFinal> examenesRaw = ExamenFinal.findAll();
            List<Map<String, Object>> examenes = new ArrayList<>();
            for (ExamenFinal e : examenesRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",   e.getId());
                row.put("fecha", e.getFecha());
                row.put("hora",  e.getHora());
                row.put("turno", e.getTurno());
                row.put("observaciones", e.getObservaciones());

                // Resolver nombre de materia
                Materia mat = Materia.findById(e.getMateriaId());
                row.put("materiaNombre", mat != null ? mat.getNombre() : "(materia eliminada)");

                examenes.add(row);
            }
            model.put("examenes", examenes);

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            // TODO: Cuando se implemente el calendario de exámenes, agregar aquí
            //       la lógica de filtrado por período/turno para alimentar esa vista.

            return new ModelAndView(model, "examenFinalList.mustache");
        }, new MustacheTemplateEngine());

        // ALTA — formulario
        get("/examen/registrar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("materias", Materia.findAll());
            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("error", err);
            return new ModelAndView(model, "examenFinalForm.mustache");
        }, new MustacheTemplateEngine());

        // ALTA — procesamiento
        post("/examen/registrar", (req, res) -> {
            String materiaIdStr = req.queryParams("materia_id");
            String fecha        = req.queryParams("fecha");
            String hora         = req.queryParams("hora");
            String turno        = req.queryParams("turno");
            String observaciones = req.queryParams("observaciones");

            if (materiaIdStr == null || materiaIdStr.isEmpty()) {
                res.redirect("/examen/registrar?error=Debe seleccionar una materia.");
                return null;
            }
            if (fecha == null || fecha.isEmpty()) {
                res.redirect("/examen/registrar?error=La fecha es requerida.");
                return null;
            }
            // Verificar que la materia exista
            Materia mat = Materia.findById(Integer.parseInt(materiaIdStr));
            if (mat == null) {
                res.redirect("/examen/registrar?error=La materia seleccionada no existe.");
                return null;
            }

            try {
                ExamenFinal e = new ExamenFinal();
                e.setMateriaId(Integer.parseInt(materiaIdStr));
                e.setFecha(fecha);
                e.setHora((hora != null && !hora.isEmpty()) ? hora : null);
                e.setTurno((turno != null && !turno.isEmpty()) ? turno : null);
                e.setObservaciones((observaciones != null && !observaciones.isEmpty()) ? observaciones : null);
                e.saveIt();

                // TODO: Cuando se implemente InscripcionExamen, notificar o habilitar
                //       inscripciones a partir de esta fecha.
                res.redirect("/examenes/listar?message=Examen registrado correctamente.");
            } catch (Exception ex) {
                System.err.println("ERROR al registrar examen: " + ex.getMessage());
                res.redirect("/examen/registrar?error=No se pudo registrar el examen.");
            }
            return null;
        });

        // MODIFICACIÓN — formulario con datos pre-cargados
        get("/examen/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            ExamenFinal e = ExamenFinal.findById(id);
            if (e == null) {
                res.redirect("/examenes/listar?error=Examen no encontrado.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("esEdicion", true);
            model.put("id",           e.getId());
            model.put("fecha",        e.getFecha());
            model.put("hora",         e.getHora());
            model.put("observaciones", e.getObservaciones());

            // Marcar el turno seleccionado para el <select> en Mustache
            String turno = e.getTurno();
            if ("Febrero".equals(turno))    model.put("turnoFebrero",   true);
            if ("Julio".equals(turno))      model.put("turnoJulio",     true);
            if ("Diciembre".equals(turno))  model.put("turnoDiciembre", true);

            // Construir lista de materias marcando la seleccionada
            List<Materia> materiasRaw = Materia.findAll();
            List<Map<String, Object>> materias = new ArrayList<>();
            for (Materia m : materiasRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",     m.getId());
                row.put("nombre", m.getNombre());
                row.put("codigo", m.getCodigo());
                if (m.getId().equals(e.getMateriaId())) row.put("seleccionada", true);
                materias.add(row);
            }
            model.put("materias", materias);

            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("error", err);

            return new ModelAndView(model, "examenFinalForm.mustache");
        }, new MustacheTemplateEngine());

        // MODIFICACIÓN — procesamiento
        post("/examen/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            ExamenFinal e = ExamenFinal.findById(id);
            if (e == null) {
                res.redirect("/examenes/listar?error=Examen no encontrado.");
                return null;
            }

            String materiaIdStr  = req.queryParams("materia_id");
            String fecha         = req.queryParams("fecha");
            String hora          = req.queryParams("hora");
            String turno         = req.queryParams("turno");
            String observaciones = req.queryParams("observaciones");

            if (materiaIdStr == null || materiaIdStr.isEmpty()) {
                res.redirect("/examen/editar/" + id + "?error=Debe seleccionar una materia.");
                return null;
            }
            if (fecha == null || fecha.isEmpty()) {
                res.redirect("/examen/editar/" + id + "?error=La fecha es requerida.");
                return null;
            }
            Materia mat = Materia.findById(Integer.parseInt(materiaIdStr));
            if (mat == null) {
                res.redirect("/examen/editar/" + id + "?error=La materia seleccionada no existe.");
                return null;
            }

            try {
                e.setMateriaId(Integer.parseInt(materiaIdStr));
                e.setFecha(fecha);
                e.setHora((hora != null && !hora.isEmpty()) ? hora : null);
                e.setTurno((turno != null && !turno.isEmpty()) ? turno : null);
                e.setObservaciones((observaciones != null && !observaciones.isEmpty()) ? observaciones : null);
                e.saveIt();

                // TODO: Cuando se implemente InscripcionExamen, verificar si existen
                //       inscripciones activas antes de permitir modificar la fecha.
                res.redirect("/examenes/listar?message=Examen actualizado correctamente.");
            } catch (Exception ex) {
                System.err.println("ERROR al editar examen: " + ex.getMessage());
                res.redirect("/examen/editar/" + id + "?error=No se pudo actualizar el examen.");
            }
            return null;
        });

        // BAJA — eliminación física (mismo patrón que materia/eliminar)
        get("/examen/eliminar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            ExamenFinal e = ExamenFinal.findById(id);
            if (e != null) {
                // TODO: Cuando se implemente InscripcionExamen, verificar aquí
                //       si existen inscripciones asociadas antes de eliminar.
                e.delete();
            }
            res.redirect("/examenes/listar?message=Examen eliminado correctamente.");
            return null;
        });

        // ============================================================
        // CALENDARIO DE EXÁMENES (solo lectura)
        // ============================================================

        get("/examenes/calendario", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            List<ExamenFinal> examenesRaw = ExamenFinal.where("1=1 ORDER BY fecha ASC, hora ASC");
            List<Map<String, Object>> examenes = new ArrayList<>();

            for (ExamenFinal e : examenesRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",            e.getId());
                row.put("fecha",         e.getFecha());
                row.put("hora",          e.getHora());
                row.put("turno",         e.getTurno());
                row.put("observaciones", e.getObservaciones());

                Materia mat = Materia.findById(e.getMateriaId());
                row.put("materiaNombre", mat != null ? mat.getNombre() : "(materia eliminada)");

                examenes.add(row);
            }

            model.put("examenes", examenes);
            return new ModelAndView(model, "calendarioExamenes.mustache");
        }, new MustacheTemplateEngine());

        // ============================================================
        // MENSAJERÍA INTERNA
        // ============================================================

        get("/mensaje/nuevo", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String username = req.session().attribute("currentUserUsername");
            String userTipo = req.session().attribute("userTipo");
            model.put("remitenteNombre", username);

            List<Map<String, Object>> destinatarios = new ArrayList<>();

            boolean esAlumno   = "alumno".equals(userTipo);
            boolean esProfesor = "profesor".equals(userTipo);

            if (esAlumno || (!esProfesor)) {
                List<Profesor> profesores = Profesor.findAll();
                for (Profesor p : profesores) {
                    Map<String, Object> dest = new HashMap<>();
                    dest.put("nombreCompleto", p.getNombre() + " " + p.getApellido());
                    dest.put("tipo",          "Profesor");
                    dest.put("userId", "P:" + p.getId());
                    destinatarios.add(dest);
                }
            }

            if (esProfesor || (!esAlumno)) {
                List<Alumno> alumnos = Alumno.findAll();
                for (Alumno a : alumnos) {
                    Map<String, Object> dest = new HashMap<>();
                    dest.put("userId",        "A:" + a.getId());
                    dest.put("nombreCompleto", a.getNombre() + " " + a.getApellido());
                    dest.put("tipo",          "Alumno");
                    destinatarios.add(dest);
                }
            }

            model.put("destinatarios", destinatarios);

            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("error", err);

            return new ModelAndView(model, "mensajeForm.mustache");
        }, new MustacheTemplateEngine());

        post("/mensaje/enviar", (req, res) -> {
            String destinatarioParam = req.queryParams("destinatario_id");
            String asunto            = req.queryParams("asunto");
            String contenido         = req.queryParams("contenido");
            Object sessionUserId     = req.session().attribute("userId");

            if (destinatarioParam == null || destinatarioParam.isEmpty()) {
                res.redirect("/mensaje/nuevo?error=Debe seleccionar un destinatario.");
                return null;
            }
            if (asunto == null || asunto.trim().isEmpty()) {
                res.redirect("/mensaje/nuevo?error=El asunto es obligatorio.");
                return null;
            }
            if (contenido == null || contenido.trim().isEmpty()) {
                res.redirect("/mensaje/nuevo?error=El contenido del mensaje es obligatorio.");
                return null;
            }

            int destinatarioUserId;
            try {
                String[] partes = destinatarioParam.split(":");
                String tipoDest = partes[0];
                int perfilId    = Integer.parseInt(partes[1]);

                if ("P".equals(tipoDest)) {
                    Profesor p = Profesor.findById(perfilId);
                    if (p == null) {
                        res.redirect("/mensaje/nuevo?error=El destinatario seleccionado no existe.");
                        return null;
                    }
                    destinatarioUserId = perfilId;
                } else if ("A".equals(tipoDest)) {
                    Alumno a = Alumno.findById(perfilId);
                    if (a == null) {
                        res.redirect("/mensaje/nuevo?error=El destinatario seleccionado no existe.");
                        return null;
                    }
                    destinatarioUserId = perfilId;
                } else {
                    res.redirect("/mensaje/nuevo?error=Destinatario invalido.");
                    return null;
                }
            } catch (Exception ex) {
                res.redirect("/mensaje/nuevo?error=Destinatario invalido.");
                return null;
            }

            int remitenteId = ((Number) sessionUserId).intValue();

            if (remitenteId == destinatarioUserId) {
                res.redirect("/mensaje/nuevo?error=No puedes enviarte un mensaje a ti mismo.");
                return null;
            }

            try {
                String ahora = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date());

                Mensaje m = new Mensaje();
                m.setRemitenteId(remitenteId);
                m.setDestinatarioId(destinatarioUserId);
                m.setAsunto(asunto.trim());
                m.setContenido(contenido.trim());
                m.setFechaEnvio(ahora);
                m.saveIt();

                res.redirect("/dashboard?message=Mensaje enviado correctamente.");
            } catch (Exception ex) {
                System.err.println("ERROR al enviar mensaje: " + ex.getMessage());
                res.redirect("/mensaje/nuevo?error=No se pudo enviar el mensaje. Intentá nuevamente.");
            }
            return null;
        });

        // ============================================================
        // BANDEJA DE ENTRADA
        // ============================================================

        get("/mensajes/bandeja", (req, res) -> {
            Object sessionUserId = req.session().attribute("userId");
            int userId = ((Number) sessionUserId).intValue();

            Map<String, Object> model = new HashMap<>();

            List<Mensaje> mensajesRaw = Mensaje.where(
                    "destinatario_id = ? ORDER BY fecha_envio DESC", userId);

            List<Map<String, Object>> mensajes = new ArrayList<>();
            for (Mensaje m : mensajesRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("asunto",    m.getAsunto());
                row.put("contenido", m.getContenido());
                row.put("fechaEnvio", m.getFechaEnvio());

                User remitente = User.findById(m.getRemitenteId());
                row.put("remitenteNombre",
                        remitente != null ? remitente.getName() : "(usuario eliminado)");

                mensajes.add(row);
            }

            model.put("mensajes", mensajes);
            return new ModelAndView(model, "bandejaMensajes.mustache");
        }, new MustacheTemplateEngine());

        // ============================================================
        // ABM DE PERÍODOS DE INSCRIPCIÓN
        // ============================================================

        // LISTADO
        get("/periodos/listar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            List<PeriodoInscripcion> periodosRaw = PeriodoInscripcion.findAll();
            List<Map<String, Object>> periodos = new ArrayList<>();
            for (PeriodoInscripcion p : periodosRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",          p.getId());
                row.put("nombre",      p.getNombre());
                row.put("tipo",        p.getTipo());
                row.put("fechaInicio", p.getFechaInicio());
                row.put("fechaFin",    p.getFechaFin());
                row.put("estado",      p.getEstado());
                periodos.add(row);
            }
            model.put("periodos", periodos);

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            return new ModelAndView(model, "periodosList.mustache");
        }, new MustacheTemplateEngine());

        // ALTA — formulario
        get("/periodo/registrar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("error", err);
            return new ModelAndView(model, "periodoForm.mustache");
        }, new MustacheTemplateEngine());

        // ALTA — procesamiento
        post("/periodo/registrar", (req, res) -> {
            String nombre      = req.queryParams("nombre");
            String tipo        = req.queryParams("tipo");
            String fechaInicio = req.queryParams("fecha_inicio");
            String fechaFin    = req.queryParams("fecha_fin");

            String validacion = validarCamposPeriodo(nombre, tipo, fechaInicio, fechaFin);
            if (validacion != null) {
                res.redirect("/periodo/registrar?error=" + validacion);
                return null;
            }

            try {
                PeriodoInscripcion p = new PeriodoInscripcion();
                p.setNombre(nombre.trim());
                p.setTipo(tipo);
                p.setFechaInicio(fechaInicio);
                p.setFechaFin(fechaFin);
                p.saveIt();

                res.redirect("/periodos/listar?message=Periodo registrado correctamente.");
            } catch (Exception ex) {
                System.err.println("ERROR al registrar periodo: " + ex.getMessage());
                res.redirect("/periodo/registrar?error=No se pudo registrar el periodo.");
            }
            return null;
        });

        // EDICIÓN — formulario con datos pre-cargados
        get("/periodo/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            PeriodoInscripcion p = PeriodoInscripcion.findById(id);
            if (p == null) {
                res.redirect("/periodos/listar?error=Periodo no encontrado.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("esEdicion",   true);
            model.put("id",          p.getId());
            model.put("nombre",      p.getNombre());
            model.put("fechaInicio", p.getFechaInicio());
            model.put("fechaFin",    p.getFechaFin());

            // Marcar el tipo seleccionado para el <select> en Mustache
            if ("MATERIAS".equals(p.getTipo()))  model.put("tipoMaterias",  true);
            if ("EXAMENES".equals(p.getTipo()))  model.put("tipoExamenes",  true);

            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("error", err);

            return new ModelAndView(model, "periodoForm.mustache");
        }, new MustacheTemplateEngine());

        // EDICIÓN — procesamiento
        post("/periodo/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            PeriodoInscripcion p = PeriodoInscripcion.findById(id);
            if (p == null) {
                res.redirect("/periodos/listar?error=Periodo no encontrado.");
                return null;
            }

            String nombre      = req.queryParams("nombre");
            String tipo        = req.queryParams("tipo");
            String fechaInicio = req.queryParams("fecha_inicio");
            String fechaFin    = req.queryParams("fecha_fin");

            String validacion = validarCamposPeriodo(nombre, tipo, fechaInicio, fechaFin);
            if (validacion != null) {
                res.redirect("/periodo/editar/" + id + "?error=" + validacion);
                return null;
            }

            try {
                p.setNombre(nombre.trim());
                p.setTipo(tipo);
                p.setFechaInicio(fechaInicio);
                p.setFechaFin(fechaFin);
                p.saveIt();

                res.redirect("/periodos/listar?message=Periodo actualizado correctamente.");
            } catch (Exception ex) {
                System.err.println("ERROR al editar periodo: " + ex.getMessage());
                res.redirect("/periodo/editar/" + id + "?error=No se pudo actualizar el periodo.");
            }
            return null;
        });

        // BAJA
        get("/periodo/eliminar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            PeriodoInscripcion p = PeriodoInscripcion.findById(id);
            if (p != null) {
                // TODO: Cuando se implemente InscripcionMateria / InscripcionExamen,
                //       verificar si existen inscripciones activas antes de eliminar.
                p.delete();
            }
            res.redirect("/periodos/listar?message=Periodo eliminado correctamente.");
            return null;
        });

        // ============================================================
        // NOTAS DE ALUMNOS POR MATERIA (carga por profesor)
        // ============================================================

        // LISTADO — muestra todos los alumnos inscriptos en la materia con su nota actual.
        // Si un alumno no tiene fila en alumnos_materias aún, se le crea una al guardar.
        // GET /materia/:id/notas
        get("/materia/:id/notas", (req, res) -> {
            int materiaId = Integer.parseInt(req.params(":id"));
            Materia materia = Materia.findById(materiaId);
            if (materia == null) {
                res.redirect("/materias/listar?error=Materia no encontrada.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("materiaId", materia.getId());
            model.put("materiaNombre", materia.getNombre());
            model.put("materiaCodigo", materia.getCodigo());

            // Traer solo los alumnos inscriptos en esta materia
            List<AlumnoMateria> inscripciones = AlumnoMateria.where("materia_id = ?", materiaId);
            List<Map<String, Object>> filas = new ArrayList<>();

            for (AlumnoMateria am : inscripciones) {
                Alumno a = Alumno.findById(am.getAlumnoId());
                if (a != null) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("alumnoId", a.getId());
                    fila.put("alumnoNombre", a.getNombre());
                    fila.put("alumnoApellido", a.getApellido());
                    fila.put("nota", am.getNota() != null ? am.getNota().toString() : "");
                    filas.add(fila);
                }
            }
            model.put("alumnos", filas);

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            return new ModelAndView(model, "notasForm.mustache");
        }, new MustacheTemplateEngine());


        // GUARDAR NOTAS — procesa el formulario de carga de notas.
        // POST /materia/:id/notas
        // El formulario envía un campo nota_{alumnoId} por cada fila visible.
        // Si la fila ya existe en alumnos_materias se actualiza; si no, se crea.
        // Campos vacíos se guardan como null (nota no cargada).
        post("/materia/:id/notas", (req, res) -> {
            int materiaId = Integer.parseInt(req.params(":id"));
            Materia materia = Materia.findById(materiaId);
            if (materia == null) {
                res.redirect("/materias/listar?error=Materia no encontrada.");
                return null;
            }

            List<AlumnoMateria> inscripciones = AlumnoMateria.where("materia_id = ?", materiaId);
            List<String> errores = new ArrayList<>();

            for (AlumnoMateria am : inscripciones) {
                int alumnoId = am.getAlumnoId();
                String campo = req.queryParams("nota_" + alumnoId);

                if (campo == null || campo.trim().isEmpty()) {
                    am.setNota(null);
                    am.saveIt();
                    continue;
                }

                double nota;
                try {
                    nota = Double.parseDouble(campo.trim().replace(",", "."));
                } catch (NumberFormatException ex) {
                    errores.add("Alumno ID " + alumnoId + ": la nota debe ser un número.");
                    continue;
                }
                if (nota < 0 || nota > 10) {
                    errores.add("Alumno ID " + alumnoId + ": la nota debe estar entre 0 y 10.");
                    continue;
                }

                am.setNota(nota);
                am.saveIt();
            }

            if (!errores.isEmpty()) {
                String mensajeError = String.join(" | ", errores);
                res.redirect("/materia/" + materiaId + "/notas?error="
                        + java.net.URLEncoder.encode(mensajeError, "UTF-8"));
            } else {
                res.redirect("/materia/" + materiaId + "/notas?message="
                        + java.net.URLEncoder.encode("Notas guardadas correctamente.", "UTF-8"));
            }
            return null;
        });

        // ============================================================
        // ISSUE #5 — INSCRIPCION A MATERIAS (alumno)
        // ============================================================

        // MATERIAS DISPONIBLES — lista todas las materias indicando si el alumno ya esta inscripto.
        // Solo se muestra el boton "Inscribirse" cuando hay un periodo ACTIVO para MATERIAS.
        // GET /materias/disponibles
        get("/materias/disponibles", (req, res) -> {
            Integer profileId = getSessionProfileId(req);
            if (profileId == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden inscribirse a materias.");
                return null;
            }

            Alumno alumno = Alumno.findById(profileId);
            if (alumno == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden inscribirse a materias.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();

            // Verificar periodo activo
            PeriodoInscripcion periodo = PeriodoInscripcion.getPeriodoActivoMaterias();
            boolean periodoActivo = (periodo != null);
            model.put("periodoActivo", periodoActivo);
            if (periodoActivo) {
                model.put("periodoNombre", periodo.getNombre());
                model.put("periodoFin",    periodo.getFechaFin());
            }

            // Obtener la carrera del alumno
            Integer carreraId = alumno.getInteger("carrera_id");

            // Traer solo las materias de esa carrera
            List<Materia> materiasDeCarrera = Materia.find(
                "id IN (SELECT materia_id FROM carrera_materias WHERE carrera_id = ?)", carreraId
            );

            List<Map<String, Object>> materias = new ArrayList<>();
            for (Materia m : materiasDeCarrera) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",          m.getId());
                row.put("nombre",      m.getNombre());
                row.put("codigo",      m.getCodigo());
                row.put("descripcion", m.getDescripcion());

                AlumnoMateria am = AlumnoMateria.findByAlumnoIdAndMateriaId(profileId,
                        ((Number) m.getId()).intValue());
                row.put("inscripto", am != null);
                row.put("fechaInscripcion", am != null ? am.getFechaInscripcion() : null);
                materias.add(row);
            }
            model.put("materias", materias);

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            return new ModelAndView(model, "materiasDisponibles.mustache");
        }, new MustacheTemplateEngine());

        // INSCRIBIRSE A UNA MATERIA
        // POST /materia/inscribirse/:id
        post("/materia/inscribirse/:id", (req, res) -> {
            Integer profileId = getSessionProfileId(req);
            if (profileId == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden inscribirse a materias.");
                return null;
            }

            Alumno alumno = Alumno.findById(profileId);
            if (alumno == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden inscribirse a materias.");
                return null;
            }

            // Validar periodo activo
            PeriodoInscripcion periodo = PeriodoInscripcion.getPeriodoActivoMaterias();
            if (periodo == null) {
                res.redirect("/materias/disponibles?error="
                        + java.net.URLEncoder.encode(
                                "No hay un periodo de inscripcion activo. La inscripcion esta cerrada.",
                                "UTF-8"));
                return null;
            }

            int materiaId = Integer.parseInt(req.params(":id"));
            Materia materia = Materia.findById(materiaId);
            if (materia == null) {
                res.redirect("/materias/disponibles?error=La materia seleccionada no existe.");
                return null;
            }

            // Validar doble inscripcion
            AlumnoMateria existente = AlumnoMateria.findByAlumnoIdAndMateriaId(profileId, materiaId);
            if (existente != null) {
                res.redirect("/materias/disponibles?error="
                        + java.net.URLEncoder.encode(
                                "Ya estas inscripto en " + materia.getNombre() + ".",
                                "UTF-8"));
                return null;
            }

            try {
                String hoy = java.time.LocalDate.now().toString();
                AlumnoMateria am = new AlumnoMateria();
                am.setAlumnoId(profileId);
                am.setMateriaId(materiaId);
                am.setFechaInscripcion(hoy);
                am.saveIt();

                res.redirect("/materias/disponibles?message="
                        + java.net.URLEncoder.encode(
                                "Te inscribiste correctamente en " + materia.getNombre() + ".",
                                "UTF-8"));
            } catch (Exception ex) {
                System.err.println("ERROR al inscribir alumno " + profileId
                        + " en materia " + materiaId + ": " + ex.getMessage());
                res.redirect("/materias/disponibles?error=No se pudo completar la inscripcion. Intentá nuevamente.");
            }
            return null;
        });

        // MIS MATERIAS — lista las materias en que el alumno esta inscripto.
        // GET /mis-materias
        get("/mis-materias", (req, res) -> {
            Integer profileId = getSessionProfileId(req);
            if (profileId == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden acceder a esta seccion.");
                return null;
            }

            Alumno alumno = Alumno.findById(profileId);
            if (alumno == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden acceder a esta seccion.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("alumnoNombre",   alumno.getNombre());
            model.put("alumnoApellido", alumno.getApellido());

            List<AlumnoMateria> inscripciones = AlumnoMateria.findByAlumnoId(profileId);
            List<Map<String, Object>> materias = new ArrayList<>();
            for (AlumnoMateria am : inscripciones) {
                Map<String, Object> row = new HashMap<>();
                row.put("fechaInscripcion", am.getFechaInscripcion());
                row.put("nota", am.getNota() != null ? am.getNota().toString() : "Sin nota");

                Materia m = Materia.findById(am.getMateriaId());
                row.put("materiaNombre", m != null ? m.getNombre() : "(materia eliminada)");
                row.put("materiaCodigo", m != null ? m.getCodigo() : "");
                materias.add(row);
            }
            model.put("materias", materias);
            model.put("tieneInscripciones", !materias.isEmpty());

            return new ModelAndView(model, "misMaterias.mustache");
        }, new MustacheTemplateEngine());

        // ============================================================
        // ISSUE #4 — PLAN DE ESTUDIOS POR CARRERA
        // ============================================================

        // LISTADO DE CARRERAS
        // GET /carreras
        get("/carreras", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            List<Carrera> carrerasRaw = Carrera.findAll();
            List<Map<String, Object>> carreras = new ArrayList<>();
            for (Carrera c : carrerasRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",     c.getId());
                row.put("nombre", c.getNombre());
                row.put("codigo", c.getCodigo());
                carreras.add(row);
            }
            model.put("carreras", carreras);

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            return new ModelAndView(model, "carrerasList.mustache");
        }, new MustacheTemplateEngine());

        // PLAN DE ESTUDIOS DE UNA CARRERA — agrupa materias por año
        // GET /carrera/:id/plan
        get("/carrera/:id/plan", (req, res) -> {
            int carreraId = Integer.parseInt(req.params(":id"));
            Carrera carrera = Carrera.findById(carreraId);
            if (carrera == null) {
                res.redirect("/carreras?error=Carrera no encontrada.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("carreraId",     carrera.getId());
            model.put("carreraNombre", carrera.getNombre());
            model.put("carreraCodigo", carrera.getCodigo());

            // Obtener relaciones carrera-materia ordenadas por anio y orden
            List<CarreraMateria> relaciones = Carrera.getMateriasDe(carreraId);

            // Agrupar por año usando una lista de mapas compatibles con Mustache.
            // Estructura: [{anio: "Primer Año", materias: [{nombre, codigo, descripcion}, ...]}, ...]
            // Materias sin año asignado van al grupo "Sin año asignado".
            java.util.LinkedHashMap<String, List<Map<String, Object>>> gruposPorAnio =
                    new java.util.LinkedHashMap<>();

            for (CarreraMateria cm : relaciones) {
                Integer anio = cm.getAnio();
                String clave = (anio != null) ? anioEnLetras(anio) : "Sin año asignado";

                gruposPorAnio.putIfAbsent(clave, new ArrayList<>());

                Materia m = Materia.findById(cm.getMateriaId());
                if (m != null) {
                    Map<String, Object> mat = new HashMap<>();
                    mat.put("nombre",      m.getNombre());
                    mat.put("codigo",      m.getCodigo());
                    mat.put("descripcion", m.getDescripcion());
                    gruposPorAnio.get(clave).add(mat);
                }
            }

            // Convertir a lista de mapas para Mustache (que no itera Map directamente)
            List<Map<String, Object>> anios = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : gruposPorAnio.entrySet()) {
                Map<String, Object> grupo = new HashMap<>();
                grupo.put("anioNombre", entry.getKey());
                grupo.put("materias",   entry.getValue());
                anios.add(grupo);
            }
            model.put("anios", anios);
            model.put("tieneAnios", !anios.isEmpty());

            return new ModelAndView(model, "planEstudios.mustache");
        }, new MustacheTemplateEngine());

        // ============================================================
        // ISSUE #6 — INSCRIPCION A EXAMENES FINALES (alumno)
        // ============================================================

        // EXAMENES DISPONIBLES — muestra todos los examenes indicando si el alumno ya esta inscripto.
        // Solo habilita la inscripcion cuando hay un periodo ACTIVO para EXAMENES.
        // GET /examenes/disponibles
        get("/examenes/disponibles", (req, res) -> {
            Integer profileId = getSessionProfileId(req);
            if (profileId == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden inscribirse a examenes.");
                return null;
            }

            Alumno alumno = Alumno.findById(profileId);
            if (alumno == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden inscribirse a examenes.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();

            // Verificar periodo activo para EXAMENES
            PeriodoInscripcion periodo = PeriodoInscripcion.getPeriodoActivoExamenes();
            boolean periodoActivo = (periodo != null);
            model.put("periodoActivo", periodoActivo);
            if (periodoActivo) {
                model.put("periodoNombre", periodo.getNombre());
                model.put("periodoFin",    periodo.getFechaFin());
            }

            // Construir lista de todos los examenes enriquecida con datos de materia
            // y marcando si el alumno ya esta inscripto en cada uno
            List<ExamenFinal> todosLosExamenes = ExamenFinal.findAll();
            List<Map<String, Object>> examenes = new ArrayList<>();
            for (ExamenFinal e : todosLosExamenes) {
                Map<String, Object> row = new HashMap<>();
                row.put("id",    e.getId());
                row.put("fecha", e.getFecha());
                row.put("hora",  e.getHora());
                row.put("turno", e.getTurno());
                row.put("observaciones", e.getObservaciones());

                Materia mat = Materia.findById(e.getMateriaId());
                row.put("materiaNombre", mat != null ? mat.getNombre() : "(materia eliminada)");
                row.put("materiaCodigo", mat != null ? mat.getCodigo() : "");

                InscripcionExamen ie = InscripcionExamen.findByAlumnoIdAndExamenId(
                        profileId, ((Number) e.getId()).intValue());
                row.put("inscripto",         ie != null);
                row.put("fechaInscripcion",  ie != null ? ie.getFechaInscripcion() : null);

                examenes.add(row);
            }
            model.put("examenes", examenes);

            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);

            return new ModelAndView(model, "examenesDisponibles.mustache");
        }, new MustacheTemplateEngine());

        // INSCRIBIRSE A UN EXAMEN
        // POST /examen/inscribirse/:id
        post("/examen/inscribirse/:id", (req, res) -> {
            Integer profileId = getSessionProfileId(req);
            if (profileId == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden inscribirse a examenes.");
                return null;
            }

            Alumno alumno = Alumno.findById(profileId);
            if (alumno == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden inscribirse a examenes.");
                return null;
            }

            // Validar periodo activo para EXAMENES
            PeriodoInscripcion periodo = PeriodoInscripcion.getPeriodoActivoExamenes();
            if (periodo == null) {
                res.redirect("/examenes/disponibles?error="
                        + java.net.URLEncoder.encode(
                                "No hay un periodo de inscripcion a examenes activo. La inscripcion esta cerrada.",
                                "UTF-8"));
                return null;
            }

            int examenId = Integer.parseInt(req.params(":id"));
            ExamenFinal examen = ExamenFinal.findById(examenId);
            if (examen == null) {
                res.redirect("/examenes/disponibles?error=El examen seleccionado no existe.");
                return null;
            }

            // Validar doble inscripcion
            InscripcionExamen existente = InscripcionExamen.findByAlumnoIdAndExamenId(profileId, examenId);
            if (existente != null) {
                Materia mat = Materia.findById(examen.getMateriaId());
                String nombreMat = mat != null ? mat.getNombre() : "ese examen";
                res.redirect("/examenes/disponibles?error="
                        + java.net.URLEncoder.encode(
                                "Ya estas inscripto al examen de " + nombreMat + ".",
                                "UTF-8"));
                return null;
            }

            try {
                String hoy = java.time.LocalDate.now().toString();
                InscripcionExamen ie = new InscripcionExamen();
                ie.setAlumnoId(profileId);
                ie.setExamenId(examenId);
                ie.setFechaInscripcion(hoy);
                ie.saveIt();

                Materia mat = Materia.findById(examen.getMateriaId());
                String nombreMat = mat != null ? mat.getNombre() : "el examen";
                res.redirect("/examenes/disponibles?message="
                        + java.net.URLEncoder.encode(
                                "Te inscribiste correctamente al examen de " + nombreMat + ".",
                                "UTF-8"));
            } catch (Exception ex) {
                System.err.println("ERROR al inscribir alumno " + profileId
                        + " en examen " + examenId + ": " + ex.getMessage());
                res.redirect("/examenes/disponibles?error=No se pudo completar la inscripcion. Intentá nuevamente.");
            }
            return null;
        });

        // MIS EXAMENES — lista los examenes a los que el alumno esta inscripto.
        // GET /mis-examenes
        get("/mis-examenes", (req, res) -> {
            Integer profileId = getSessionProfileId(req);
            if (profileId == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden acceder a esta seccion.");
                return null;
            }

            Alumno alumno = Alumno.findById(profileId);
            if (alumno == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden acceder a esta seccion.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("alumnoNombre",   alumno.getNombre());
            model.put("alumnoApellido", alumno.getApellido());

            List<InscripcionExamen> inscripciones = InscripcionExamen.findByAlumnoId(profileId);
            List<Map<String, Object>> examenes = new ArrayList<>();
            for (InscripcionExamen ie : inscripciones) {
                Map<String, Object> row = new HashMap<>();
                row.put("fechaInscripcion", ie.getFechaInscripcion());

                ExamenFinal e = ExamenFinal.findById(ie.getExamenId());
                if (e != null) {
                    row.put("fecha", e.getFecha());
                    row.put("hora",  e.getHora());
                    row.put("turno", e.getTurno());
                    Materia mat = Materia.findById(e.getMateriaId());
                    row.put("materiaNombre", mat != null ? mat.getNombre() : "(materia eliminada)");
                } else {
                    row.put("fecha",        "(examen eliminado)");
                    row.put("hora",         null);
                    row.put("turno",        null);
                    row.put("materiaNombre", "(materia eliminada)");
                }
                examenes.add(row);
            }
            model.put("examenes", examenes);
            model.put("tieneExamenes", !examenes.isEmpty());

            return new ModelAndView(model, "misExamenes.mustache");
        }, new MustacheTemplateEngine());

        // ============================================================
        // ISSUE #9 — CONSULTA DE NOTAS POR ALUMNO
        // ============================================================

        // MIS NOTAS — muestra todas las notas cargadas para el alumno autenticado.
        // Lee de alumnos_materias (misma tabla que usa el profesor para cargar notas).
        // Solo muestra filas donde nota IS NOT NULL.
        // GET /mis-notas
        get("/mis-notas", (req, res) -> {
            Integer profileId = getSessionProfileId(req);
            if (profileId == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden consultar sus notas.");
                return null;
            }

            Alumno alumno = Alumno.findById(profileId);
            if (alumno == null) {
                res.redirect("/dashboard?error=Solo los alumnos pueden consultar sus notas.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            model.put("alumnoNombre",   alumno.getNombre());
            model.put("alumnoApellido", alumno.getApellido());

            // Leer de alumnos_materias — la misma tabla que usa el profesor
            List<AlumnoMateria> inscripciones = AlumnoMateria.findByAlumnoId(profileId);
            List<Map<String, Object>> notas = new ArrayList<>();
            for (AlumnoMateria am : inscripciones) {
                // Solo incluir filas con nota cargada
                if (am.getNota() == null) continue;

                Map<String, Object> row = new HashMap<>();
                row.put("nota", am.getNota().toString());

                Materia m = Materia.findById(am.getMateriaId());
                row.put("materiaNombre", m != null ? m.getNombre() : "(materia eliminada)");
                row.put("materiaCodigo", m != null ? m.getCodigo() : "");
                notas.add(row);
            }
            model.put("notas", notas);
            model.put("tieneNotas", !notas.isEmpty());

            // Calcular promedio si hay notas
            if (!notas.isEmpty()) {
                double suma = 0;
                int count = 0;
                for (AlumnoMateria am : inscripciones) {
                    if (am.getNota() != null) {
                        suma += am.getNota();
                        count++;
                    }
                }
                // Formatear a 2 decimales
                String promedio = String.format("%.2f", suma / count);
                model.put("promedio", promedio);
            }

            return new ModelAndView(model, "misNotas.mustache");
        }, new MustacheTemplateEngine());

        get("/alumnos/listar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            List<Alumno> alumnosRaw = Alumno.findAll();
            List<Map<String, Object>> alumnos = new ArrayList<>();

            for (Alumno a : alumnosRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", a.getId());
                row.put("nombre", a.getNombre());
                row.put("apellido", a.getApellido());
                row.put("correo", a.getCorreo());
                row.put("dni", a.getDni());
                alumnos.add(row);
            }

            model.put("alumnos", alumnos);
            return new ModelAndView(model, "alumnosList.mustache");
        }, new MustacheTemplateEngine());


        get("/carrera/:id/materias", (req, res) -> {
            int carreraId = Integer.parseInt(req.params(":id"));
            Carrera carrera = Carrera.findById(carreraId);

            List<Materia> materias = Materia.find(
                "id IN (SELECT materia_id FROM carrera_materias WHERE carrera_id = ?)", carreraId
            );

            Map<String, Object> model = new HashMap<>();
            model.put("carrera", carrera);
            model.put("materias", materias);

            return new ModelAndView(model, "carreraMaterias.mustache");
        }, new MustacheTemplateEngine());

        get("/profesor/:id/materias", (req, res) -> {
            int profesorId = Integer.parseInt(req.params(":id"));
            Profesor profesor = Profesor.findById(profesorId);
            if (profesor == null) {
                res.redirect("/dashboard?error=Profesor no encontrado.");
                return null;
            }

            // Obtener la carrera del profesor
            Integer carreraId = profesor.getInteger("carrera_id");

            // Traer solo las materias de esa carrera
            List<Materia> materias = Materia.find(
                "id IN (SELECT materia_id FROM carrera_materias WHERE carrera_id = ?)", carreraId
            );

            Map<String, Object> model = new HashMap<>();
            model.put("profesor", profesor);
            model.put("materias", materias);

            return new ModelAndView(model, "profesorMaterias.mustache");
        }, new MustacheTemplateEngine());


        
        
        
        System.out.println(BCrypt.hashpw("user", BCrypt.gensalt()));


    } // fin main()

    // ----------------------------------------------------------------
    // Helpers privados
    // ----------------------------------------------------------------

    private static void addTipo(Map<String, Object> model, String tipo) {
        if (tipo != null && !tipo.isEmpty()) {
            model.put("tipo", tipo);
            if ("alumno".equals(tipo))   model.put("esAlumno", true);
            if ("profesor".equals(tipo)) model.put("esProfesor", true);
            if ("admin".equals(tipo))    model.put("esAdmin", true);
        }
    }

    // Helper: obtener el profileId para alumno/profesor desde sesión
    private static Integer getSessionProfileId(spark.Request req) {
        String tipo = req.session().attribute("userTipo");
        if ("alumno".equals(tipo) || "profesor".equals(tipo)) {
            Object profileId = req.session().attribute("profileId");
            if (profileId instanceof Number) {
                return ((Number) profileId).intValue();
            }
            if (profileId instanceof String) {
                try {
                    return Integer.parseInt((String) profileId);
                } catch (NumberFormatException ignored) {
                }
            }
            Object userId = req.session().attribute("userId");
            if (userId instanceof Number) {
                return ((Number) userId).intValue();
            }
            if (userId instanceof String) {
                try {
                    return Integer.parseInt((String) userId);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Valida los campos comunes de alta y edición de PeriodoInscripcion.
     * Devuelve null si todo es válido, o el mensaje de error codificado para URL si no lo es.
     */
    private static String validarCamposPeriodo(String nombre, String tipo,
                                               String fechaInicio, String fechaFin) {
        if (nombre == null || nombre.trim().isEmpty())
            return "El nombre es obligatorio.";
        if (tipo == null || tipo.isEmpty() || (!tipo.equals("MATERIAS") && !tipo.equals("EXAMENES")))
            return "El tipo es obligatorio y debe ser MATERIAS o EXAMENES.";
        if (fechaInicio == null || fechaInicio.isEmpty())
            return "La fecha de inicio es obligatoria.";
        if (fechaFin == null || fechaFin.isEmpty())
            return "La fecha de fin es obligatoria.";

        try {
            java.time.LocalDate inicio = java.time.LocalDate.parse(fechaInicio);
            java.time.LocalDate fin    = java.time.LocalDate.parse(fechaFin);
            if (fin.isBefore(inicio))
                return "La fecha de fin no puede ser anterior a la fecha de inicio.";
        } catch (Exception e) {
            return "Las fechas deben tener el formato YYYY-MM-DD.";
        }

        return null; // sin errores
    }

    /**
     * Convierte un numero de año a su representacion en texto para el plan de estudios.
     * Ejemplo: 1 → "Primer Año", 2 → "Segundo Año", etc.
     */
    private static String anioEnLetras(int anio) {
        switch (anio) {
            case 1: return "Primer Año";
            case 2: return "Segundo Año";
            case 3: return "Tercer Año";
            case 4: return "Cuarto Año";
            case 5: return "Quinto Año";
            case 6: return "Sexto Año";
            default: return "Año " + anio;
        }
    }



}
