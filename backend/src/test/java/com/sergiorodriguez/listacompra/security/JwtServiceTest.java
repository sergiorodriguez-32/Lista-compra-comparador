package com.sergiorodriguez.listacompra.security;

import com.sergiorodriguez.listacompra.core.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test unitario sin Mockito: JwtService no depende de nada externo (solo de un secreto y una
 * duración), así que se construye de verdad y se prueba con tokens reales.
 */
class JwtServiceTest {

    // Mínimo 32 caracteres: HS256 exige una clave de al menos 256 bits
    private static final String SECRET = "secreto-de-prueba-con-mas-de-32-caracteres-seguro";
    private static final long UNA_HORA_MS = 3_600_000;

    private JwtService jwtService;
    private User user;

    @BeforeEach // se ejecuta antes de CADA test: así todos parten de un estado limpio
    void setUp() {
        jwtService = new JwtService(SECRET, UNA_HORA_MS);
        user = new User();
        user.setId(1L);
        user.setEmail("a@b.com");
    }

    @Test
    @DisplayName("un token recién generado se valida y devuelve el email del usuario")
    void generateToken_luegoExtractEmail_devuelveElEmail() {
        // WHEN
        String token = jwtService.generateToken(user);

        // THEN: tiene las 3 partes cabecera.payload.firma y el email sale del propio token
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtService.extractEmail(token)).isEqualTo("a@b.com");
    }

    @Test
    @DisplayName("un token con la firma alterada es rechazado")
    void extractEmail_conFirmaAlterada_lanzaJwtException() {
        // GIVEN: un token válido al que se le cambia el primer carácter de la firma
        String[] partes = jwtService.generateToken(user).split("\\.");
        char primero = partes[2].charAt(0);
        String firmaFalsa = (primero == 'A' ? 'B' : 'A') + partes[2].substring(1);
        String tokenManipulado = partes[0] + "." + partes[1] + "." + firmaFalsa;

        // WHEN + THEN
        assertThatThrownBy(() -> jwtService.extractEmail(tokenManipulado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("un token firmado con otra clave es rechazado")
    void extractEmail_conTokenDeOtraClave_lanzaJwtException() {
        // GIVEN: un atacante que no conoce nuestro secreto firma con el suyo
        JwtService servicioAtacante =
                new JwtService("otro-secreto-distinto-de-mas-de-32-caracteres-xx", UNA_HORA_MS);
        String tokenFalso = servicioAtacante.generateToken(user);

        // WHEN + THEN: nuestro servicio recalcula la firma con SU clave y no coincide
        assertThatThrownBy(() -> jwtService.extractEmail(tokenFalso))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("un token caducado es rechazado")
    void extractEmail_conTokenCaducado_lanzaExpiredJwtException() {
        // GIVEN: duración negativa, así el token nace ya caducado
        JwtService servicioCaducado = new JwtService(SECRET, -1000);
        String tokenCaducado = servicioCaducado.generateToken(user);

        // WHEN + THEN
        assertThatThrownBy(() -> jwtService.extractEmail(tokenCaducado))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
