package guru.springframework.juniemvc.web;

import guru.springframework.juniemvc.exceptions.BeerNotFoundException;
import guru.springframework.juniemvc.exceptions.CustomerNotFoundException;
import guru.springframework.juniemvc.exceptions.InvalidOrderException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BeerNotFoundException.class)
    ProblemDetail handleBeerNotFound(BeerNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Beer Not Found");
        pd.setType(URI.create("https://httpstatuses.com/404"));
        return pd;
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    ProblemDetail handleCustomerNotFound(CustomerNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Customer Not Found");
        pd.setType(URI.create("https://httpstatuses.com/404"));
        return pd;
    }

    @ExceptionHandler(InvalidOrderException.class)
    ProblemDetail handleInvalidOrder(InvalidOrderException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid Order");
        pd.setType(URI.create("https://httpstatuses.com/400"));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Failed");
        pd.setDetail("Request validation failed");
        pd.setType(URI.create("https://httpstatuses.com/400"));
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        pd.setProperty("errors", errors);
        return pd;
    }
}
