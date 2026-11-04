package io.github.ifenixi.supportflow.worker;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;

@Configuration
public class BrokerConfig {
  public static final String EXCHANGE = "supportflow.events";
  public static final String QUEUE = "supportflow.notifications";

  @Bean
  Declarables topology() {
    var exchange = new DirectExchange(EXCHANGE, true, false);
    var dead = new DirectExchange("supportflow.dead", true, false);
    var queue =
        QueueBuilder.durable(QUEUE)
            .deadLetterExchange("supportflow.dead")
            .deadLetterRoutingKey("failed")
            .build();
    var dlq = QueueBuilder.durable("supportflow.notifications.dlq").build();
    return new Declarables(
        exchange,
        dead,
        queue,
        dlq,
        BindingBuilder.bind(queue).to(exchange).with("ticket.status"),
        BindingBuilder.bind(dlq).to(dead).with("failed"));
  }
}
