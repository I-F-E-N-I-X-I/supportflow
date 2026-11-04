package io.github.ifenixi.supportflow.ticket;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
public interface CommentRepository extends JpaRepository<TicketComment,Long> {
 @Query("select c from TicketComment c where c.ticket.id=:ticketId and (:staff=true or c.internal=false)")
 Page<TicketComment> visible(long ticketId,boolean staff,Pageable pageable);
}
