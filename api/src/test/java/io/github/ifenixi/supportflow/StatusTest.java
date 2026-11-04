package io.github.ifenixi.supportflow;
import io.github.ifenixi.supportflow.ticket.Status;
import io.github.ifenixi.supportflow.common.DomainException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
class StatusTest {
 @ParameterizedTest @EnumSource(Status.class) void validatesEveryTransition(Status from){
  for(Status to:Status.values()){
   boolean allowed=from==Status.NEW&&to==Status.IN_PROGRESS || from==Status.IN_PROGRESS&&to==Status.RESOLVED || from==Status.RESOLVED&&(to==Status.CLOSED||to==Status.IN_PROGRESS);
   if(allowed)assertDoesNotThrow(()->from.transitionTo(to));else assertThrows(DomainException.class,()->from.transitionTo(to));
  }
 }
}
