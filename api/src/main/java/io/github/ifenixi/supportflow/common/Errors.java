package io.github.ifenixi.supportflow.common;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.*;
@RestControllerAdvice
public class Errors {
 private static final Logger log=LoggerFactory.getLogger(Errors.class);
 @ExceptionHandler(DomainException.class) ProblemDetail domain(DomainException e) {return ProblemDetail.forStatusAndDetail(e.status(),e.getMessage());}
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,IllegalArgumentException.class})
 ProblemDetail invalid(Exception e) {return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Invalid request parameters or body");}
 @ExceptionHandler({DataIntegrityViolationException.class,ObjectOptimisticLockingFailureException.class})
 ProblemDetail conflict(Exception e) {return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Conflicting or concurrently modified data");}
 @ExceptionHandler(AccessDeniedException.class) ProblemDetail denied(Exception e) {return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,"Insufficient permissions");}
 @ExceptionHandler(Exception.class) ProblemDetail unexpected(Exception e) {
  String id=java.util.UUID.randomUUID().toString();
  // Do not log request payloads, SQL parameters, passwords or exception messages.
  log.error("Unhandled error reference={} type={}",id,e.getClass().getSimpleName());
  return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,"Internal error; reference="+id);
 }
}
