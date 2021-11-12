package com.techstore.ecommerce.config.inteceptor;

import com.techstore.ecommerce.object.model.AuthenticationInfo;
import com.techstore.ecommerce.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
@Log4j2
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractTokenFromHeader(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                AuthenticationInfo user = tokenProvider.getAuthInfoFromToken(jwt);

                Collection<? extends GrantedAuthority> authorities =
                        user.getRoleName().equals("CUSTOMER")
                        ? Collections.singleton(
                                new SimpleGrantedAuthority("ROLE_" + user.getRoleName()))
                        : user.getPermissions().stream()
                              .map(SimpleGrantedAuthority::new)
                              .collect(Collectors.toSet());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error(ex);
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        final String AUTHENTICATION_SCHEME = "Bearer ";
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AUTHENTICATION_SCHEME)) {
            return bearerToken.substring(AUTHENTICATION_SCHEME.length());
        }
        return null;
    }
}
