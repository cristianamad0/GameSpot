package com.usta.gamespot.exception;

public class EmailYaRegistradoException extends RuntimeException {
    public EmailYaRegistradoException(String email) {
        super("Ya existe una cuenta registrada con el correo: " + email);
    }
}
