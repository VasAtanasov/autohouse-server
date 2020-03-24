package bg.autohouse.web.models.request;

import bg.autohouse.validation.user.ValidSeller;
import bg.autohouse.web.validations.annotations.MatchingFieldsConstraint;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@MatchingFieldsConstraint(fields = {"password", "confirmPassword"})
public class UserRegisterRequest {
  @NotBlank(message = "Username is required.")
  private String username;

  @NotBlank(message = "Password field is required")
  private String password;

  private String confirmPassword;

  private String phoneNumber;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid.")
  private String email;

  @NotBlank(message = "First name is required field.")
  private String firstName;

  @NotBlank(message = "Last name is required field.")
  private String lastName;

  @ValidSeller private String seller;
}