package ar.com.atp.puestos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto de Spring arranca sin conexion a SQL Server (T-005),
 * usando el perfil {@code test} (H2 en memoria, {@code ddl-auto=none}).
 */
@SpringBootTest
@ActiveProfiles("test")
class PuestosApplicationTests {

    @Test
    void contextLoads() {
        // El arranque del contexto es la verificacion.
    }
}
