package io.github.ifenixi.supportflow.notification;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import java.net.http.HttpClient;
import java.time.Duration;
import io.github.ifenixi.supportflow.common.DomainException;
import org.springframework.http.HttpStatus;
@Component
public class NotificationClient {
 private final RestClient client;private final RetryTemplate retry;
 public NotificationClient(@Value("${app.worker-url}") String url,@Value("${app.internal-token}") String token){
  if(token.length()<32)throw new IllegalStateException("INTERNAL_TOKEN requires 32+ characters");
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());factory.setReadTimeout(Duration.ofSeconds(3));
  client=RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeader("X-Internal-Token",token).build();
  retry=RetryTemplate.builder().maxAttempts(2).fixedBackoff(100).retryOn(org.springframework.web.client.ResourceAccessException.class).retryOn(org.springframework.web.client.HttpServerErrorException.class).build();
 }
 public String inbox(long clientId,int page,int size){
  try{return retry.execute(context->client.get().uri("/internal/notifications?clientId={id}&page={page}&size={size}",clientId,page,size).retrieve().body(String.class));}
  catch(Exception e){throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE,"Notifications temporarily unavailable; ticket operations remain available");}
 }
}
