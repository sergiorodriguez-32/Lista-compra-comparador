package com.sergiorodriguez.listacompra.core;

import com.sergiorodriguez.listacompra.core.exception.EmailAlreadyRegisteredException;
import com.sergiorodriguez.listacompra.core.exception.InvalidCredentialsException;
import com.sergiorodriguez.listacompra.security.JwtService;
import com.sergiorodriguez.listacompra.web.LoginRequest;
import com.sergiorodriguez.listacompra.web.LoginResponse;
import com.sergiorodriguez.listacompra.web.RegisterRequest;
import com.sergiorodriguez.listacompra.web.UserResponse;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Lógica de negocio de los usuarios. No sabe nada de HTTP: el controlador le pasa los datos
 * y ella aplica las reglas (email único, contraseña cifrada) y usa el repositorio.
 */
@Service
@RequiredArgsConstructor // constructor con los campos final: así Spring inyecta las dependencias
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // bean definido en PasswordConfig (BCrypt)
    private final JwtService jwtService;

    /**
     * Registra un usuario nuevo (RF-01).
     *
     * @throws EmailAlreadyRegisteredException si el email ya existe (se traducirá a un 409)
     */
    @Transactional // todo el método es una transacción: si algo falla, no se guarda nada
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        // Se guarda el hash BCrypt, nunca la contraseña en claro (RNF-01)
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());

        // save devuelve la entidad ya con el id que le ha asignado la BD
        User saved = userRepository.save(user);

        // Se devuelve un DTO, no la entidad, para no exponer el hash de la contraseña
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getDisplayName());
    }

    /**
     * Inicia sesión (RF-02): comprueba email y contraseña y devuelve un JWT.
     *
     * @throws InvalidCredentialsException si el email no existe o la contraseña no coincide
     *         (mismo error en ambos casos para no revelar qué emails están registrados)
     */
    @Transactional(readOnly = true) // solo lee de la BD, no escribe nada
    public LoginResponse login(LoginRequest request) {
        // orElseThrow: si findByEmail devuelve Optional vacío (email no existe), lanza la excepción
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // matches NO descifra el hash: recalcula con la sal que lleva dentro y compara
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // Contraseña correcta: se emite el token que el cliente enviará en cada petición
        return new LoginResponse(jwtService.generateToken(user));
    }
}
