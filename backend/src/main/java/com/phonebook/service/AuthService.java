package com.phonebook.service;

import com.phonebook.dto.LoginRequest;
import com.phonebook.dto.LoginResponse;
import com.phonebook.dto.UserResponse;
import com.phonebook.entity.User;
import com.phonebook.exception.ApiException;
import com.phonebook.repository.UserRepository;
import com.phonebook.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider tokenProvider) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenProvider = tokenProvider;
  }

  /**
   * Authenticates with a username OR an email and returns a JWT access
   * token. Never reveals whether the username or the password was wrong.
   */
  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    String identifier = request.getUsername();
    User user = userRepository.findByUsernameOrEmail(identifier, identifier).orElse(null);
    if (user == null
        || !passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
      throw new ApiException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Incorrect username/email or password");
    }
    String token = tokenProvider.createToken(user.getUsername());
    return new LoginResponse(token, "bearer", UserResponse.from(user));
  }

  @Transactional(readOnly = true)
  public User findByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () -> new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Not authenticated"));
  }
}
