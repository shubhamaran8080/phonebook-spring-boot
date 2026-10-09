package com.phonebook.security;

import com.phonebook.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads application users from the database for the JWT filter. */
@Service
public class SecurityUserService implements UserDetailsService {

  private final UserRepository userRepository;

  public SecurityUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userRepository
        .findByUsername(username)
        .map(
            user ->
                org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                    .password(user.getHashedPassword())
                    .authorities("ROLE_USER")
                    .build())
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }
}
