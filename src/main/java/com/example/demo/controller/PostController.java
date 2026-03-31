package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;

@Controller
public class PostController {

	@Autowired
	private PostRepository postRepository;

	@GetMapping("/")
	public String index(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
		List<Post> posts;

		if (keyword != null && !keyword.isEmpty()) {
			// 検索キーワードがある場合
			posts = postRepository.searchByKeywordWithLikes(keyword);
			if (posts.isEmpty()) {
				model.addAttribute("searchMessage", "「" + keyword + "」に一致する投稿は見つかりませんでした。");
			}
		} else {
			// キーワードがない場合は全件取得
			posts = postRepository.findAllWithLikesOrderByCreatedAtDesc();
		}

		model.addAttribute("posts", posts);
		model.addAttribute("keyword", keyword); // 検索窓に値を残すために渡す
		return "index";
	}

	@Value("${upload.path}")
	private String uploadPath;

	@GetMapping("/post/new")
	public String newPost(Model model) {
		model.addAttribute("post", new Post());
		return "post-form";
	}

//投稿詳細に飛ぶ機能
	@GetMapping("/post/{id}")
	public String postDetail(@PathVariable("id") Integer id, Model model) {
		Post post = postRepository.findById(id).orElseThrow();
		model.addAttribute("post", post);
		model.addAttribute("comment", new Comment());
		return "post-detail";
	}

	// 編集機能①
	// PostController.java

	@GetMapping("/post/edit/{id}") // 編集ボタンのリンク先URLに合わせてください
	public String editPost(@PathVariable("id") Integer id, Model model) {
		// 1. DBから編集対象のデータを取得
		Post post = postRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Invalid post Id:" + id));

		// 2. 重要：HTML側の th:object="${post}" という名前に合わせてデータを渡す
		model.addAttribute("post", post);

		// 3. 編集画面のHTML名を返す
		return "post-edit";
	}

	// 編集機能②
	@PostMapping("/post/update/{id}")
	public String updatePost(@PathVariable("id") Integer id, @ModelAttribute Post postData,
			@RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
			@RequestParam(value = "videoFile", required = false) MultipartFile videoFile) throws IOException {
		Post post = postRepository.findById(id).orElseThrow();

		// 内容を書き換える
		post.setMovieTitle(postData.getMovieTitle());
		post.setContent(postData.getContent());
		post.setYoutubeVideoId(postData.getYoutubeVideoId());

		// 画像が新しく選択されていたら差し替え
		if (imageFile != null && !imageFile.isEmpty()) {
			String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
			Path filePath = Paths.get(uploadPath, fileName);
			Files.copy(imageFile.getInputStream(), filePath);
			post.setImageUrl("/images/" + fileName);
		}

		// 動画が新しく選択されていたら差し替え
		if (videoFile != null && !videoFile.isEmpty()) {
			String fileName = System.currentTimeMillis() + "_" + videoFile.getOriginalFilename();
			Path filePath = Paths.get(uploadPath, fileName);
			Files.copy(videoFile.getInputStream(), filePath);
			post.setVideoUrl("/images/" + fileName);
		}

		// 保存（IDが既存のものなので、JPAが自動でUPDATE文を発行します）→save後になるためredirect
		postRepository.save(post);
		return "redirect:/post/" + id;
	};

	// 削除機能
	@PostMapping("/post/delete/{id}")
	public String deletePost(@PathVariable("id") Integer id, @AuthenticationPrincipal User loginUser) {
		// 削除前にデータを取得（ファイル削除が必要な場合のため）
		Post post = postRepository.findById(id).orElseThrow();

		// (オプション) もしMacのフォルダ内の画像ファイル自体も消したい場合はここに追加
		// if (post.getImageUrl() != null) {
		// String fileName = post.getImageUrl().replace("/images/", "");
		// new File(uploadPath + "/" + fileName).delete();
		// }
		// ログインユーザーのIDと、投稿の所有者IDを比較
		if (loginUser == null || !post.getUserId().equals(loginUser.getId())) {
			// 本人でない場合は削除させずに詳細画面へ戻す（エラーメッセージ等を付けても良い）
			return "redirect:/post/" + id;
		}

		// データベースから削除
		postRepository.deleteById(id);

		return "redirect:/"; // 削除後は一覧画面に戻る →deleteの後のためredirect
	}

	@Autowired
	private CommentRepository commentRepository;

	// コメントを追加する
	@PostMapping("/post/{id}/comment")
	public String addComment(@PathVariable("id") Integer id, @Valid @ModelAttribute("comment") Comment comment, // 1.
																												// オブジェクトで受け取る
			BindingResult bindingResult, Model model) {
		// バリデーションエラーがある場合
		if (bindingResult.hasErrors()) {
			Post post = postRepository.findById(id).orElseThrow();
			model.addAttribute("post", post);
			return "post-detail";
		}
		// ★重要：IDをnullにセットする
		// これにより、既存データの更新ではなく「完全な新規登録」として扱われます
		comment.setId(null);
		// 2. 投稿データを取得
		Post post = postRepository.findById(id).orElseThrow();
		// 3. commentオブジェクトに必要な情報をセット
		// ※ content は @ModelAttribute によって既に comment に入っているので setContent は不要です
		comment.setPost(post);// コメントにポストを紐付けている
		comment.setCreatedAt(LocalDateTime.now());// コメントに今の時間を紐づけている

		// 保存
		commentRepository.save(comment);

		return "redirect:/post/" + id;
	}

	// コメントを編集する画面へ遷移
	@GetMapping("/comment/edit/{id}")
	public String editComment(@PathVariable("id") Integer id, Model model) {
		Comment comment = commentRepository.findById(id).orElseThrow();

		model.addAttribute("comment", comment);
		model.addAttribute("postId", comment.getPost().getId());
		return "comment-edit";
	}

	// コメントを更新する

	// コメントを削除する
	@Transactional
	@PostMapping("/comment/delete/{id}") // パスが post-detail.html と一致しているか確認
	public String deleteComment(@PathVariable("id") Integer id) {
		System.out.println("削除処理を開始します: ID=" + id);

		// 1. 先に削除対象を検索して、戻り先のPost IDを確保する
		Comment comment = commentRepository.findById(id).orElseThrow(() -> new RuntimeException("Comment not found"));
		Integer postId = comment.getPost().getId();

		// 2. カスタムクエリで直接削除を実行
		commentRepository.deleteByIdCustom(id);

		System.out.println("削除処理が完了しました");

		return "redirect:/post/" + postId;
	}

	// 投稿を新規作成する
	@PostMapping("/post/create")
	public String createPost(@AuthenticationPrincipal User loginUser, @Valid @ModelAttribute Post post,
			BindingResult bindingResult, // エラー結果を受け取る引数を追加
			@RequestParam("imageFile") MultipartFile imageFile, @RequestParam("videoFile") MultipartFile videoFile,
			RedirectAttributes redirectAttributes, Model model // エラー時に画面へ戻すために必要
	) throws IOException {
		// スペースを詰めたタイトルで重複チェック
		String trimmedTitle = post.getMovieTitle().replace(" ", "").replace("　", "");
		List<Post> existingPosts = postRepository.findByMovieTitleIgnoringSpaces(trimmedTitle);

		if (!existingPosts.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "そちらの投稿は既に存在しています。\nこちらをご覧ください。");
			redirectAttributes.addFlashAttribute("duplicatePost", existingPosts.get(0));
			return "redirect:/post/new";
		}

		// 1. バリデーションエラー（500文字超えなど）がある場合の処理
		if (bindingResult.hasErrors()) {
			// 入力画面（post-form.html）に戻す
			return "post-form";
		}

		if (loginUser != null) {
			post.setUserId(loginUser.getId());
		} else {
			return "redirect:/login";
		}

		// 画像の保存処理 (post.getMovieTitle() などは既にセットされています)
		if (!imageFile.isEmpty()) {
			String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
			Path filePath = Paths.get(uploadPath, fileName);
			Files.copy(imageFile.getInputStream(), filePath);
			post.setImageUrl("/images/" + fileName);
		}

		// 動画の保存処理
		if (!videoFile.isEmpty()) {
			String fileName = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
			Path filePath = Paths.get(uploadPath, fileName);
			Files.copy(videoFile.getInputStream(), filePath);
			post.setVideoUrl("/images/" + fileName);
		}

		postRepository.save(post);
		return "redirect:/";
	}

//部分一致検索用API(サジェスト用)
	// PostController.java

	@GetMapping("/api/posts/suggestions")
	@ResponseBody
	public List<Map<String, Object>> getSuggestions(@RequestParam String title) {
		if (title.length() < 2)
			return List.of();

		String trimmed = title.replace(" ", "").replace("　", "");
		List<Post> posts = postRepository.findByMovieTitlePartially(trimmed);

		// Postエンティティをそのまま返すとLazy読み込みエラーになるため、
		// 必要なデータ（id, movieTitle）だけをMapに詰めて返します
		return posts.stream().map(p -> {
		Map<String, Object> map = new HashMap<>();
			map.put("id", p.getId());
			map.put("movieTitle", p.getMovieTitle());
			return map;
		}).collect(Collectors.toList());
	}

	// エラー発生時に優先的に動くエラーハンドラー
	@ExceptionHandler(IOException.class)
	public String handleIOException(IOException e, RedirectAttributes attrs) {
		attrs.addFlashAttribute("error", "ファイルの読み書きでエラーが発生しました");
		return "redirect:/";
	}

}
