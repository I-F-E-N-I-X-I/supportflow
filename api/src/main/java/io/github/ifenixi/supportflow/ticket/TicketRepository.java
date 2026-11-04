package io.github.ifenixi.supportflow.ticket;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TicketRepository
    extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
  Optional<Ticket> findByClientIdAndRequestKey(long clientId, String requestKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Ticket t where t.id=:id")
  Optional<Ticket> lockById(@Param("id") long id);
}
