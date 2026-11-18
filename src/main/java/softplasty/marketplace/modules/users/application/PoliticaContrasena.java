package softplasty.marketplace.modules.users.application;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

@Component
public class PoliticaContrasena {

    public void validar(String contrasena) {
        if (contrasena == null || contrasena.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña excede la longitud máxima admitida.");
        }
    }
}
