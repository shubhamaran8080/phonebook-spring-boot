package com.phonebook;

import static org.assertj.core.api.Assertions.assertThat;

import com.phonebook.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  @Test
  void createsAndValidatesTokens() {
    JwtTokenProvider provider = new JwtTokenProvider("a-test-secret", 720);
    String token = provider.createToken("alice");

    assertThat(provider.validateToken(token)).isTrue();
    assertThat(provider.getUsername(token)).isEqualTo("alice");
  }

  @Test
  void rejectsTamperedTokens() {
    JwtTokenProvider provider = new JwtTokenProvider("a-test-secret", 720);
    String token = provider.createToken("alice");
    String tampered =
        token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

    assertThat(provider.validateToken(tampered)).isFalse();
  }

  @Test
  void rejectsGarbageTokens() {
    JwtTokenProvider provider = new JwtTokenProvider("a-test-secret", 720);

    assertThat(provider.validateToken("not-a-jwt")).isFalse();
    assertThat(provider.validateToken("")).isFalse();
    assertThat(provider.validateToken(null)).isFalse();
  }

  @Test
  void rejectsExpiredTokens() {
    // Negative expiration -> tokens are invalid immediately.
    JwtTokenProvider provider = new JwtTokenProvider("a-test-secret", -1);
    String token = provider.createToken("alice");

    assertThat(provider.validateToken(token)).isFalse();
  }

  @Test
  void supportsShortSecrets() {
    // Secrets of any length must work (the key is derived via SHA-256),
    // matching the Python backend which signs with the raw secret.
    JwtTokenProvider provider = new JwtTokenProvider("short", 720);
    String token = provider.createToken("bob");

    assertThat(provider.validateToken(token)).isTrue();
    assertThat(provider.getUsername(token)).isEqualTo("bob");
  }

  @Test
  void supportsEmptySecretWithEphemeralKey() {
    JwtTokenProvider provider = new JwtTokenProvider("", 720);
    String token = provider.createToken("carol");

    assertThat(provider.validateToken(token)).isTrue();
    assertThat(provider.getUsername(token)).isEqualTo("carol");
  }
}
