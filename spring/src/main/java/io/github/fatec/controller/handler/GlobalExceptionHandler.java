package io.github.fatec.controller.handler;

import io.github.fatec.controller.RelatorioController;
import io.github.fatec.controller.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduz as exceções em códigos HTTP com { "mensagem": "..." }.
// Antes isso ficava dentro do RelatorioController; agora o controller só cuida do endpoint.
// O assignableTypes limita estes tratamentos ao mesmo controller de antes (rota inexistente continua 404/405).
@RestControllerAdvice(assignableTypes = RelatorioController.class)
public class GlobalExceptionHandler {

    // Sem X-User-Roles = a requisição não passou pelo gateway estando logada
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> semUsuario(MissingRequestHeaderException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Usuário não identificado"));
    }
}
