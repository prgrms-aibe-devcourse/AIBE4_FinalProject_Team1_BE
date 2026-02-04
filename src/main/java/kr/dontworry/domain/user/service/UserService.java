package kr.dontworry.domain.user.service;

import kr.dontworry.domain.user.controller.dto.UserProfileResponse;
import kr.dontworry.domain.user.entity.User;
import kr.dontworry.domain.user.exception.UserErrorCode;
import kr.dontworry.domain.user.exception.UserException;
import kr.dontworry.domain.user.repository.UserRepository;
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

        return new UserProfileResponse(user.getName(), user.getEmail());
    }
}