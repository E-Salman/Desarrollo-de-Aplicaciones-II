package ar.edu.uade.inversorar.business;

import ar.edu.uade.inversorar.data.Usuario;

public interface UsuarioService {

    Usuario login(
            String email,
            String password
    );

    Usuario registrar(
            String nombre,
            String apellido,
            String email,
            String password
    );
}