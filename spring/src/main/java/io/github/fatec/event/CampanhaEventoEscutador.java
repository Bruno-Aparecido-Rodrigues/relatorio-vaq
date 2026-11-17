package io.github.fatec.event;

import io.github.fatec.event.dto.CampanhaEvento;
import io.github.fatec.event.retry.RetryMensagem;
import io.github.fatec.service.RelatorioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Escuta a fila relatorio.campanha-eventos e registra os eventos do CRUD no painel.
// Se falhar, o RetryMensagem decide: nova tentativa (dead-letter) ou parking-lot.
@Component
public class CampanhaEventoEscutador {

    private static final Logger log = LoggerFactory.getLogger(CampanhaEventoEscutador.class);

    private final RelatorioService service;
    private final RetryMensagem retry;
    private final String queue;
    private final String parkingLot;

    public CampanhaEventoEscutador(RelatorioService service,
                                   RetryMensagem retry,
                                   @Value("${event.campanha.queue}") String queue,
                                   @Value("${event.campanha.parking-lot}") String parkingLot) {
        this.service = service;
        this.retry = retry;
        this.queue = queue;
        this.parkingLot = parkingLot;
    }

    @RabbitListener(queues = "${event.campanha.queue}")
    public void receber(CampanhaEvento evento, Message message) {
        try {
            service.registrarEventoCampanha(evento.acao(), evento.campanhaId(), evento.titulo(),
                    evento.criadorNome(), evento.data());
            log.info("Evento de campanha registrado: {} ({})", evento.campanhaId(), evento.acao());
        } catch (Exception e) {
            log.error("Falha ao registrar evento da campanha {}: {}", evento.campanhaId(), e.getMessage());
            retry.tratarFalha(message, queue, parkingLot, e);
        }
    }
}
