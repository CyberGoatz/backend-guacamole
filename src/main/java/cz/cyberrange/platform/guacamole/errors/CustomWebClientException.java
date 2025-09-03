package cz.cyberrange.platform.guacamole.errors;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The type Rest template exception. */
public class CustomWebClientException extends RuntimeException {

  @Getter private final HttpStatusCode statusCode;

  @Getter private final ApiSubError apiSubError;

  /**
   * Instantiates a new Rest template exception.
   *
   * @param apiSubError detailed information about error.
   */
  public CustomWebClientException(HttpStatusCode httpStatus, ApiSubError apiSubError) {
    super();
    this.apiSubError = apiSubError;
    this.statusCode = httpStatus;
  }

  @Getter
  @ResponseStatus(
      reason = "Error when calling external service API. See the console for the detail reason.")
  public static class MicroserviceApiException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final ApiSubError apiSubError;

    private MicroserviceApiException(
        String message, HttpStatusCode statusCode, ApiSubError apiSubError) {
      super(message + " " + apiSubError.getMessage());
      this.statusCode = statusCode;
      this.apiSubError = apiSubError;
    }

    public MicroserviceApiException(HttpStatusCode statusCode, ApiSubError apiSubError) {
      this("Error when calling external microservice.", statusCode, apiSubError);
    }

    public MicroserviceApiException(
        String message, CustomWebClientException customWebClientException) {
      this(
          message,
          customWebClientException.getStatusCode(),
          customWebClientException.getApiSubError());
    }
  }
}
