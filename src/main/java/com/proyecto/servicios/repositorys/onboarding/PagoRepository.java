package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findByIdSolicitud(String idSolicitud);

    /** Bloquea el pago para que solo una transacción a la vez pueda resolverlo (respuesta vs. conciliación). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    Optional<Pago> buscarParaActualizar(@Param("id") Integer id);

    /** Pagos iguales (misma cuenta, producto, destino y monto) hechos o en proceso recientemente. */
    @Query("select count(p) from Pago p where p.cuentaId = :cuentaId and p.idProducto = :idProducto "
            + "and p.destino = :destino and p.monto = :monto and p.estatus in :estatus "
            + "and p.fechaCreacion >= :desde")
    long contarSimilares(@Param("cuentaId") Integer cuentaId,
                         @Param("idProducto") Integer idProducto,
                         @Param("destino") String destino,
                         @Param("monto") BigDecimal monto,
                         @Param("estatus") Collection<String> estatus,
                         @Param("desde") LocalDateTime desde);

    /** Pagos sin resultado final que ya llevan suficiente tiempo para consultarse con el proveedor. */
    @Query("select p from Pago p where p.estatus in :estatus and p.fechaActualizacion <= :limite "
            + "and p.intentosConciliacion < :maxIntentos order by p.fechaActualizacion")
    List<Pago> buscarPorConciliar(@Param("estatus") Collection<String> estatus,
                                  @Param("limite") LocalDateTime limite,
                                  @Param("maxIntentos") Integer maxIntentos,
                                  Pageable pageable);
}
