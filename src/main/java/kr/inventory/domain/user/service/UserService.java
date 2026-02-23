package kr.inventory.domain.user.service;

import kr.inventory.domain.user.controller.dto.UserProfileResponse;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.exception.UserErrorCode;
import kr.inventory.domain.user.exception.UserException;
import kr.inventory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user.getName(), user.getEmail());
    }
}