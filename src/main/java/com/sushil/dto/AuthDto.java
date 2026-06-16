package com.sushil.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sushil.entity.Permission;
import com.sushil.entity.User;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

public final class AuthDto {

    private AuthDto() {}

    /**
     * Login request — exactly one of username / email / mobile must be provided.
     * username + password  → username-based
     * email    + password  → email-based
     * mobile               → mobile OTP-based (no password)
     */
    public record LoginRequest(
            String username,
            @Email(message = "Invalid email format") String email,
            @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid mobile number") String mobile,
            String password
    ) {}

    /**
     * Submitted to /verify-otp after a login response with otpRequired=true.
     * loginToken is the short-lived token returned by the login endpoint.
     */
    public record OtpVerifyRequest(
            @NotBlank(message = "Login token is required")                               String loginToken,
            @NotBlank(message = "OTP is required") @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits") String otp
    ) {}

    /**
     * Registration — username is auto-generated from firstName + lastName.
     */
    public record RegisterRequest(
            @NotBlank(message = "First name is required")  @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")  String firstName,
            @NotBlank(message = "Last name is required")   @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")   String lastName,
            @NotBlank(message = "Email is required")       @Email(message = "Invalid email format") @Size(max = 100, message = "Email must not exceed 100 characters") String email,
            @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Invalid mobile number") String mobile,
            @NotBlank(message = "Password is required")    @Size(min = 6, max = 100, message = "Password must be at least 6 characters") String password,
            User.Role role
    ) {
        public RegisterRequest {
            if (role == null) role = User.Role.USER;
        }
    }

    /**
     * Response for POST /login and POST /verify-otp.
     *
     * otpRequired=true  → only message + loginToken present. username/role/permissions/tokens are null (excluded by NON_NULL).
     * otpRequired=false → only message + tokens + username + role + permissions present. loginToken is null (excluded).
     */
    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LoginResponse {
        boolean otpRequired;
        String  message;
        String  loginToken;
        String  accessToken;
        String  refreshToken;
    }

    @Value
    @Builder
    public static class UserDto {
        Long        id;
        String      username;
        String      firstName;
        String      lastName;
        String      email;
        String      mobile;
        String      profilePicUrl;
        String      role;
        Set<String> permissions;
        boolean     enabled;

        public static UserDto from(User user) {
            Set<String> perms = new java.util.LinkedHashSet<>();
            user.effectivePermissions().forEach(p -> perms.add(p.name()));
            return UserDto.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .mobile(user.getMobile())
                    .profilePicUrl(user.getProfilePicUrl())
                    .role(user.getRole().name())
                    .permissions(perms)
                    .enabled(user.isEnabled())
                    .build();
        }
    }

    public record UpdateUserRequest(
            @Email(message = "Invalid email format") String email,
            User.Role role,
            Boolean enabled,
            Set<Permission> extraPermissions
    ) {}
}
