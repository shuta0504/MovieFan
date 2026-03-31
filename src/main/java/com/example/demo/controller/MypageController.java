package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.PostRepository;

@Controller
public class MypageController {

	@Autowired
	private PostRepository postRepository;

	@GetMapping("/mypage")
	public String index(@AuthenticationPrincipal User loginUser, Model model) {
		// ログインチェック
		if (loginUser == null) {
			return "redirect:/login";
		}

		List<Post> myPosts = postRepository.findByUserIdWithLikesOrderByCreatedAtDesc(loginUser.getId());

		// 画面に渡す
		model.addAttribute("username", loginUser.getUsername());
		model.addAttribute("posts", myPosts);
		return "mypage";
	}
}
