package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;

@Controller
public class PostController {

	@Autowired
	private PostRepository postRepository;

	@GetMapping("/")
	public String index(Model model) {
		// 全ての投稿をIDの降順（新着順）で取得
		List<Post> postList = postRepository.findAllByOrderByIdDesc();

		model.addAttribute("posts", postList);

		return "index";
	}

	@Value("${upload.path}")
	private String uploadPath;

	@GetMapping("/post/new")
	public String newPost() {
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
	@GetMapping("/post/edit/{id}")
	public String editPost(@PathVariable("id") Integer id, Model model) {
		Post post = postRepository.findById(id).orElseThrow();
		model.addAttribute("post", post); // 現在のデータをフォームに渡す
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
	public String deletePost(@PathVariable("id") Integer id) {
		// 削除前にデータを取得（ファイル削除が必要な場合のため）
		Post post = postRepository.findById(id).orElseThrow();

		// (オプション) もしMacのフォルダ内の画像ファイル自体も消したい場合はここに追加
		// if (post.getImageUrl() != null) {
		// String fileName = post.getImageUrl().replace("/images/", "");
		// new File(uploadPath + "/" + fileName).delete();
		// }

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
		comment.setPost(post);
		comment.setCreatedAt(LocalDateTime.now());

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
	@PostMapping("/comment/update/{id}")
	public String updateComment(@PathVariable("id") Integer id, @RequestParam("content") String content) {
		Comment comment = commentRepository.findById(id).orElseThrow();

		comment.setContent(content);
		commentRepository.save(comment);

		return "redirect:/post/" + comment.getPost().getId();
	}

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

	@PostMapping("/post/create")
	public String createPost(@RequestParam("movieTitle") String movieTitle, @RequestParam("content") String content,
			@RequestParam("imageFile") MultipartFile imageFile, @RequestParam("videoFile") MultipartFile videoFile)
			throws IOException {

		Post post = new Post();
		post.setMovieTitle(movieTitle);
		post.setContent(content);
		post.setUserId(1); // ログイン機能ができるまでは一旦固定値の「１」でセット

		// 画像の保存処理
		if (!imageFile.isEmpty()) {
			String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
			Path filePath = Paths.get(uploadPath, fileName);
			Files.copy(imageFile.getInputStream(), filePath);
			post.setImageUrl("/images/" + fileName); // 後ほど画像を表示するためのURLパス
		}
		// 動画の保存処理
		if (!videoFile.isEmpty()) {
			String fileName = UUID.randomUUID() + "_" + videoFile.getOriginalFilename();
			Path filePath = Paths.get(uploadPath, fileName);
			Files.copy(videoFile.getInputStream(), filePath);
			post.setVideoUrl("/images/" + fileName);
		}

		postRepository.save(post);
		return "redirect:/"; // 画像や動画をsaveした後のためredirect
	}

	// エラー発生時に優先的に動くエラーハンドラー
	@ExceptionHandler(IOException.class)
	public String handleIOException(IOException e, RedirectAttributes attrs) {
		attrs.addFlashAttribute("error", "ファイルの読み書きでエラーが発生しました");
		return "redirect:/";
	}

}
