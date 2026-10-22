package ar.com.atp.puestos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend de ATPPuestos (modulo nucleo de datos).
 *
 * <p>Replatforming del sistema legacy GeneXus (15/18) a Java 17 + Spring Boot 3.x.
 * La persistencia reutiliza el esquema fisico existente en SQL Server ({@code Rentas});
 * {@code ddl-auto=none} garantiza que Hibernate no genere DDL (R-204, R-205).</p>
 */
@SpringBootApplication
public class PuestosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PuestosApplication.class, args);
    }
}
