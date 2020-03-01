package bg.autohouse.errors;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public abstract class BaseException extends RuntimeException {

  private static final long serialVersionUID = 3859964215866372293L;
  private HttpStatus status;

  // TODO lombok call to super
  BaseException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }
}
