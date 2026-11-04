package io.github.ifenixi.supportflow.identity;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface AccountRepository extends JpaRepository<Account,Long> {
 Optional<Account> findByUsername(String username);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from Account a where a.id=:id")
 Optional<Account> lockById(@Param("id") long id);
}
