package guru.springframework.juniemvc.exceptions;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(Integer id) {
        super("Customer not found: id=" + id);
    }
}
