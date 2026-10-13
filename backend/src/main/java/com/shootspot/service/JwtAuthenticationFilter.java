package com.shootspot.service;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shootspot.entity.User;
import com.shootspot.repository.UserRepository;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logProtectedPostAuthenticationRejection(request, "missing bearer token");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorizationHeader.substring(7);
            var claims = jwtService.parseToken(token);
            String email = claims.getSubject();
            User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user == null) {
                logProtectedPostAuthenticationRejection(request, "user not found");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
            Object tokenVersionClaim = claims.get("ver");
            int tokenVersion = tokenVersionClaim instanceof Number number
                    ? number.intValue()
                    : 0;
            if (tokenVersion != user.getTokenVersion()) {
                logProtectedPostAuthenticationRejection(request, "token-version mismatch");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }

            UserBuilder userBuilder = org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPasswordHash())
                    .roles(user.getRole());
            UserDetails userDetails = userBuilder.build();
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException exception) {
            logProtectedPostAuthenticationRejection(request, "expired token");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        } catch (JwtException | IllegalArgumentException exception) {
            logProtectedPostAuthenticationRejection(request, "invalid token");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void logProtectedPostAuthenticationRejection(HttpServletRequest request, String reason) {
        String path = request.getServletPath();
        boolean bookingRequest = "/api/bookings".equals(path);
        boolean locationReviewRequest = "POST".equals(request.getMethod())
                && path.matches("/api/locations/\\d+/reviews");
        if ("POST".equals(request.getMethod()) && (bookingRequest || locationReviewRequest)) {
            LOGGER.warn(
                    "Authentication rejected for {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    reason);
        }
    }
}
