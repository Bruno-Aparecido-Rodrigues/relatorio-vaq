package io.github.fatec.event.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

//   falhou e ainda tem tentativa -> rejeita, a mensagem vai para a dead-letter, espera o TTL e volta;
//   esgotou as tentativas        -> manda para a parking-lot (com o erro no header) e não volta mais.
@Component
public class RetryMensagem {

    private static final Logger log = LoggerFactory.getLogger(RetryMensagem.class);

    private final RabbitTemplate rabbitTemplate;
    private final String retryExchange;
    private final int maxRetries;

    public RetryMensagem(RabbitTemplate rabbitTemplate,
                         @Value("${event.retry.exchange}") String retryExchange,
                         @Value("${event.retry.max-retries}") int maxRetries) {
        this.rabbitTemplate = rabbitTemplate;
        this.retryExchange = retryExchange;
        this.maxRetries = maxRetries;
    }

    // Chamado no catch do escutador.
    public void tratarFalha(Message message, String fila, String parkingLot, Exception ex) {
        long retries = retries(message, fila);
        if (retries < maxRetries) {
            log.warn("Falha na fila '{}' (retry {}/{}): {}", fila, retries + 1, maxRetries, ex.getMessage());
            throw new AmqpRejectAndDontRequeueException(ex);
        }
        log.error("Esgotou {} retries na fila '{}'; enviando para '{}'", maxRetries, fila, parkingLot);
        message.getMessageProperties().setHeader("x-exception-message", ex.getMessage());
        rabbitTemplate.send(retryExchange, parkingLot, message);
    }

    // Quantas vezes a mensagem já foi rejeitada nesta fila (header x-death que o RabbitMQ preenche).
    public long retries(Message message, String fila) {
        List<Map<String, ?>> xDeath = message.getMessageProperties().getXDeathHeader();
        if (xDeath == null) {
            return 0;
        }
        return xDeath.stream()
                .filter(death -> fila.equals(death.get("queue")) && "rejected".equals(String.valueOf(death.get("reason"))))
                .map(death -> death.get("count"))
                .filter(Number.class::isInstance)
                .mapToLong(count -> ((Number) count).longValue())
                .findFirst()
                .orElse(0);
    }
}
