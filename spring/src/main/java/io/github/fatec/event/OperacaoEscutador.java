package io.github.fatec.event;

import io.github.fatec.event.adapter.OperacaoEventoAdapter;
import io.github.fatec.event.dto.OperacaoEvento;
import io.github.fatec.event.retry.RetryMensagem;
import io.github.fatec.service.RelatorioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Escuta a fila relatorio.operacoes e atualiza o status de cada doação.
// Se falhar, o RetryMensagem decide: nova tentativa (dead-letter) ou parking-lot.
@Component
public class OperacaoEscutador {

    private static final Logger log = LoggerFactory.getLogger(OperacaoEscutador.class);

    private final RelatorioService service;
    private final RetryMensagem retry;
    private final String queue;
    private final String parkingLot;

    public OperacaoEscutador(RelatorioService service,
                             RetryMensagem retry,
                             @Value("${event.operacao.queue}") String queue,
                             @Value("${event.operacao.parking-lot}") String parkingLot) {
        this.service = service;
        this.retry = retry;
        this.queue = queue;
        this.parkingLot = parkingLot;
    }

    @RabbitListener(queues = "${event.operacao.queue}")
    public void receber(OperacaoEvento evento, Message message) {
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
            retry.tratarFalha(message, queue, parkingLot, e);
        }
    }
}