package com.axconstantino.reservationsystem.user.service;

import com.axconstantino.reservationsystem.common.exception.NotFoundException;
import com.axconstantino.reservationsystem.common.utils.BaseCRUDService;
import com.axconstantino.reservationsystem.user.database.model.User;
import com.axconstantino.reservationsystem.user.database.repository.UserRepository;
import com.axconstantino.reservationsystem.user.dto.UserDTO;
import com.axconstantino.reservationsystem.user.mapper.UserMapper;
import com.axconstantino.reservationsystem.validation.PhoneValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService extends BaseCRUDService<User, UserDTO, UUID, UserRepository, UserMapper> {
    private final PhoneValidator phoneValidator;

    public UserService(UserRepository repository, UserMapper mapper, PhoneValidator phoneValidator) {
        super(repository, mapper);
        this.phoneValidator = phoneValidator;
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));
    }

    @Transactional
    public User updateUserBasicInfo(String email, UserDTO updateRequest) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));

        mapper.updateFromDTO(user, updateRequest);

        if (updateRequest.getPhone() != null) {
            user.setPhone(phoneValidator.formatToE164(updateRequest.getPhone()));
        }

        return repository.save(user);
    }

    public User addPhone(String email, String phone) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(email));

        if (user.getPhone() != null) {
            throw new RuntimeException("You already have a phone number");
        }

        user.setPhone(phoneValidator.formatToE164(phone));
        return repository.save(user);
    }
}