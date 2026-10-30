package ar.com.atp.puestos.nucleo.web.rest;

import ar.com.atp.puestos.nucleo.service.Credencial;
import ar.com.atp.puestos.nucleo.service.DatosPeticion;
import ar.com.atp.puestos.nucleo.service.PerfilAcceso;
import ar.com.atp.puestos.nucleo.service.ResultadoAcceso;
import ar.com.atp.puestos.nucleo.service.ServicioAcceso;
import ar.com.atp.puestos.nucleo.web.dto.AuthLoginRequestDto;
import ar.com.atp.puestos.nucleo.web.dto.AuthLoginResponseDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Canal WS del login: {@code POST /api/v1/auth/login} (spec 03-login §3.8, R-LOG-027).
 *
 * <p>Invoca el <strong>perfil WS</strong> ({@code Seg_ControlAccesoWS}): sin contexto de sesión,
 * sin chequeo de IP y sin rama de historia de comisión. El canal es <strong>stateless</strong>
 * (R-LOG-028): no crea sesión, no escribe la cookie {@code SUCURSAL} y no exige token CSRF
 * (D-LOG-17(a)); la autenticación es {@code permitAll} (D-LOG-19(a)).
 *
 * <p>La respuesta es <strong>siempre HTTP 200</strong> con la forma del SDT
 * {@code SDTSalidaLoginWS}: el WS legacy nunca devuelve un estado de error.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ServicioAcceso servicioAcceso;

    public AuthController(ServicioAcceso servicioAcceso) {
        this.servicioAcceso = servicioAcceso;
    }

    /**
     * Login del canal WS.
     *
     * @param peticion credenciales ({@code usuario}, {@code pass}, {@code tipo})
     * @return el codigo de resultado y la sucursal y la comision resueltas
     */
    @PostMapping("/login")
    public AuthLoginResponseDto login(@Valid @RequestBody AuthLoginRequestDto peticion) {
        Credencial credencial = new Credencial(peticion.usuario(), peticion.pass(), peticion.tipo());
        ResultadoAcceso resultado =
                servicioAcceso.autenticar(PerfilAcceso.WS, credencial, DatosPeticion.sinIp());
        return new AuthLoginResponseDto(resultado.codigo(), resultado.sucursal(), resultado.comision());
    }
}
