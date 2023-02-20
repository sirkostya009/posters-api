package ua.sirkostya009.posterapp.exception;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@StandardException
@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class EmailNotSentException extends RuntimeException {
}
