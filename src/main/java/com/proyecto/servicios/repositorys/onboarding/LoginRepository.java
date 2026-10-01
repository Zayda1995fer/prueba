package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Login, Integer> {

    Optional<Login> findByClienteId(Integer clienteId);

    Optional<Login> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);
}
