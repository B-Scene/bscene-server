package com.umc.bscene.domain.post.repository;

import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.enums.PostType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByBand_IdAndIdLessThanOrderByIdDesc(Long bandId, Long cursor, Pageable pageable);

    List<Post> findByBand_IdAndTypeAndIdLessThanOrderByIdDesc(Long bandId, PostType type, Long cursor, Pageable pageable);
}
