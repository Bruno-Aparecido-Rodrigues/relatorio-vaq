package io.github.fatec.service;

import io.github.fatec.entity.Operacao;
import io.github.fatec.entity.enumerable.StatusOperacao;
import io.github.fatec.repository.OperacaoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Monta o histórico do Painel: uma linha por operação, atualizada a cada mensagem.
// Usa a máquina de estado do StatusOperacao: uma mensagem que pediria uma transição
// inválida (ex.: PROCESSANDO atrasado depois de CONCLUIDA) é ignorada, e um status final nunca muda.
// É Java puro; os escutadores (módulo spring) convertem as mensagens e chamam este service.
public class RelatorioService {

    private final OperacaoRepository repository;

    public RelatorioService(OperacaoRepository repository) {
        this.repository = repository;
    }

    // Status de uma doação. "recebida" é o estado informado pela mensagem; o inicio dela é a
    // data da mensagem. Devolve false quando a mensagem foi ignorada pela máquina de estado.
    public boolean registrarOperacao(Operacao recebida) {
        if (recebida.operacaoId() == null || recebida.status() == null) {
            throw new IllegalArgumentException("Evento de operação incompleto: " + recebida);
        }

        Instant data = recebida.inicio() != null ? recebida.inicio() : Instant.now();
        Optional<Operacao> existente = repository.findById(recebida.operacaoId());

        if (existente.isPresent() && !aceita(existente.get().status(), recebida.status())) {
            return false;
        }

        Instant inicio = existente.map(Operacao::inicio).orElse(data);
        int tentativas = Math.max(
                existente.map(Operacao::tentativas).orElse(0),
                recebida.tentativas() != null ? recebida.tentativas() : 0);
        Instant fim = recebida.status().finalizado() ? data : null;

        repository.save(new Operacao(
                recebida.operacaoId(),
                recebida.tipo(),
                recebida.status(),
                recebida.campanhaId(),
                recebida.campanhaTitulo(),
                recebida.usuarioNome(),
                recebida.valor(),
                tentativas,
                recebida.motivo(),
                inicio,
                fim
        ));
        return true;
    }

    // Um status final nunca muda. Um status final novo é sempre aceito, mesmo que uma mensagem
    // intermediária tenha se perdido (assim a linha não fica travada em PENDENTE).
    // Os status intermediários só entram se a máquina de estado permitir a transição.
    private boolean aceita(StatusOperacao atual, StatusOperacao novo) {
        if (atual.finalizado()) {
            return false;
        }
        return novo.finalizado() || atual.podeIrPara(novo);
    }

    // Evento de vaquinha (criada, atualizada, excluída) vira uma linha já concluída.
    public void registrarEventoCampanha(String acao, String campanhaId, String titulo, String autorNome, Instant data) {
        if (acao == null) {
            throw new IllegalArgumentException("Evento de campanha sem ação: " + campanhaId);
        }
        Instant quando = data != null ? data : Instant.now();

        repository.save(new Operacao(
                UUID.randomUUID().toString(),
                "CAMPANHA_" + acao,
                StatusOperacao.CONCLUIDA,
                campanhaId,
                titulo,
                autorNome,
                null,
                1,
                null,
                quando,
                quando
        ));
    }

    public List<Operacao> listarOperacoes() {
        return repository.findRecentes();
    }
}
