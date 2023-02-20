package ua.sirkostya009.posterapp.exception;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.function.Supplier;

@StandardException
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    public static Supplier<NotFoundException> supplier(String message) {
        return () -> new NotFoundException(message);
    }
}
