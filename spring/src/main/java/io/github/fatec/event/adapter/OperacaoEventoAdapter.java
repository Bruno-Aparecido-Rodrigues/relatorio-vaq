package io.github.fatec.event.adapter;

import io.github.fatec.entity.Operacao;
import io.github.fatec.event.dto.OperacaoEvento;

// Converte a mensagem recebida da Doação (OperacaoEvento) no estado informado de uma Operacao.
// O inicio recebe a data da mensagem; quem decide o inicio e o fim de verdade é o RelatorioService.
public class OperacaoEventoAdapter {

    private OperacaoEventoAdapter() {
    }

    public static Operacao cast(OperacaoEvento evento) {
        return new Operacao(
                evento.operacaoId(),
                evento.tipo(),
                evento.status(),
                evento.campanhaId(),
                evento.campanhaTitulo(),
                evento.usuarioNome(),
                evento.valor(),
                evento.tentativas(),
                evento.motivo(),
                evento.data(),
                null
        );
    }
}
