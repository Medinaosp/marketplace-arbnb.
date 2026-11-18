package softplasty.marketplace.common.api;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidRequest(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage(),
                        (first, second) -> first
                ));
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "Uno o más campos no cumplen las validaciones.",
                request
        );
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Solicitud inválida",
                "Uno o más valores no cumplen las validaciones.",
                request
        );
        Map<String, String> errors = exception.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, second) -> first
                ));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableRequest(HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Cuerpo de solicitud inválido",
                "El cuerpo no tiene un formato válido.",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataConflict(HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Conflicto de datos",
                "La operación entra en conflicto con datos existentes.",
                request
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode status = exception.getStatusCode();
        return problem(status, "Solicitud rechazada", exception.getReason(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail handleBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Credenciales inválidas",
                "Correo o contraseña incorrectos.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Acceso denegado",
                "La cuenta no tiene permiso para esta operación.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidDomainArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleInvalidDomainState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        return problem(HttpStatus.CONFLICT, "Operación no permitida", exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Error inesperado atendiendo {}", request.getRequestURI(), exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno",
                "No fue posible completar la solicitud.",
                request
        );
    }

    private ProblemDetail problem(
            HttpStatusCode status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
