package cz.cyberrange.platform.guacamole.errors;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = JavaApiError.class, name = "JavaApiError"),
  @JsonSubTypes.Type(value = PythonApiError.class, name = "PythonApiError")
})
@Schema(
    description = "Superclass for classes JavaApiError and PythonApiError",
    oneOf = {JavaApiError.class, PythonApiError.class})
public abstract class ApiSubError {

  public abstract String getMessage();
}
