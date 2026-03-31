package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.service.UserService;

@Controller
public class RegistrationController {

	@Autowired
	private UserService userService;

	@GetMapping("/register")
	public String registerForm() {
		return "register";
	}

	@PostMapping("/register")
	public String registerUser(@RequestParam String username, @RequestParam String password,
			RedirectAttributes redirectAttributes) { // 引数に正しく渡せるようになります
		try {
			userService.register(username, password);
			return "redirect:/login?register_success";
		} catch (RuntimeException e) {
			if ("USER_ALREADY_EXISTS".equals(e.getMessage())) {
				redirectAttributes.addFlashAttribute("errorMessage", "入力されたユーザーは既に存在しています。\n違うユーザー名で登録を試みてください");
				return "redirect:/register";
			}
			throw e;
		}
	}
}
