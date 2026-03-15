package com.arimar.gwent.ingame_service.service;

import com.arimar.gwent.common.user.dto.request.UserRequestDTO;
import com.arimar.gwent.common.user.dto.response.UserResponseDTO;
import com.arimar.gwent.ingame_service.mapper.UserMapper;
import com.arimar.gwent.ingame_service.model.User;
import com.arimar.gwent.ingame_service.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserMapper userMapper;
    private UserRepository userRepository;

    public UserService(UserMapper userMapper, UserRepository userRepository) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponseDTO save(UserRequestDTO requestDTO) {
        User user = userMapper.toEntityFromRequest(requestDTO);

        try {
            throw new Exception("prueba de error");
            //user = userRepository.save(user);
            //return userMapper.toResponseFromEntity(user);
        } catch (Exception e) {
            throw new RuntimeException("Error while saving user: " + e.getMessage());
        }
    }
}
