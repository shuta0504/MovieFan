package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

@Entity
@Table(name = "mt_users")
@Data
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // DBのカラム名が user_name の場合
    @Column(name = "user_name", nullable = false, unique = true)
    private String userName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    //実行前に現在の日時を取得する
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- UserDetailsインターフェースの実装 ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ロールを権限として返す
    	//Spring Securityは権限を GrantedAuthority という型で管理します。
    	//ここでは、DBに保存されている role 文字列（例: "ROLE_USER", "ROLE_ADMIN"）を、Springが理解できる形式に変換して渡しています
        return List.of(new SimpleGrantedAuthority(this.role));
    }

    @Override
    public String getUsername() {
        // Spring Securityが「ユーザー名」として使うフィールドを返す
        return this.userName;
    }

    @Override
    public boolean isAccountNonExpired() { return true; } //有効期限が切れていないか
    @Override
    public boolean isAccountNonLocked() { return true; }//アカウントがロックされていないか
    @Override
    public boolean isCredentialsNonExpired() { return true; }//パスワード自体の期限が切れていないか
    @Override
    public boolean isEnabled() { return true; }//アカウント自体が有効か
}