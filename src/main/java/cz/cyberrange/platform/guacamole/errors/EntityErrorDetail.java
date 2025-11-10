package cz.cyberrange.platform.guacamole.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityErrorDetail {
  @Schema(description = "Class of the entity.", example = "IDMGroup")
  private String entity;

  @Schema(description = "Identifier of the entity.", example = "id")
  private String identifier;

  @Schema(description = "Value of the identifier.", example = "1")
  private Object identifierValue;

  @Schema(
      description = "Detailed message of the exception",
      example = "Group with same name already exists.")
  private String reason;

  public EntityErrorDetail() {}

  public EntityErrorDetail(@NotBlank String reason) {
    this.reason = reason;
  }

  public EntityErrorDetail(@NotNull Class<?> entityClass, @NotBlank String reason) {
    this(reason);
    this.entity = entityClass.getSimpleName();
  }

  public EntityErrorDetail(
      @NotNull Class<?> entityClass,
      @NotBlank String identifier,
      @NotNull Class<?> identifierClass,
      @NotNull Object identifierValue,
      @NotBlank String reason) {
    this(entityClass, reason);
    this.identifier = identifier;
    this.identifierValue = identifierClass.cast(identifierValue);
  }

  public EntityErrorDetail(
      @NotNull Class<?> entityClass,
      @NotBlank String identifier,
      @NotNull Class<?> identifierClass,
      @NotNull Object identifierValue) {
    this.entity = entityClass.getSimpleName();
    this.identifier = identifier;
    this.identifierValue = identifierClass.cast(identifierValue);
  }

  public void setEntity(@NotBlank String entity) {
    this.entity = entity;
  }

  public void setIdentifier(@NotBlank String identifier) {
    this.identifier = identifier;
  }

  public void setIdentifierValue(@NotNull Object identifierValue) {
    this.identifierValue = identifierValue;
  }

  public void setReason(@NotBlank String reason) {
    this.reason = reason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EntityErrorDetail entity = (EntityErrorDetail) o;
    return Objects.equals(getEntity(), entity.getEntity())
        && Objects.equals(getIdentifier(), entity.getIdentifier())
        && Objects.equals(getIdentifierValue(), entity.getIdentifierValue())
        && Objects.equals(getReason(), entity.getReason());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEntity(), getIdentifier(), getIdentifierValue(), getReason());
  }

  @Override
  public String toString() {
    return "EntityErrorDetail{"
        + "entity='"
        + entity
        + '\''
        + ", identifier='"
        + identifier
        + '\''
        + ", identifierValue="
        + identifierValue
        + ", reason='"
        + reason
        + '\''
        + '}';
  }
}
