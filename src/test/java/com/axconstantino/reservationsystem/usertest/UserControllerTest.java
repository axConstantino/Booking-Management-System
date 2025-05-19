package com.axconstantino.reservationsystem.usertest;

import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.common.exception.RestExceptionHandler;
import com.axconstantino.reservationsystem.user.controller.UserController;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.dto.ChangePasswordRequest;
import com.axconstantino.reservationsystem.user.dto.ResetPasswordRequest;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.user.service.UserService;
import com.axconstantino.reservationsystem.validation.PhoneCustomValidator;
import com.axconstantino.reservationsystem.validation.PhoneValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private UserService userService;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserController userController;

    private final String email = "user@example.com";
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        PhoneValidator phoneValidator = new PhoneValidator();

        PhoneCustomValidator phoneCustomValidator = new PhoneCustomValidator(phoneValidator);

        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(new DefaultListableBeanFactory()) {
            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                if (key == PhoneCustomValidator.class) {
                    return (T) phoneCustomValidator;
                }
                return super.getInstance(key);
            }
        });
        validatorFactory.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setValidator(validatorFactory)
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        authentication = new UsernamePasswordAuthenticationToken(email, null);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getCurrentUser_ShouldReturn200WithDto() throws Exception {
        User user = User.builder().email(email).build();
        UserDTO dto = new UserDTO(); dto.setEmail(email);
        given(userService.getUserByEmail(email)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(dto);

        mockMvc.perform(get("/users/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(dto)));
    }

    @Test
    void getCurrentUser_ShouldReturn404WhenNotFound() throws Exception {
        given(userService.getUserByEmail(email)).willThrow(new NotFoundException("not found"));

        mockMvc.perform(get("/users/me").principal(authentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCurrentUser_ShouldReturn200WithDto() throws Exception {
        UserDTO reqDto = new UserDTO(); reqDto.setName("New");
        User user = User.builder().email(email).name("New").build();
        UserDTO respDto = new UserDTO(); respDto.setName("New");
        given(userService.updateUserBasicInfo(email, reqDto)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(respDto);

        mockMvc.perform(put("/users/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(respDto)));
    }

    @Test
    void addPhone_ShouldReturn200WithDto() throws Exception {
        UserDTO reqDto = new UserDTO();
        reqDto.setPhone("5512345678");

        User user = User.builder().email(email).phone("+525512345678").build();
        UserDTO respDto = new UserDTO();
        respDto.setPhone("+525512345678");

        given(userService.addPhone(email, reqDto.getPhone())).willReturn(user);
        given(userMapper.toDto(user)).willReturn(respDto);

        mockMvc.perform(post("/users/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(respDto)));
    }

    @Test
    void changePassword_ShouldReturn204() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("OldPassword123@");
        req.setNewPassword("NewPassword123@");

        mockMvc.perform(post("/users/me/change-password")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        then(userService).should().changePassword(email, req);
    }

    @Test
    void requestPasswordReset_ShouldReturn204() throws Exception {
        String resetEmail = "foo@bar.com";
        mockMvc.perform(post("/users/me/reset-password")
                        .param("email", resetEmail))
                .andExpect(status().isNoContent());

        then(userService).should().initiatePasswordReset(resetEmail);
    }

    @Test
    void confirmPasswordReset_ShouldReturn204() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("tkn"); req.setNewPassword("NewPassword123@");

        mockMvc.perform(post("/users/me/reset-password/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        then(userService).should().completePasswordReset(req);
    }
}


