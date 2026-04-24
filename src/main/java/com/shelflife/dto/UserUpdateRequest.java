package com.shelflife.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Email(message = "Email must be valid")
    @Size(max = 320, message = "Email must be at most 320 characters")
    private String email;

    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;
}
