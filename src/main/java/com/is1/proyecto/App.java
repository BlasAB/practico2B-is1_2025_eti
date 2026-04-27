package com.is1.proyecto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.Base;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.is1.proyecto.config.DBConfigSingleton;
import com.is1.proyecto.models.Alumno;
import com.is1.proyecto.models.Materia;
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
                Base.open(dbConfig.getDriver(), dbConfig.getDbUrl(), dbConfig.getUser(), dbConfig.getPass());
                System.out.println(req.url());
            } catch (Exception e) {
                System.err.println("Error al abrir conexion: " + e.getMessage());
                halt(500, "{\"error\": \"Error interno del servidor.\"}");
            }
        });

        after((req, res) -> {
            try { Base.close(); } catch (Exception e) { System.err.println("Error al cerrar conexion: " + e.getMessage()); }
        });

        // LOGIN PRINCIPAL
        get("/", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String error = req.queryParams("error");
            String msg   = req.queryParams("message");
            String tipo  = req.queryParams("tipo");
            if (error != null && !error.isEmpty())  model.put("errorMessage", error);
            if (msg   != null && !msg.isEmpty())    model.put("successMessage", msg);
            if (tipo  != null && !tipo.isEmpty()) {
                model.put("tipo", tipo);
                if ("alumno".equals(tipo))   model.put("esAlumno", true);
                if ("profesor".equals(tipo)) model.put("esProfesor", true);
            }
            return new ModelAndView(model, "login.mustache");
        }, new MustacheTemplateEngine());

        get("/login", (req, res) -> { res.redirect("/"); return null; });

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

        // DASHBOARD
        get("/dashboard", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String currentUsername = req.session().attribute("currentUserUsername");
            Boolean loggedIn = req.session().attribute("loggedIn");
            if (currentUsername == null || loggedIn == null || !loggedIn) {
                res.redirect("/?error=Debes iniciar sesion para acceder a esta pagina.");
                return null;
            }
            String tipo = req.session().attribute("userTipo");
            model.put("username", currentUsername);
            addTipo(model, tipo);
            return new ModelAndView(model, "dashboard.mustache");
        }, new MustacheTemplateEngine());

        // LOGOUT
        get("/logout", (req, res) -> {
            req.session().invalidate();
            res.redirect("/?message=Sesion cerrada correctamente.");
            return null;
        });

        // REGISTRO DE ALUMNO
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

                res.redirect("/alumno/registrar?successMessage=Alumno registrado correctamente. Ya podes iniciar sesion.");
            } catch (Exception e) {
                System.err.println("ERROR al registrar alumno: " + e.getMessage());
                res.redirect("/alumno/registrar?error=No se pudo registrar el alumno. El correo o el DNI pueden estar en uso.");
            }
            return null;
        });

        // REGISTRO DE PROFESOR
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

        // PANEL ADMIN
        get("/admin/panel", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("alumnos", Alumno.findAll());
            model.put("profesores", Profesor.findAll());
            String err = req.queryParams("error");
            String ok  = req.queryParams("message");
            if (err != null && !err.isEmpty()) model.put("error", err);
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);
            return new ModelAndView(model, "adminPanel.mustache");
        }, new MustacheTemplateEngine());

        get("/admin/alumno/nuevo", (req, res) -> { res.redirect("/alumno/registrar"); return null; });

        post("/alumno/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Alumno a = Alumno.findById(id);
            if (a != null) {
                a.set("nombre", req.queryParams("nombre"));
                a.set("apellido", req.queryParams("apellido"));
                a.set("correo", req.queryParams("correo"));
                a.set("dni", req.queryParams("dni"));
                a.saveIt();
            }
            res.redirect("/admin/panel");
            return null;
        });

        get("/alumno/eliminar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Alumno a = Alumno.findById(id);
            if (a != null) a.delete();
            res.redirect("/admin/panel");
            return null;
        });

        post("/profesor/editar/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            Profesor p = Profesor.findById(id);
            if (p != null) {
                p.set("nombre", req.queryParams("nombre"));
                p.set("apellido", req.queryParams("apellido"));
                p.set("correo", req.queryParams("correo"));
                p.set("dni", req.queryParams("dni"));
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

        // USUARIOS GENERICO (legacy)
        get("/user/create", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String ok  = req.queryParams("message");
            String err = req.queryParams("error");
            if (ok  != null && !ok.isEmpty())  model.put("successMessage", ok);
            if (err != null && !err.isEmpty()) model.put("errorMessage", err);
            return new ModelAndView(model, "user_form.mustache");
        }, new MustacheTemplateEngine());

        get("/user/new", (req, res) -> new ModelAndView(new HashMap<>(), "user_form.mustache"), new MustacheTemplateEngine());

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

        // MATERIAS
        post("/materia/registrar", (req, res) -> {
            Materia m = new Materia();
            m.set("nombre", req.queryParams("nombre"));
            m.set("codigo", req.queryParams("codigo"));
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
                m.set("nombre", req.queryParams("nombre"));
                m.set("codigo", req.queryParams("codigo"));
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

    }

    private static void addTipo(Map<String, Object> model, String tipo) {
        if (tipo != null && !tipo.isEmpty()) {
            model.put("tipo", tipo);
            if ("alumno".equals(tipo))   model.put("esAlumno", true);
            if ("profesor".equals(tipo)) model.put("esProfesor", true);
        }
    }

}