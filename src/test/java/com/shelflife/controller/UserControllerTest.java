package com.shelflife.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shelflife.dto.UserResponse;
import com.shelflife.dto.UserUpdateRequest;
import com.shelflife.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void getMyProfile_shouldReturnProfile() throws Exception {
        UserResponse response = UserResponse.builder()
                .id("uid-1")
                .email("u1@example.com")
                .displayName("User One")
                .build();

        when(userService.getProfile("uid-1")).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                        .principal(() -> "uid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("uid-1"))
                .andExpect(jsonPath("$.email").value("u1@example.com"));
    }

    @Test
    void updateMyProfile_shouldValidatePayload() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("not-an-email")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .principal(() -> "uid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateMyProfile_shouldReturnUpdatedProfile() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("u1@example.com")
                .displayName("User One")
                .build();

        UserResponse response = UserResponse.builder()
                .id("uid-1")
                .email("u1@example.com")
                .displayName("User One")
                .build();

        when(userService.updateProfile(eq("uid-1"), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/me")
                        .principal(() -> "uid-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("User One"));
    }

    @Test
    void deleteMyAccount_shouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteAccount("uid-1");

                mockMvc.perform(delete("/api/users/me")
                                                .principal(() -> "uid-1"))
                .andExpect(status().isNoContent());
    }
}
