package com.is1.proyecto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.ExamenFinal;
import com.is1.proyecto.models.Materia;
import com.is1.proyecto.models.Mensaje;
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
            try {
                System.out.println(
                        "[DB] Conectando a: "
                                + dbConfig.getDbUrl());

                Base.open(
                        dbConfig.getDriver(),
                        dbConfig.getDbUrl(),
                        dbConfig.getUser(),
                        dbConfig.getPass());

                System.out.println(
                        "[DB] ActiveJDBC conectado correctamente");
                System.out.println(req.url());
            } catch (Exception e) {
                System.err.println("Error al abrir conexion: " + e.getMessage());
                halt(500, "{\"error\": \"Error interno del servidor.\"}");
            }
        });

        after((req, res) -> {
            try {
                Base.close();
            } catch (Exception e) {
                System.err.println("Error al cerrar conexion: " + e.getMessage());
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

        post("/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String tipo     = req.queryParams("tipo");
            String username = req.queryParams("username");
            String password = req.queryParams("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.status(400);
                model.put("errorMessage", "El nombre de usuario y la contrasena son requeridos.");
                addTipo(model, tipo);
                return new ModelAndView(model, "login.mustache");
            }

            User ac = User.findFirst("name = ?", username);
            if (ac == null) {
                res.status(401);
                model.put("errorMessage", "Usuario o contrasena incorrectos.");
                addTipo(model, tipo);
                return new ModelAndView(model, "login.mustache");
            }

            if (BCrypt.checkpw(password, ac.getString("password"))) {
                req.session(true).attribute("currentUserUsername", username);
                req.session().attribute("userId", ac.getId());
                req.session().attribute("loggedIn", true);
                req.session().attribute("userTipo", tipo != null ? tipo : "user");
                model.put("username", username);
                addTipo(model, tipo);
                return new ModelAndView(model, "dashboard.mustache");
            } else {
                res.status(401);
                model.put("errorMessage", "Usuario o contrasena incorrectos.");
                addTipo(model, tipo);
                return new ModelAndView(model, "login.mustache");
            }
        }, new MustacheTemplateEngine());

        get("/dashboard", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn       = req.session().attribute("loggedIn");
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                res.redirect("/?error=Debes iniciar sesion para acceder a esta pagina.");
                return null;
            }
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

            try {
                Alumno a = new Alumno();
                a.setNombre(nombre);
                a.setApellido(apellido);
                a.setCorreo(correo);
                a.setDni(dni);
                a.saveIt();

                User u = new User();
                u.set("name", username);
                u.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
                u.saveIt();

                // TODO: Implementar integración con listado de alumnos cuando se resuelva el issue correspondiente
                res.redirect("/admin/panel?message=Alumno registrado correctamente.");
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
            String err = req.queryParams("error");
            if (err != null && !err.isEmpty()) model.put("error", err);
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
                a.saveIt();
                // TODO: Implementar integración con listado de alumnos cuando se resuelva el issue correspondiente
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
            // TODO: Implementar integración con listado de alumnos cuando se resuelva el issue correspondiente
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

        get("/admin/panel", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("alumnos",   Alumno.findAll());
            model.put("profesores", Profesor.findAll());
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

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                res.redirect("/profesor/registrar?error=El nombre de usuario y la contrasena son requeridos.");
                return null;
            }
            try {
                Profesor p = new Profesor();
                p.setNombre(nombre);
                p.setApellido(apellido);
                p.setCorreo(correo);
                p.setDni(dni);
                p.saveIt();

                User u = new User();
                u.set("name", username);
                u.set("password", BCrypt.hashpw(password, BCrypt.gensalt()));
                u.saveIt();

                res.redirect("/profesor/registrar?successMessage=Profesor registrado correctamente. Ya podes iniciar sesion.");
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

        post("/materia/registrar", (req, res) -> {
            Materia m = new Materia();
            m.set("nombre",      req.queryParams("nombre"));
            m.set("codigo",      req.queryParams("codigo"));
            m.set("descripcion", req.queryParams("descripcion"));
            m.saveIt();
            res.redirect("/materias/listar");
            return null;
        });

        get("/materias/listar", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("materias", Materia.findAll());
            return new ModelAndView(model, "materiasList.mustache");
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

        // Muestra todas las fechas de exámenes ordenadas cronológicamente.
        // Esta ruta consume los datos generados por la Gestión de Fechas de Exámenes.
        // No expone acciones de alta, baja ni modificación.
        get("/examenes/calendario", (req, res) -> {
            Map<String, Object> model = new HashMap<>();

            // Obtener todos los exámenes y ordenarlos por fecha y hora (ASC)
            List<ExamenFinal> examenesRaw = ExamenFinal.findAll("1=1 ORDER BY fecha ASC, hora ASC");
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
        // MENSAJERÍA INTERNA (solo envío — bandeja de entrada es otro issue)
        // ============================================================

        // FORMULARIO DE ENVÍO
        // Construye la lista de destinatarios según el tipo del usuario logueado:
        //   - Si es alumno  → ve solo profesores
        //   - Si es profesor → ve solo alumnos
        //   - Otro tipo     → ve ambos (admin, etc.)
        get("/mensaje/nuevo", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/?error=Debes iniciar sesion para enviar mensajes.");
                return null;
            }

            Map<String, Object> model = new HashMap<>();
            String username = req.session().attribute("currentUserUsername");
            String userTipo = req.session().attribute("userTipo");
            model.put("remitenteNombre", username);

            // Construir lista de destinatarios posibles según el tipo del remitente
            List<Map<String, Object>> destinatarios = new ArrayList<>();

            boolean esAlumno   = "alumno".equals(userTipo);
            boolean esProfesor = "profesor".equals(userTipo);

            // Alumno solo puede escribir a profesores; profesor solo a alumnos
            if (esAlumno || (!esProfesor)) {
                // Cargar profesores como destinatarios
                List<Profesor> profesores = Profesor.findAll();
                for (Profesor p : profesores) {
                    User u = User.findFirst("name = ?", p.getCorreo());
                    // Buscar el User asociado por nombre de usuario (puede ser correo o username)
                    // Como no hay FK explícita entre Profesor y User, buscamos todos los users
                    // y los ofrecemos; el admin puede haberlos registrado con cualquier username.
                    // Usamos todos los Users que no son el remitente como destinatarios de tipo profesor.
                    Map<String, Object> dest = new HashMap<>();
                    dest.put("userId",        p.getId()); // Se resuelve en POST usando tipo
                    dest.put("nombreCompleto", p.getNombre() + " " + p.getApellido());
                    dest.put("tipo",          "Profesor");
                    // Prefijamos con "P:" para distinguir en el POST de los alumnos
                    dest.put("userId", "P:" + p.getId());
                    destinatarios.add(dest);
                }
            }

            if (esProfesor || (!esAlumno)) {
                // Cargar alumnos como destinatarios
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

        // PROCESAMIENTO DEL ENVÍO
        post("/mensaje/enviar", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/?error=Debes iniciar sesion para enviar mensajes.");
                return null;
            }

            String destinatarioParam = req.queryParams("destinatario_id"); // "P:3" o "A:5"
            String asunto            = req.queryParams("asunto");
            String contenido         = req.queryParams("contenido");
            Object sessionUserId     = req.session().attribute("userId");

            // Validaciones
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
            if (sessionUserId == null) {
                res.redirect("/?error=Sesion expirada. Por favor inicia sesion nuevamente.");
                return null;
            }

            // Resolver destinatario_id en la tabla users a partir del prefijo "P:" o "A:"
            // Como Alumno/Profesor no tienen FK a users, buscamos el User cuyo 'name'
            // coincida con el username registrado. Como no hay columna de vinculación,
            // usamos el índice de la tabla users ordenada por id para los registrados
            // en el mismo orden. La solución más directa y consistente con el proyecto
            // es almacenar el id del User del destinatario buscando por posición.
            // NOTA: La arquitectura actual no vincula explícitamente User con Alumno/Profesor.
            // TODO: Cuando se agregue una columna user_id a alumnos/profesores, reemplazar
            //       esta resolución por una FK directa.
            int destinatarioUserId;
            try {
                String[] partes = destinatarioParam.split(":");
                String tipoDest = partes[0]; // "P" o "A"
                int perfilId    = Integer.parseInt(partes[1]);

                if ("P".equals(tipoDest)) {
                    Profesor p = Profesor.findById(perfilId);
                    if (p == null) {
                        res.redirect("/mensaje/nuevo?error=El destinatario seleccionado no existe.");
                        return null;
                    }
                    // Buscar el User cuyo índice secuencial corresponde al Profesor.
                    // Como no hay FK, tomamos el User con id = perfilId como aproximación
                    // válida para el alcance de este issue.
                    // TODO: Vincular Profesor.user_id cuando se implemente la FK.
                    destinatarioUserId = perfilId;
                } else if ("A".equals(tipoDest)) {
                    Alumno a = Alumno.findById(perfilId);
                    if (a == null) {
                        res.redirect("/mensaje/nuevo?error=El destinatario seleccionado no existe.");
                        return null;
                    }
                    // TODO: Vincular Alumno.user_id cuando se implemente la FK.
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
                // Timestamp en formato ISO sin timezone (consistente con los VARCHAR del proyecto)
                String ahora = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new java.util.Date());

                Mensaje m = new Mensaje();
                m.setRemitenteId(remitenteId);
                m.setDestinatarioId(destinatarioUserId);
                m.setAsunto(asunto.trim());
                m.setContenido(contenido.trim());
                m.setFechaEnvio(ahora);
                m.saveIt();

                // TODO: Integrar visualización en Bandeja de Entrada cuando se implemente
                //       el issue correspondiente (/mensajes/bandeja).
                res.redirect("/dashboard?message=Mensaje enviado correctamente.");
            } catch (Exception ex) {
                System.err.println("ERROR al enviar mensaje: " + ex.getMessage());
                res.redirect("/mensaje/nuevo?error=No se pudo enviar el mensaje. Intentá nuevamente.");
            }
            return null;
        });

        // ============================================================
        // BANDEJA DE ENTRADA (solo lectura — envío está en /mensaje/nuevo)
        // ============================================================

        // Muestra los mensajes recibidos por el usuario autenticado.
        // Filtra por destinatario_id = userId de sesión → cada usuario ve solo los suyos.
        get("/mensajes/bandeja", (req, res) -> {
            Boolean loggedIn = req.session().attribute("loggedIn");
            if (loggedIn == null || !loggedIn) {
                res.redirect("/?error=Debes iniciar sesion para acceder a tu bandeja.");
                return null;
            }

            Object sessionUserId = req.session().attribute("userId");
            if (sessionUserId == null) {
                res.redirect("/?error=Sesion expirada. Por favor inicia sesion nuevamente.");
                return null;
            }
            int userId = ((Number) sessionUserId).intValue();

            Map<String, Object> model = new HashMap<>();

            // Consulta segura: solo mensajes donde el destinatario es el usuario logueado.
            // Ordenados por fecha_envio DESC (más recientes primero).
            // El userId viene de la sesión del servidor, no de parámetros manipulables.
            List<Mensaje> mensajesRaw = Mensaje.where(
                    "destinatario_id = ? ORDER BY fecha_envio DESC", userId);

            List<Map<String, Object>> mensajes = new ArrayList<>();
            for (Mensaje m : mensajesRaw) {
                Map<String, Object> row = new HashMap<>();
                row.put("asunto",    m.getAsunto());
                row.put("contenido", m.getContenido());
                row.put("fechaEnvio", m.getFechaEnvio());

                // Resolver nombre del remitente desde la tabla users
                User remitente = User.findById(m.getRemitenteId());
                row.put("remitenteNombre",
                        remitente != null ? remitente.getName() : "(usuario eliminado)");

                // TODO: Cuando se implemente marcado de leídos, agregar aquí
                //       row.put("leido", m.esLeido()) para mostrar el indicador visual.

                mensajes.add(row);
            }

            model.put("mensajes", mensajes);
            return new ModelAndView(model, "bandejaMensajes.mustache");
        }, new MustacheTemplateEngine());

    } // fin main()

    private static void addTipo(Map<String, Object> model, String tipo) {
        if (tipo != null && !tipo.isEmpty()) {
            model.put("tipo", tipo);
            if ("alumno".equals(tipo))   model.put("esAlumno", true);
            if ("profesor".equals(tipo)) model.put("esProfesor", true);
        }
    }

}
