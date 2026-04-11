package com.cpt202.app.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    // 必须加这个，否则数据库存的是数字 0,1,2，看不懂
    @Column(length = 20, nullable = false)
    private UserRole role; // ADMIN, CUSTOMER, SPECIALIST

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // TODO: 请使用 IDE 生成所有属性的 Getter 和 Setter 方法
    // (如果你在 pom.xml 里引入了 Lombok，可以直接在类名上方打上 @Data 注解，就不用手写 Getter/Setter 了)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonIgnore
    //防止密码返回给前端的 JSON 数据包里
    //如果不加 @JsonIgnore
    //前端发来一个请求：GET /api/users/1（查询 ID 为 1 的顾客信息）。
    //Spring Boot 后端查到了这个 User 对象，准备把它打包发给前端
    //底层的 Jackson 翻译官开始工作了，它会把所有的 Getter 方法都调用一遍，拼成一个 JSON发给前端
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}