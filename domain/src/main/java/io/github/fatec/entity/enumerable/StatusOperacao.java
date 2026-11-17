package io.github.fatec.entity.enumerable;

import java.util.EnumSet;
import java.util.Set;

// Máquina de estado de uma operação de doação (mesmas regras do serviço de Doação).
// O Relatório usa para ignorar mensagens que pediriam uma transição inválida.
//
//   PENDENTE -> PROCESSANDO -> CONCLUIDA | RECUSADA | ERRO
//                    |   ^
//                    v   |
//                  CONFLITO  (perdeu a disputa: tenta de novo, ou desiste com ERRO)
//
// CONCLUIDA, RECUSADA e ERRO são estados finais: depois deles nada muda.
// Diferente do StatusCampanha, aqui repetir o mesmo status não conta como transição válida.
public enum StatusOperacao {
    PENDENTE,
    PROCESSANDO,
    CONFLITO,
    CONCLUIDA,
    RECUSADA,
    ERRO;

    public Set<StatusOperacao> proximosPermitidos() {
        return switch (this) {
            case PENDENTE -> EnumSet.of(PROCESSANDO, ERRO);
            case PROCESSANDO -> EnumSet.of(CONFLITO, CONCLUIDA, RECUSADA, ERRO);
            case CONFLITO -> EnumSet.of(PROCESSANDO, ERRO);
            case CONCLUIDA, RECUSADA, ERRO -> EnumSet.noneOf(StatusOperacao.class);
        };
    }

    public boolean podeIrPara(StatusOperacao destino) {
        return proximosPermitidos().contains(destino);
    }

    // Faz a transição; se ela não for permitida, lança IllegalStateException.
    public StatusOperacao irPara(StatusOperacao destino) {
        if (!podeIrPara(destino)) {
            throw new IllegalStateException("Transição de status inválida: " + this + " -> " + destino);
        }
        return destino;
    }

    public boolean finalizado() {
        return proximosPermitidos().isEmpty();
    }
}
