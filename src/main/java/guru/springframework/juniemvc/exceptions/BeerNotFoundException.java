package guru.springframework.juniemvc.exceptions;

public class BeerNotFoundException extends RuntimeException {
    public BeerNotFoundException(Integer beerId) {
        super("Beer not found: id=" + beerId);
    }
}
