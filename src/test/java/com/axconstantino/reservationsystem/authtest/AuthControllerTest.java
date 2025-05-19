package com.axconstantino.reservationsystem.authtest;

import com.axconstantino.reservationsystem.auth.dto.AuthRequest;
import com.axconstantino.reservationsystem.auth.dto.RegisterRequest;
import com.axconstantino.reservationsystem.auth.dto.TokenResponse;
import com.axconstantino.reservationsystem.auth.service.AuthService;
import com.axconstantino.reservationsystem.auth.service.JwtService;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldReturnTokenResponse() throws Exception {
        RegisterRequest request = new RegisterRequest("John", "john@example.com", "Password1@");
        TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token");

        Mockito.when(authService.register(any(RegisterRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void authenticate_shouldReturnTokenResponse() throws Exception {
        AuthRequest request = new AuthRequest("john@example.com", "Password1@");
        TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token");

        Mockito.when(authService.authenticate(any(AuthRequest.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refreshToken_shouldReturnNewTokens() throws Exception {
        String refreshToken = "Bearer refresh-token";
        TokenResponse tokenResponse = new TokenResponse("new-access-token", "new-refresh-token");

        Mockito.when(authService.refreshToken(refreshToken)).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/refresh-token")
                        .header("Authorization", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void logout_shouldReturnOk() throws Exception {
        String authHeader = "Bearer token";

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk());

        Mockito.verify(authService).logout(authHeader);
    }
}