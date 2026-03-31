package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.Post;

public interface PostRepository extends JpaRepository<Post, Integer> {
	// 作成日時の降順（新しい順）で全件取得する
	// JOIN FETCH を使うことで LazyInitializationException を防ぎつつ、新しい順に並べる
	@Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.likes ORDER BY p.createdAt DESC")
	List<Post> findAllWithLikesOrderByCreatedAtDesc();

	// 検索時も同様に likes を含めて新しい順にする場合
	@Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.movieTitle LIKE %:keyword% OR p.content LIKE %:keyword% ORDER BY p.createdAt DESC")
	List<Post> searchByKeywordWithLikes(@Param("keyword") String keyword);

	// 既存のメソッド（もしあれば）
	// PostRepository.java
	List<Post> findByUserId(Integer userId);

	@Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.userId = :userId ORDER BY p.createdAt DESC")
	List<Post> findByUserIdWithLikesOrderByCreatedAtDesc(@Param("userId") Integer userId);

	// PostRepository.java
	@Query("SELECT p FROM Post p WHERE REPLACE(REPLACE(p.movieTitle, ' ', ''), '　', '') = :trimmedTitle")
	List<Post> findByMovieTitleIgnoringSpaces(@Param("trimmedTitle") String trimmedTitle);

	@Query("SELECT p FROM Post p WHERE REPLACE(REPLACE(p.movieTitle, ' ', ''), '　', '') LIKE %:trimmedTitle%")
	List<Post> findByMovieTitlePartially(@Param("trimmedTitle") String trimmedTitle);
}