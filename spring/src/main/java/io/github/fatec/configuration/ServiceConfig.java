package io.github.fatec.configuration;

import io.github.fatec.repository.OperacaoRepository;
import io.github.fatec.service.RelatorioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Os services agora ficam no módulo domain, em Java puro (sem @Service).
// Esta classe diz ao Spring como criar o RelatorioService, entregando o OperacaoRepositoryImpl
// para a interface OperacaoRepository que ele pede.
@Configuration
public class ServiceConfig {

    @Bean
    public RelatorioService relatorioService(OperacaoRepository repository) {
        return new RelatorioService(repository);
    }
}
