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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        // 登录、注册等接口直接放行
        if (path.contains("/login") || path.contains("/register") || path.contains("/verify-code")) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerAuth = request.getHeader("Authorization");

        try {
            if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
                String token = headerAuth.substring(7);

                // 将 validate 逻辑拆解，以便捕获token不合法的具体原因
                if (jwtUtils.validateJwtToken(token)) {
                    String username = jwtUtils.getUserNameFromJwtToken(token);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        System.out.println("DEBUG: 用户 [" + username + "] 已登录，权限为: " + authentication.getAuthorities());
                    }
                } else {
                    // 如果 validateJwtToken 返回 false，说明 Token 签名不对或格式错误
                    sendErrorResponse(response, "无效的 Token (Invalid Signature or Format)");
                    return;
                }
            } else if (headerAuth == null) {
                // 如果你希望没有 Token 时也报错，可以取消下面这行的注释
                 sendErrorResponse(response, "缺少 Authorization Header (Missing Token)");
                 return;
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            sendErrorResponse(response, "Token 已过期 (Token Expired)");
            return;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            sendErrorResponse(response, "Token 格式畸形 (Malformed Token)");
            return;
        } catch (Exception e) {
            sendErrorResponse(response, "Token 解析异常: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
    /**
     * 💥 新增辅助方法：直接向前端输出 JSON 错误信息
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");
        String json = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        response.getWriter().write(json);
    }
}
