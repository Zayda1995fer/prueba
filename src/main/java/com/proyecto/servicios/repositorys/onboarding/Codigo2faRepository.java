package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Codigo2fa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Codigo2faRepository extends JpaRepository<Codigo2fa, Integer> {

    // Con bloqueo: dos validaciones simultáneas no pueden consumir el mismo código
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Codigo2fa> findFirstByClienteIdAndCuentaIdAndUsadoFalseOrderByFechaCreacionDesc(Integer clienteId,
                                                                                            Integer cuentaId);

    List<Codigo2fa> findByClienteIdAndCuentaIdAndUsadoFalse(Integer clienteId, Integer cuentaId);
}
