package com.proyecto.servicios.validation;

import com.proyecto.servicios.model.onboarding.ClienteConsultaRequest;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.LoginRegistroRequest;
import com.proyecto.servicios.model.onboarding.NacionalidadRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba las reglas de formato de los DTOs (sin Spring ni base de datos):
 * campos vacíos, longitudes, caracteres especiales, formatos de correo,
 * teléfono, CURP/RFC, código postal, ingreso, contraseña, XSS e inyección SQL.
 */
class ValidacionRequestsTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    // ---------- utilidades ----------

    private static ClienteRequest clienteValido() {
        ClienteRequest r = new ClienteRequest();
        r.setNombre("Juan");
        r.setApellidoPaterno("Garcia");
        r.setApellidoMaterno("Lopez");
        r.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        r.setCurp("GALJ900101HDFRPN01");
        r.setRfc("GALJ900101A01");
        r.setSexo("H");
        r.setNacionalidad(List.of(nacionalidad(1, "Mexicana")));
        r.setEstadoCivil("Soltero(a)");
        r.setCorreoElectronico("juan.garcia@correo.com");
        r.setTelefonoMovil(5512345678L);
        r.setOcupacion("Ingeniero");
        r.setEmpresa("ACME S.A. de C.V.");
        r.setIngresoMensual(new BigDecimal("15000.00"));
        r.setDomicilio(domicilioValido());
        return r;
    }

    private static DomicilioRequest domicilioValido() {
        DomicilioRequest d = new DomicilioRequest();
        d.setCalle("Av. Reforma #100");
        d.setNumeroExterior("45-B");
        d.setColonia("Centro");
        d.setMunicipio("Cuauhtémoc");
        d.setEstado("Ciudad de México");
        d.setCodigoPostal("06000");
        d.setPais("México");
        return d;
    }

    /** Campos (con ruta, p. ej. "domicilio.calle") que tienen al menos un error. */
    private static Set<String> camposConError(Object objeto) {
        return VALIDADOR.validate(objeto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static String mensajeDe(Object objeto, String campo) {
        return VALIDADOR.validate(objeto).stream()
                .filter(v -> v.getPropertyPath().toString().equals(campo))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(" | "));
    }

    // ---------- caso base ----------

    @Test
    void solicitudCompletaYCorrecta_noTieneErrores() {
        assertThat(VALIDADOR.validate(clienteValido())).isEmpty();
    }

    @Test
    void solicitudVacia_reportaTodosLosCamposObligatorios() {
        Set<String> campos = camposConError(new ClienteRequest());
        assertThat(campos).contains("nombre", "apellidoPaterno", "apellidoMaterno", "fechaNacimiento",
                "curp", "rfc", "sexo", "nacionalidad", "estadoCivil", "correoElectronico",
                "telefonoMovil", "domicilio", "ocupacion", "empresa", "ingresoMensual");
        assertThat(campos).doesNotContain("segundoNombre", "telefonoAlternativo");
    }

    // ---------- nombre / apellidos ----------

    @ParameterizedTest
    @ValueSource(strings = {"Juan#", "Juan3", "Ju@n", "<script>alert(1)</script>", "Juan$%&", "J", "", "   ",
            "Juan  Carlos", "Juan=", "Juan{x}", "Juan[0]", "'; DROP TABLE clientes;--"})
    void nombreInvalido_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setNombre(valor);
        assertThat(camposConError(r)).contains("nombre");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Juan", "José Luis", "María de los Ángeles", "Ñandú", "Müller", "Li"})
    void nombreValido_seAcepta(String valor) {
        ClienteRequest r = clienteValido();
        r.setNombre(valor);
        assertThat(camposConError(r)).doesNotContain("nombre");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Garcia3", "Garc#a", "<b>Garcia</b>", "G"})
    void apellidosInvalidos_seRechazan(String valor) {
        ClienteRequest r = clienteValido();
        r.setApellidoPaterno(valor);
        r.setApellidoMaterno(valor);
        assertThat(camposConError(r)).contains("apellidoPaterno", "apellidoMaterno");
    }

    @Test
    void segundoNombre_esOpcionalPeroSiSeCapturaSeValida() {
        ClienteRequest r = clienteValido();
        r.setSegundoNombre(null);
        assertThat(camposConError(r)).doesNotContain("segundoNombre");
        r.setSegundoNombre("Carlos2");
        assertThat(camposConError(r)).contains("segundoNombre");
    }

    @Test
    void nombreMuyLargo_seRechaza() {
        ClienteRequest r = clienteValido();
        r.setNombre("A".repeat(41));
        assertThat(camposConError(r)).contains("nombre");
    }

    // ---------- correo ----------

    @ParameterizedTest
    @ValueSource(strings = {"usuario@", "usuario", "usuario@@gmail.com", "usuario @gmail.com", "@gmail.com",
            "usuario@gmail", "a..b@gmail.com", "usuario@gmail.c", "us<er>@gmail.com", "usuario@gma il.com"})
    void correoInvalido_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setCorreoElectronico(valor);
        assertThat(camposConError(r)).contains("correoElectronico");
    }

    @ParameterizedTest
    @ValueSource(strings = {"usuario@gmail.com", "USUARIO@GMAIL.COM", "juan.garcia+pruebas@correo.com.mx", "a_b-c@mi-dominio.org"})
    void correoValido_seAceptaYSeNormalizaAMinusculas(String valor) {
        ClienteRequest r = clienteValido();
        r.setCorreoElectronico(valor);
        assertThat(camposConError(r)).doesNotContain("correoElectronico");
        assertThat(r.getCorreoElectronico()).isEqualTo(valor.toLowerCase());
    }

    @Test
    void correoDemasiadoLargo_seRechaza() {
        ClienteRequest r = clienteValido();
        r.setCorreoElectronico("a".repeat(95) + "@x.com");
        assertThat(camposConError(r)).contains("correoElectronico");
    }

    // ---------- teléfonos ----------

    @ParameterizedTest
    @ValueSource(longs = {123456789L, 55123456789L, 0L, -5512345678L, 1234567890L, 1555123456L,
            5555555555L, 2222222222L, 9999999999L, 7777777777L})
    void telefonoInvalido_seRechaza(long valor) {
        ClienteRequest r = clienteValido();
        r.setTelefonoMovil(valor);
        r.setTelefonoAlternativo(valor);
        assertThat(camposConError(r)).contains("telefonoMovil", "telefonoAlternativo");
    }

    @ParameterizedTest
    @ValueSource(longs = {5512345678L, 2212345678L, 8112345679L, 3312345670L})
    void telefonoValido_seAcepta(long valor) {
        ClienteRequest r = clienteValido();
        r.setTelefonoMovil(valor);
        r.setTelefonoAlternativo(valor);
        assertThat(camposConError(r)).doesNotContain("telefonoMovil", "telefonoAlternativo");
    }

    @Test
    void telefonoMensajes_indicanElMotivoEspecifico() {
        ClienteRequest r = clienteValido();
        r.setTelefonoMovil(123L);
        assertThat(mensajeDe(r, "telefonoMovil")).contains("10 dígitos");
        r.setTelefonoMovil(1234567890L);
        assertThat(mensajeDe(r, "telefonoMovil")).contains("lada válida");
        r.setTelefonoMovil(5555555555L);
        assertThat(mensajeDe(r, "telefonoMovil")).contains("secuencia repetida");
    }

    @Test
    void telefonoObligatorio_elAlternativoEsOpcional() {
        ClienteRequest r = clienteValido();
        r.setTelefonoMovil(null);
        r.setTelefonoAlternativo(null);
        assertThat(camposConError(r)).contains("telefonoMovil").doesNotContain("telefonoAlternativo");
    }

    // ---------- CURP / RFC / sexo / estado civil ----------

    @ParameterizedTest
    @ValueSource(strings = {"GALJ900101HDFRPN0", "GALJ900101HDFRPN011", "GALJ900101XDFRPN01", "GALJ90010#HDFRPN01",
            "GALJ900101HDFRPN0 ", "1ALJ900101HDFRPN01", "GAL J900101HDFRPN01", "<script>alert(1)</s"})
    void curpInvalida_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setCurp(valor);
        assertThat(camposConError(r)).contains("curp");
    }

    @Test
    void curp_enMinusculasSeNormalizaAMayusculas() {
        ClienteRequest r = clienteValido();
        r.setCurp("galj900101hdfrpn01");
        assertThat(r.getCurp()).isEqualTo("GALJ900101HDFRPN01");
        assertThat(camposConError(r)).doesNotContain("curp");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GALJ900101A0", "GALJ900101A012", "GALJ90010#A01", "GA1J900101A01", "GALJ9001A1A01", "GALJ900101 01"})
    void rfcInvalido_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setRfc(valor);
        assertThat(camposConError(r)).contains("rfc");
    }

    @Test
    void rfcValido_conAmpersandYEnie_seAcepta() {
        ClienteRequest r = clienteValido();
        r.setRfc("Ñ&LJ900101A01");
        assertThat(camposConError(r)).doesNotContain("rfc");
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "HM", "Hombre", "1", "#"})
    void sexoInvalido_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setSexo(valor);
        assertThat(camposConError(r)).contains("sexo");
    }

    @Test
    void sexo_enMinusculasSeNormaliza() {
        ClienteRequest r = clienteValido();
        r.setSexo("m");
        assertThat(r.getSexo()).isEqualTo("M");
        assertThat(camposConError(r)).doesNotContain("sexo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Soltero", "Otro", "123", "Soltero(a)#", "casado(a)"})
    void estadoCivilFueraDelCatalogo_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setEstadoCivil(valor);
        assertThat(camposConError(r)).contains("estadoCivil");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nacionalidadIdNoPositiva_seRechaza(int valor) {
        ClienteRequest r = clienteValido();
        r.setNacionalidad(List.of(nacionalidad(valor, "Mexicana")));
        assertThat(camposConError(r)).contains("nacionalidad[0].id");
    }

    @Test
    void nacionalidadSinNombre_seRechaza() {
        ClienteRequest r = clienteValido();
        r.setNacionalidad(List.of(nacionalidad(1, "")));
        assertThat(camposConError(r)).contains("nacionalidad[0].nombre");
    }

    @Test
    void nacionalidadConNumerosEnElNombre_seRechaza() {
        ClienteRequest r = clienteValido();
        r.setNacionalidad(List.of(nacionalidad(1, "Mex1cana")));
        assertThat(camposConError(r)).contains("nacionalidad[0].nombre");
    }

    @Test
    void nacionalidadVaciaOConMasDeUnObjeto_seRechaza() {
        ClienteRequest vacia = clienteValido();
        vacia.setNacionalidad(List.of());
        assertThat(camposConError(vacia)).contains("nacionalidad");

        ClienteRequest dos = clienteValido();
        dos.setNacionalidad(List.of(nacionalidad(1, "Mexicana"), nacionalidad(2, "Estadounidense")));
        assertThat(camposConError(dos)).contains("nacionalidad");
    }

    @Test
    void nacionalidadMasculinaOEnMayusculas_pasaLaValidacionDeFormato() {
        ClienteRequest r = clienteValido();
        r.setNacionalidad(List.of(nacionalidad(1, "MEXICANO")));
        assertThat(camposConError(r)).isEmpty();
    }

    private static NacionalidadRequest nacionalidad(int id, String nombre) {
        NacionalidadRequest n = new NacionalidadRequest();
        n.setId(id);
        n.setNombre(nombre);
        return n;
    }

    // ---------- fecha / ingreso / ocupación / empresa ----------

    @Test
    void fechaDeNacimientoFuturaOHoy_seRechaza() {
        ClienteRequest r = clienteValido();
        r.setFechaNacimiento(LocalDate.now().plusDays(1));
        assertThat(camposConError(r)).contains("fechaNacimiento");
        r.setFechaNacimiento(LocalDate.now());
        assertThat(camposConError(r)).contains("fechaNacimiento");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-15000.50", "0.001", "100.555", "12345678.00", "99999999999"})
    void ingresoInvalido_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setIngresoMensual(new BigDecimal(valor));
        assertThat(camposConError(r)).contains("ingresoMensual");
    }

    @ParameterizedTest
    @ValueSource(strings = {"15000", "15000.5", "15000.50", "0.01", "9999999.99"})
    void ingresoValido_seAcepta(String valor) {
        ClienteRequest r = clienteValido();
        r.setIngresoMensual(new BigDecimal(valor));
        assertThat(camposConError(r)).doesNotContain("ingresoMensual");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ingeniero2", "Ing#", "<script>", "ab", "Ingeniero; DROP TABLE x"})
    void ocupacionInvalida_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setOcupacion(valor);
        assertThat(camposConError(r)).contains("ocupacion");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACME <b>", "ACME@", "<script>alert(1)</script>", "A", "ACME; DROP TABLE", "ACME=1", "ACME{}"})
    void empresaInvalida_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.setEmpresa(valor);
        assertThat(camposConError(r)).contains("empresa");
    }

    // ---------- domicilio ----------

    @ParameterizedTest
    @ValueSource(strings = {"0600", "060000", "06A00", "06 00", "0600#", "abcde", "-0600", "06.00"})
    void codigoPostalInvalido_seRechaza(String valor) {
        ClienteRequest r = clienteValido();
        r.getDomicilio().setCodigoPostal(valor);
        assertThat(camposConError(r)).contains("domicilio.codigoPostal");
    }

    @ParameterizedTest
    @ValueSource(strings = {"06000", "00100", "99999"})
    void codigoPostalValido_conceroAlInicio_seAcepta(String valor) {
        ClienteRequest r = clienteValido();
        r.getDomicilio().setCodigoPostal(valor);
        assertThat(camposConError(r)).doesNotContain("domicilio.codigoPostal");
    }

    @Test
    void domicilio_rechazaHtmlScriptsYSqlInjection() {
        ClienteRequest r = clienteValido();
        r.getDomicilio().setCalle("Reforma'; DROP TABLE clientes;--");
        r.getDomicilio().setNumeroExterior("<b>1</b>");
        r.getDomicilio().setNumeroInterior("1 OR 1=1");
        r.getDomicilio().setColonia("<script>alert(1)</script>");
        r.getDomicilio().setMunicipio("Municipio123");
        r.getDomicilio().setEstado("Estado@");
        r.getDomicilio().setPais("Pais#1");
        assertThat(camposConError(r)).contains("domicilio.calle", "domicilio.numeroExterior",
                "domicilio.numeroInterior", "domicilio.colonia", "domicilio.municipio",
                "domicilio.estado", "domicilio.pais");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "45-B", "12B", "S/N"})
    void numeroExteriorValido_seAcepta(String valor) {
        ClienteRequest r = clienteValido();
        r.getDomicilio().setNumeroExterior(valor);
        assertThat(camposConError(r)).doesNotContain("domicilio.numeroExterior");
    }

    // ---------- actualización: mismas reglas ----------

    @Test
    void actualizacion_aplicaLasMismasReglas() {
        ClienteUpdateRequest u = new ClienteUpdateRequest();
        u.setNombre("Juan#");
        u.setTelefonoMovil(5555555555L);
        u.setCorreoElectronico("usuario@@gmail.com");
        u.setIngresoMensual(new BigDecimal("100.555"));
        assertThat(camposConError(u)).contains("id", "nombre", "telefonoMovil", "correoElectronico", "ingresoMensual",
                "apellidoPaterno", "domicilio", "estadoCivil");
    }

    // ---------- consulta (criterios en el body) y baja ----------

    @Test
    void consultaSinFiltros_esValida() {
        assertThat(VALIDADOR.validate(new ClienteConsultaRequest())).isEmpty();
    }

    @Test
    void consultaConFiltrosValidos_esValida() {
        ClienteConsultaRequest c = new ClienteConsultaRequest();
        c.setId(3);
        c.setNombre("Juan");
        c.setCurp("galj900101hdfrpn01");        // se normaliza a mayúsculas
        c.setCorreoElectronico("JUAN@CORREO.COM"); // se normaliza a minúsculas
        c.setNumeroCuenta("1234567890");
        c.setActivo(true);
        assertThat(VALIDADOR.validate(c)).isEmpty();
        assertThat(c.getCurp()).isEqualTo("GALJ900101HDFRPN01");
        assertThat(c.getCorreoElectronico()).isEqualTo("juan@correo.com");
    }

    @Test
    void consultaConFiltrosInvalidos_seRechazan() {
        ClienteConsultaRequest c = new ClienteConsultaRequest();
        c.setId(0);
        c.setNombre("J");
        c.setCurp("123");
        c.setRfc("ABC");
        c.setCorreoElectronico("usuario@@gmail.com");
        c.setNumeroCuenta("12345");
        assertThat(camposConError(c)).contains("id", "nombre", "curp", "rfc", "correoElectronico", "numeroCuenta");
    }

    // ---------- usuario / contraseña ----------

    private static LoginRegistroRequest accesoValido() {
        LoginRegistroRequest r = new LoginRegistroRequest();
        r.setUsuario("juan.perez_01");
        r.setContrasena("Qx7#mLp2vZ");
        return r;
    }

    @Test
    void accesoCorrecto_noTieneErrores() {
        assertThat(VALIDADOR.validate(accesoValido())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "juan perez", "juan<script>", "juan@x", "juán", "juan;drop", "x" + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "   "})
    void usuarioInvalido_seRechaza(String valor) {
        LoginRegistroRequest r = accesoValido();
        r.setUsuario(valor);
        assertThat(camposConError(r)).contains("usuario");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ab1#", "sinmayuscula1!", "SINMINUSCULA1!", "SinNumeros!!!", "SinEspecial123",
            "Con Espacio1!", "Password1!", "Qwerty123#", "  ", "Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!Aa1!"})
    void contrasenaInvalida_seRechaza(String valor) {
        LoginRegistroRequest r = accesoValido();
        r.setContrasena(valor);
        assertThat(camposConError(r)).contains("contrasena");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Qx7#mLp2vZ", "Zebra7#Luna", "Mi_Clave-Segura9", "Ñandú2024*"})
    void contrasenaValida_seAcepta(String valor) {
        LoginRegistroRequest r = accesoValido();
        r.setContrasena(valor);
        assertThat(camposConError(r)).doesNotContain("contrasena");
    }

    @Test
    void contrasena_elMensajeIndicaLaReglaQueFalla() {
        LoginRegistroRequest r = accesoValido();
        r.setContrasena("sinmayuscula1!");
        assertThat(mensajeDe(r, "contrasena")).contains("mayúscula");
        r.setContrasena("SinEspecial123");
        assertThat(mensajeDe(r, "contrasena")).contains("carácter especial");
        r.setContrasena("Password1!");
        assertThat(mensajeDe(r, "contrasena")).contains("demasiado común");
    }

    @Test
    void datoBiometricoNegativo_seRechaza() {
        LoginRegistroRequest r = accesoValido();
        r.setDatoBiometrico(-0.5);
        assertThat(camposConError(r)).contains("datoBiometrico");
        r.setDatoBiometrico(0.87);
        assertThat(camposConError(r)).doesNotContain("datoBiometrico");
    }
}
