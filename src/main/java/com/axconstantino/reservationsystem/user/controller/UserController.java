package com.axconstantino.reservationsystem.user.controller;

import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RequestMapping("/users/me")
@RestController
@Validated
@RequiredArgsConstructor
public class UserController {
    private final UserService service;
    private final UserMapper userMapper;

}
