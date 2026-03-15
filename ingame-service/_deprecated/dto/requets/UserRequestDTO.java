package com.arimar.gwent.ingame_service.dto.requets;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "User request DTO containing user registration details")
public class UserRequestDTO {

    @Schema(description = "User ID (auto-generated)", example = "1")
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Schema(description = "Username of the user", example = "JohnDoe123")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "User password (must be at least 8 characters)", example = "P@ssw0rd123")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User email address", example = "johndoe@example.com")
    private String email;

    @NotBlank(message = "Game ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,15}$", message = "Game ID must be 3-15 characters, only letters, numbers, _ or -")
    @Schema(description = "Game ID associated with the user", example = "gamePlayer_99")
    private String gameId;

    @NotBlank(message = "User Code is required")
    @Pattern(regexp = "^\\d{4}$", message = "User Code must be exactly 4 digits")
    @Schema(description = "User code for unique identification (4-digit number)", example = "1234")
    private String userCode;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }
}
