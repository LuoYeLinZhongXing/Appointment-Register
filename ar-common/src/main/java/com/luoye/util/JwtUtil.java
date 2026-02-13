package com.luoye.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Data
public class JwtUtil implements org.springframework.beans.factory.InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // JWT token 格式验证正则表达式
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]*$");

    @Value("${jwt.key:default_jwt_secret_key_which_should_be_at_least_64_chars_long_for_hs512}")
    private String jwtKey;

    @Value("${jwt.ttl:604800000}") // 默认7天 (7*24*60*60*1000)
    private long jwtTtl = 604800000L;

    private static SecretKey staticSecretKey;
    private static long staticJwtTtl = 604800000L;

    // 在Bean初始化时同步配置值
    @Override
    public void afterPropertiesSet() {
        // 检查密钥长度，如果不够长则使用推荐的方法生成安全密钥
        byte[] keyBytes = jwtKey.getBytes(StandardCharsets.UTF_8);
        logger.info("JWT密钥长度: {} 字节", keyBytes.length);

        if (keyBytes.length < 64) {
            logger.warn("警告: JWT密钥长度不足64字节，将使用随机生成的密钥。建议配置更长的密钥以保证一致性。");
            staticSecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            logger.info("使用随机生成的密钥");
        } else {
            // 使用配置的密钥
            staticSecretKey = Keys.hmacShaKeyFor(keyBytes);
            logger.info("使用配置的密钥");
        }

        staticJwtTtl = this.jwtTtl;
        logger.info("JWT TTL设置为: {} 毫秒", staticJwtTtl);
    }

    /**
     * 生成JWT令牌
     *
     * @param claims 用户信息
     * @return JWT令牌
     */
    public static String createToken(Map<String, Object> claims) {
        try {
            Date expireDate = new Date(System.currentTimeMillis() + staticJwtTtl);

            return Jwts.builder()
                    .setClaims(claims)
                    .setExpiration(expireDate)
                    .signWith(staticSecretKey, SignatureAlgorithm.HS512)
                    .compact();

        } catch (Exception e) {
            logger.error("生成JWT token失败: {}", e.getMessage(), e);
            throw new RuntimeException("Token生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析JWT令牌
     *
     * @param token JWT令牌
     * @return 用户信息
     */
    public static Claims parseToken(String token) {
        try {
            // 验证 token 格式
            if (token == null || token.trim().isEmpty()) {
                return null;
            }

            // 移除可能的空白字符
            token = token.trim();

            // 验证 JWT 格式
            if (!JWT_PATTERN.matcher(token).matches()) {
                return null;
            }

            return Jwts.parserBuilder()
                    .setSigningKey(staticSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (Exception e) {
            logger.error("JWT解析失败: {}", e.getMessage(), e);
            return null;
        }
    }

    // getter方法供测试使用
    public String getJwtKey() {
        return jwtKey;
    }

    public long getJwtTtl() {
        return jwtTtl;
    }

    /**
     * 验证 token 格式是否有效
     * @param token 待验证的 token
     * @return 是否有效
     */
    public static boolean isValidTokenFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        return JWT_PATTERN.matcher(token.trim()).matches();
    }
}
