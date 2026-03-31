package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				// SecurityConfig.java の一部
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/images/**").permitAll()
						// 詳細画面は誰でも見れるが、編集・更新・削除・作成はログイン必須にする
						.requestMatchers("/post/new", "/post/edit/**", "/post/update/**", "/post/delete/**",
								"/post/create")
						.authenticated()
						// コメントも投稿・削除などはログイン必須にするのが一般的
						.requestMatchers("/post/*/comment", "/comment/edit/**", "/comment/delete/**").authenticated()
						.anyRequest().authenticated())
				.formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
				.logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());

		return http.build();
	}
}

// ※ AuthenticationManager や Provider の記述は一切不要です！
// なぜこれが必要なの？
// Spring Bootは、依存関係にSpring
// Securityを追加した瞬間、**「全てのページにログインが必要」**という非常に強力なデフォルト設定を適用します。
// これだと開発が進めにくい、あるいは特定のページを公開したいといった場合に、SecurityConfigを書いてデフォルト設定を上書き（カスタマイズ）する必要がある
