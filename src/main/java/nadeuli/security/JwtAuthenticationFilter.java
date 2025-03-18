package nadeuli.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nadeuli.entity.User;
import nadeuli.repository.UserRepository;
import nadeuli.service.JwtTokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null && jwtTokenService.validateToken(token)) {
            String email = jwtTokenService.extractEmail(token);
            Optional<User> userOptional = userRepository.findByUserEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                UserDetails userDetails = new CustomUserDetails(user);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 추출
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null) {
            log.debug("[extractToken] Authorization 헤더 존재: {}", bearerToken);
            if (bearerToken.startsWith("Bearer ")) {
                String token = bearerToken.substring(7);
                log.debug("[extractToken] Authorization 헤더에서 토큰 추출: {}", token);
                return token;
            } else {
                log.debug("[extractToken] Authorization 헤더가 Bearer 형식이 아님");
            }
        } else {
            log.debug("[extractToken] Authorization 헤더 없음");
        }

        // 2. accessToken 쿠키에서 추출
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                log.debug("[extractToken] 쿠키: {} = {}", cookie.getName(), cookie.getValue());
                if ("accessToken".equals(cookie.getName())) {
                    log.debug("[extractToken] accessToken 쿠키에서 토큰 추출: {}", cookie.getValue());
                    return cookie.getValue();
                }
            }
        } else {
            log.debug("[extractToken] 요청에 쿠키 없음");
        }

        log.debug("[extractToken] 토큰을 찾지 못함");
        return null;
    }

}
