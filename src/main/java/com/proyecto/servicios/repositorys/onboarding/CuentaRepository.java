package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    Optional<Cuenta> findByClienteId(Integer clienteId);

    boolean existsByNumeroCuenta(String numeroCuenta);

    // "Consultar cuentas activas"
    List<Cuenta> findByEstatus(String estatus);
}