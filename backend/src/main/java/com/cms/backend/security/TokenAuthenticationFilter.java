package com.cms.backend.security;

import com.cms.backend.auth.AuthSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final AuthSessionRepository sessions;

    public TokenAuthenticationFilter(AuthSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring("Bearer ".length());
            sessions.findById(token)
                    .filter(session -> session.getAccount().isActive())
                    .ifPresent(session -> {
                        var account = session.getAccount();
                        var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
                        if (account.isSalesMember()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_SALES_MEMBER"));
                        }
                        if (account.isManager()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
                        }
                        var authentication = new UsernamePasswordAuthenticationToken(
                                new CurrentAccount(account), token, List.copyOf(authorities));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }
}
