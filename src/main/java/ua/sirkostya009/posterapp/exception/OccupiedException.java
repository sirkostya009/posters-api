package ua.sirkostya009.posterapp.exception;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@StandardException
@ResponseStatus(code = HttpStatus.CONFLICT)
public class OccupiedException extends RuntimeException {
}
