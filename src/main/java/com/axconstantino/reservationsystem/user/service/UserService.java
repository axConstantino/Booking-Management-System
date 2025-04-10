package com.axconstantino.reservationsystem.user.service;

import com.axconstantino.reservationsystem.common.utils.BaseCRUDService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.validation.PhoneValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService extends BaseCRUDService<User, UserDTO, UUID, UserRepository, UserMapper> {
    private final PhoneValidator phoneValidator;

    public UserService(UserRepository repository, UserMapper mapper, PhoneValidator phoneValidator) {
        super(repository, mapper);
        this.phoneValidator = phoneValidator;
    }

    public User addPhone(UUID userId, String phone) {
        User user = repository.getExisted(userId, User.class);

        if (user.getPhone() != null) {
            throw new RuntimeException("You already have a phone number");
        }

        user.setPhone(phoneValidator.formatToE164(phone));
        return repository.save(user);
    }
}
