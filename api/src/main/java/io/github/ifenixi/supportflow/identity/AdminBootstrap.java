package io.github.ifenixi.supportflow.identity;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
@Configuration
public class AdminBootstrap {
 @Bean ApplicationRunner seed(AccountRepository accounts,PasswordEncoder encoder,PlatformTransactionManager manager,@Value("${app.admin-password}") String password){
  return args->{
   if(password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalStateException("Admin password must have 12+ characters and <=72 UTF-8 bytes");
   new TransactionTemplate(manager).executeWithoutResult(tx->{if(accounts.findByUsername("admin").isEmpty())accounts.save(new Account("admin",encoder.encode(password),Role.ADMIN));});
  };
 }
}
