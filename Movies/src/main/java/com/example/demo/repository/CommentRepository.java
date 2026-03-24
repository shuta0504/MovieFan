package com.example.demo.repository;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // ここに .data が必要です

import com.example.demo.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    @Modifying
    @Transactional // Repository側にも付けておくとより確実です
    @Query("DELETE FROM Comment c WHERE c.id = :id")
    void deleteByIdCustom(@Param("id") Integer id);
}