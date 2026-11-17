package io.github.fatec.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// Filas do Relatório com retry
// Cada fila tem a sua dead-letter (espera o TTL) e a sua parking-lot (desistiu):
// As novas tentativas voltam pela relatorio.retry (direct), e não pela vaquinha.eventos,
// para não chegarem de novo nas filas dos outros serviços.
@Configuration
public class RabbitMQConfig {

    @Value("${event.exchange}")
    private String exchange;

    @Value("${event.retry.exchange}")
    private String retryExchangeNome;

    @Value("${event.retry.delay}")
    private Duration retryDelay;

    @Value("${event.operacao.queue}")
    private String operacaoQueue;

    @Value("${event.operacao.dead-letter}")
    private String operacaoDeadLetter;

    @Value("${event.operacao.parking-lot}")
    private String operacaoParkingLot;

    @Value("${event.operacao.routing-key}")
    private String operacaoRoutingKey;

    @Value("${event.campanha.queue}")
    private String campanhaQueue;

    @Value("${event.campanha.dead-letter}")
    private String campanhaDeadLetter;

    @Value("${event.campanha.parking-lot}")
    private String campanhaParkingLot;

    @Value("${event.campanha.routing-key}")
    private String campanhaRoutingKey;

    @Bean
    public TopicExchange vaquinhaExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(retryExchangeNome);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // ---------- Operações de doação

    @Bean
    public Queue operacoesQueue() {
        return QueueBuilder.durable(operacaoQueue)
                .deadLetterExchange(retryExchangeNome)
                .deadLetterRoutingKey(operacaoDeadLetter)
                .build();
    }

    @Bean
    public Queue operacoesDeadLetter() {
        return QueueBuilder.durable(operacaoDeadLetter)
                .ttl((int) retryDelay.toMillis())
                .deadLetterExchange(retryExchangeNome)
                .deadLetterRoutingKey(operacaoQueue)
                .build();
    }

    @Bean
    public Queue operacoesParkingLot() {
        return QueueBuilder.durable(operacaoParkingLot).build();
    }

    @Bean
    public Binding operacoesBinding() {
        return BindingBuilder.bind(operacoesQueue()).to(vaquinhaExchange()).with(operacaoRoutingKey);
    }

    @Bean
    public Binding operacoesRetryBinding() {
        return BindingBuilder.bind(operacoesQueue()).to(retryExchange()).with(operacaoQueue);
    }

    @Bean
    public Binding operacoesDeadLetterBinding() {
        return BindingBuilder.bind(operacoesDeadLetter()).to(retryExchange()).with(operacaoDeadLetter);
    }

    @Bean
    public Binding operacoesParkingLotBinding() {
        return BindingBuilder.bind(operacoesParkingLot()).to(retryExchange()).with(operacaoParkingLot);
    }

    // ---------- Eventos de campanha

    @Bean
    public Queue campanhaEventosQueue() {
        return QueueBuilder.durable(campanhaQueue)
                .deadLetterExchange(retryExchangeNome)
                .deadLetterRoutingKey(campanhaDeadLetter)
                .build();
    }

    @Bean
    public Queue campanhaEventosDeadLetter() {
        return QueueBuilder.durable(campanhaDeadLetter)
                .ttl((int) retryDelay.toMillis())
                .deadLetterExchange(retryExchangeNome)
                .deadLetterRoutingKey(campanhaQueue)
                .build();
    }

    @Bean
    public Queue campanhaEventosParkingLot() {
        return QueueBuilder.durable(campanhaParkingLot).build();
    }

    @Bean
    public Binding campanhaEventosBinding() {
        return BindingBuilder.bind(campanhaEventosQueue()).to(vaquinhaExchange()).with(campanhaRoutingKey);
    }

    @Bean
    public Binding campanhaEventosRetryBinding() {
        return BindingBuilder.bind(campanhaEventosQueue()).to(retryExchange()).with(campanhaQueue);
    }

    @Bean
    public Binding campanhaEventosDeadLetterBinding() {
        return BindingBuilder.bind(campanhaEventosDeadLetter()).to(retryExchange()).with(campanhaDeadLetter);
    }

    @Bean
    public Binding campanhaEventosParkingLotBinding() {
        return BindingBuilder.bind(campanhaEventosParkingLot()).to(retryExchange()).with(campanhaParkingLot);
    }
}