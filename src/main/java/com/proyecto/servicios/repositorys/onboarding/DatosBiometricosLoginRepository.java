package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.DatosBiometricosLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatosBiometricosLoginRepository extends JpaRepository<DatosBiometricosLogin, Integer> {

    Optional<DatosBiometricosLogin> findByLoginId(Integer loginId);

    // busca por el HASH del token (determinístico), nunca por el
    // valor cifrado con AES (que cambia cada vez que se cifra, aunque
    // sea el mismo token de entrada).
    Optional<DatosBiometricosLogin> findByTokenSesionHash(String tokenSesionHash);
}