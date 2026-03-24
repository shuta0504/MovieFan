package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Like;

public interface LikeRepository extends JpaRepository<Like,Integer>{
	// 特定のユーザーが特定の投稿に既に「良いね」しているか確認
	Optional<Like> findByUserIdAndPostId(Integer userId,Integer PostId);
	// 特定の投稿の「良いね」数をカウント
	long countByPostId(Integer postId);
}