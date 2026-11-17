package io.github.fatec.event;

import io.github.fatec.event.adapter.OperacaoEventoAdapter;
import io.github.fatec.event.dto.OperacaoEvento;
import io.github.fatec.service.RelatorioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// Escuta a fila relatorio.operacoes e atualiza o status de cada doação.
@Component
public class OperacaoEscutador {

    private static final Logger log = LoggerFactory.getLogger(OperacaoEscutador.class);

    private final RelatorioService service;

    public OperacaoEscutador(RelatorioService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${event.operacao.queue}")
    public void receber(OperacaoEvento evento) {
        try {
            boolean aplicada = service.registrarOperacao(OperacaoEventoAdapter.cast(evento));
            if (aplicada) {
                log.info("Operação {} -> {} (tentativa {})",
                        evento.operacaoId(), evento.status(), evento.tentativas());
            } else {
                log.warn("Operação {}: transição para {} não permitida pela máquina de estado; mensagem ignorada",
                        evento.operacaoId(), evento.status());
            }
        } catch (Exception e) {
            log.error("Falha ao registrar operação {}: {}", evento.operacaoId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("Falha ao registrar operação", e);
        }
    }
}