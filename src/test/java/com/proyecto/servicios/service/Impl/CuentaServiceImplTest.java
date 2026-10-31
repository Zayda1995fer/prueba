package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    private CuentaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new CuentaServiceImpl(cuentaRepository);
    }

    private Cuenta cuentaDelCliente5() {
        Cuenta c = new Cuenta();
        c.setId(9);
        c.setClienteId(5);
        c.setNumeroCuenta("1234567890");
        c.setSaldo(new BigDecimal("2000.00"));
        c.setEstatus(Cuenta.ESTATUS_ACTIVA);
        return c;
    }

    @Test
    void consultarSaldo_delPropioCliente_loDevuelve() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDelCliente5()));
        assertThat(servicio.consultarSaldo("1234567890", 5)).isEqualByComparingTo("2000.00");
    }

    @Test
    void consultarSaldo_deCuentaAjena_respondeIgualQueSiNoExistiera() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDelCliente5()));

        assertThatThrownBy(() -> servicio.consultarSaldo("1234567890", 6))
                .isInstanceOfSatisfying(NegocioException.class,
                        ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.CUENTA_NO_ENCONTRADA));
    }

    @Test
    void consultarPorNumeroCuenta_deCuentaAjena_seRechaza() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuentaDelCliente5()));

        assertThatThrownBy(() -> servicio.consultarPorNumeroCuenta("1234567890", 6))
                .isInstanceOfSatisfying(NegocioException.class,
                        ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.CUENTA_NO_ENCONTRADA));
    }

    @Test
    void consultarActivas_soloBuscaLasDelClienteAutenticado() {
        when(cuentaRepository.findByClienteIdAndEstatus(5, Cuenta.ESTATUS_ACTIVA))
                .thenReturn(List.of(cuentaDelCliente5()));

        assertThat(servicio.consultarActivas(5)).hasSize(1);
    }
}
