package com.cpt202.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, @Lazy CustomUserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // When encountering these static resources and all HTML pages, the JWT filter passes them directly without checking the Token
        return path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/favicon.ico") ||
                path.equals("/") ||
                path.endsWith(".html");
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        // Interfaces such as login and registration are allowed directly, as well as forgot-password
        if (path.contains("/login") || path.contains("/register") || path.contains("/verify-code") || path.contains("/forgot-password")) { // 👈 forgot-password added here
            filterChain.doFilter(request, response);
            return;
        }

        String headerAuth = request.getHeader("Authorization");

        try {
            if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                String token = headerAuth.substring(7);

                // Deconstruct validate logic to capture the specific reason for an invalid token
                if (jwtUtils.validateJwtToken(token)) {
                    String username = jwtUtils.getUserNameFromJwtToken(token);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        System.out.println("DEBUG: User [" + username + "] logged in, authorities: " + authentication.getAuthorities());
                    }
                } else {
                    // If validateJwtToken returns false, it means the Token signature is wrong or the format is incorrect
                    sendErrorResponse(response, "Invalid Token (Invalid Signature or Format)");
                    return;
                }
            } else if (headerAuth == null) {
                // If you want to report an error when there is no Token, you can uncomment the line below
                sendErrorResponse(response, "Missing Authorization Header (Missing Token)");
                return;
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendErrorResponse(response, "Token Expired");
            return;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            sendErrorResponse(response, "Malformed Token");
            return;
        } catch (Exception e) {
            sendErrorResponse(response, "Token parsing exception: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
    /**
     * 💥 New auxiliary method: directly output JSON error messages to the frontend
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");
        String json = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        response.getWriter().write(json);
    }
}