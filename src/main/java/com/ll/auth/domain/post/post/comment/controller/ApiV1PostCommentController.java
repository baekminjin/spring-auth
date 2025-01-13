package com.ll.auth.domain.post.comment.controller;
import com.ll.auth.domain.post.comment.dto.PostCommentDto;
import com.ll.auth.domain.post.comment.entity.PostComment;
import com.ll.auth.domain.post.post.entity.Post;
import com.ll.auth.domain.post.post.service.PostService;
import com.ll.auth.global.exceptions.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class ApiV1PostCommentController {
	private final PostService postService;

	//댓글 조회
	@GetMapping
	public List<PostCommentDto> getItems( //<PostComment> 양방향에 있는 것은 무한 재귀에 빠질 수 있어서 DTO 생성
			@PathVariable long postId
	) {
		Post post = postService.findById(postId).orElseThrow(
				() -> new ServiceException("404-1", "%d번 글은 존재하지 않습니다.".formatted(postId))
		);
		return post
				.getComments()
				.stream()
				.map(PostCommentDto::new)
				.toList();
	}
}