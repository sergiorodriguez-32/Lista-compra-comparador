package com.sergiorodriguez.listacompra.core;

import com.sergiorodriguez.listacompra.core.exception.EmailAlreadyRegisteredException;
import com.sergiorodriguez.listacompra.core.exception.InvalidCredentialsException;
import com.sergiorodriguez.listacompra.security.JwtService;
import com.sergiorodriguez.listacompra.web.LoginRequest;
import com.sergiorodriguez.listacompra.web.LoginResponse;
import com.sergiorodriguez.listacompra.web.RegisterRequest;
import com.sergiorodriguez.listacompra.web.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("register con datos válidos guarda el usuario con la contraseña cifrada y devuelve el DTO")
    void register_conDatosValidos_guardaUsuarioConPasswordCifrada() {
        // GIVEN
        RegisterRequest request = new RegisterRequest("a@b.com", "clave1234", "Ana");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("clave1234")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L); // simula el id que asigna la BD
            return u;
        });

        // WHEN
        UserResponse respuesta = userService.register(request);

        // THEN: respuesta
        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.email()).isEqualTo("a@b.com");
        assertThat(respuesta.displayName()).isEqualTo("Ana");

        // THEN: lo que se guardó
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User guardado = captor.getValue();
        assertThat(guardado.getEmail()).isEqualTo("a@b.com");
        assertThat(guardado.getDisplayName()).isEqualTo("Ana");
        assertThat(guardado.getPasswordHash()).isEqualTo("HASH").isNotEqualTo("clave1234");
    }

    @Test
    @DisplayName("register con email ya existente lanza la excepción y no guarda nada")
    void register_conEmailDuplicado_lanzaExcepcionYNoGuarda() {
        // GIVEN
        RegisterRequest request = new RegisterRequest("a@b.com", "clave1234", "Ana");
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        // WHEN + THEN
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("a@b.com");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("login con credenciales válidas devuelve el token")
    void login_conCredencialesValidas_devuelveToken() {
        // GIVEN: un usuario guardado y los mocks programados para el camino feliz
        LoginRequest request = new LoginRequest("a@b.com", "clave1234");
        User user = new User();
        user.setEmail("a@b.com");
        user.setPasswordHash("HASH");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("clave1234", "HASH")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("TOKEN");

        // WHEN
        LoginResponse respuesta = userService.login(request);

        // THEN
        assertThat(respuesta.token()).isEqualTo("TOKEN");
    }

    @Test
    @DisplayName("login con email inexistente lanza InvalidCredentialsException y no genera token")
    void login_conEmailInexistente_lanzaInvalidCredentials() {
        // GIVEN: findByEmail devuelve una caja vacía
        LoginRequest request = new LoginRequest("nadie@b.com", "clave1234");
        when(userRepository.findByEmail("nadie@b.com")).thenReturn(Optional.empty());

        // WHEN + THEN
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login con contraseña incorrecta lanza InvalidCredentialsException y no genera token")
    void login_conPasswordIncorrecta_lanzaInvalidCredentials() {
        // GIVEN: el usuario existe pero matches da false
        LoginRequest request = new LoginRequest("a@b.com", "mala");
        User user = new User();
        user.setEmail("a@b.com");
        user.setPasswordHash("HASH");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mala", "HASH")).thenReturn(false);

        // WHEN + THEN
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}
