package com.is1.proyecto;
import org.mindrot.jbcrypt.BCrypt;

public class HashGenerator {
    public static void main(String[] args) {
        String password = "user"; // la contraseña en texto plano
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("Hash generado: " + hash);
    }
}

