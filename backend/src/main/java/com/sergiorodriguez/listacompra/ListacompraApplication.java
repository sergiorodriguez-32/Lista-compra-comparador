package com.sergiorodriguez.listacompra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase de arranque de la aplicación.
 * <p>
 * {@code @SpringBootApplication} activa la autoconfiguración de Spring Boot y el escaneo de
 * componentes (@RestController, @Service, @Entity...) en este paquete y en todos sus
 * subpaquetes, por eso todo el código debe vivir bajo {@code com.sergiorodriguez.listacompra}.
 */
@SpringBootApplication
public class ListacompraApplication {

	public static void main(String[] args) {
		// Arranca el contenedor de Spring y el servidor Tomcat embebido (puerto 8080)
		SpringApplication.run(ListacompraApplication.class, args);
	}

}
