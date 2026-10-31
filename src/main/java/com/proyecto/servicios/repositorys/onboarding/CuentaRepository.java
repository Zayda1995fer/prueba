package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    Optional<Cuenta> findByClienteId(Integer clienteId);

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByClienteIdAndEstatus(Integer clienteId, String estatus);

    // Con bloqueo de escritura: los pagos simultáneos de una misma cuenta se atienden uno por uno,
    // así dos solicitudes no pueden gastar el mismo saldo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> buscarParaPago(@Param("numeroCuenta") String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.id = :id")
    Optional<Cuenta> buscarPorIdParaActualizar(@Param("id") Integer id);
}
