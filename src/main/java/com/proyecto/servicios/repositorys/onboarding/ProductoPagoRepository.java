package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.ProductoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoPagoRepository extends JpaRepository<ProductoPago, Integer> {
}
