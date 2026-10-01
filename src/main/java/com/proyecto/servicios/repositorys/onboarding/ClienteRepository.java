package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByCorreoElectronico(String correoElectronico);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreoElectronico(String correoElectronico);

    List<Cliente> findByActivoTrue();

    // "Obtener clientes registrados en un rango de fechas"
    List<Cliente> findByFechaCreacionBetween(LocalDateTime desde, LocalDateTime hasta);
}
