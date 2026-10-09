package com.phonebook.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Creates and verifies HS256 JWT access tokens.
 *
 * <p>The signing key is derived (SHA-256) from the configured secret so
 * that secrets of any length work. When no secret is configured an
 * ephemeral random key is generated, which means tokens do not survive a
 * restart - the same behavior as the Python backend.
 */
@Component
public class JwtTokenProvider {

  private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

  private final SecretKey signingKey;
  private final long expirationMillis;

  public JwtTokenProvider(
      @Value("${jwt.secret-key:}") String secret,
      @Value("${jwt.expiration-minutes:720}") long expirationMinutes) {
    this.expirationMillis = expirationMinutes * 60 * 1000L;
    if (secret != null && !secret.isBlank()) {
      this.signingKey = Keys.hmacShaKeyFor(sha256(secret.getBytes(StandardCharsets.UTF_8)));
    } else {
      byte[] random = new byte[32];
      new SecureRandom().nextBytes(random);
      this.signingKey = Keys.hmacShaKeyFor(random);
      log.warn(
          "No JWT secret configured (set JWT_SECRET_KEY). Using an ephemeral random key - "
              + "all tokens will be invalid after a restart.");
    }
  }

  private static byte[] sha256(byte[] input) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(input);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  /** Creates a signed access token for the given username ("sub" claim). */
  public String createToken(String username) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMillis);
    return Jwts.builder()
        .subject(username)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(signingKey)
        .compact();
  }

  /** Extracts the username ("sub" claim) from a valid token. */
  public String getUsername(String token) {
    return parse(token).getSubject();
  }

  public boolean validateToken(String token) {
    try {
      parse(token);
      return true;
    } catch (ExpiredJwtException e) {
      return false;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }
}
