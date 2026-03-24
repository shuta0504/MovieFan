package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@NotBlank(message = "コメントを入力してください") // 空文字やスペースのみを防ぐ
    @Size(max = 200, message = "コメントは200文字以内で入力してください") // 文字数制限
    @Column(nullable = false)
	private String content; // コメント内容
	
	private LocalDateTime createdAt;

	@ManyToOne // 多くのコメントが、一つの投稿に紐付く
	@JoinColumn(name = "post_id")
	private Post post;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	// これがPostControllerのaddComment「comment.setContent(content)」に対応します
	public void setContent(String content) {
		this.content = content;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	// これがPostControllerのaddComment「comment.setCreatedAt(...)」に対応します
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Post getPost() {
		return post;
	}

	// これが「comment.setPost(post)」に対応します
	public void setPost(Post post) {
		this.post = post;
	}
}