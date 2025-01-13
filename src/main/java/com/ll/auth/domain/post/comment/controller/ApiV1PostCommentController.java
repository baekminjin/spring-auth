package com.ll.auth.domain.post.comment.controller;

import com.ll.auth.domain.member.member.entity.Member;
import com.ll.auth.domain.post.comment.dto.PostCommentDto;
import com.ll.auth.domain.post.comment.entity.PostComment;
import com.ll.auth.domain.post.post.entity.Post;
import com.ll.auth.domain.post.post.service.PostService;
import com.ll.auth.global.exceptions.ServiceException;
import com.ll.auth.global.rq.Rq;
import com.ll.auth.global.rsData.RsData;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class ApiV1PostCommentController {
	private final PostService postService;
	private final Rq rq;

	//댓글 다건 조회
	@GetMapping
	public List<PostCommentDto> getItems( //<PostComment> 양방향에 있는 것은 무한 재귀에 빠질 수 있어서 DTO 생성
										  @PathVariable long postId
	) {
		Post post = postService.findById(postId).orElseThrow(
				() -> new ServiceException("404-1", "%d번 글은 존재하지 않습니다.".formatted(postId))
		);
		return post
				.getCommentsByOrderByIdDesc()
				.stream()
				.map(PostCommentDto::new)
				.toList();
	}

	//단건 조회
	@GetMapping("/{id}")
	public PostCommentDto getItem(
			@PathVariable long postId,
			@PathVariable long id
	) {
		Post post = postService.findById(postId).orElseThrow(
				() -> new ServiceException("404-1", "%d번 글은 존재하지 않습니다.".formatted(postId))
		);

		return post
				.getCommentById(id)
				.map(PostCommentDto::new)
				.orElseThrow(
						() -> new ServiceException("404-2", "%d번 댓글은 존재하지 않습니다.".formatted(id))
				);
	}

	record PostCommentWriteReqBody(
			@NotBlank
			@Length(min = 2)
			String content
	) {
	}

	@PostMapping
	@Transactional
	public RsData<Void> writeItem(
			@PathVariable long postId,
			@RequestBody @Valid PostCommentWriteReqBody reqBody
	) {
		Member actor = rq.checkAuthentication();

		Post post = postService.findById(postId).orElseThrow(
				() -> new ServiceException("404-1", "%d번 글은 존재하지 않습니다.".formatted(postId))
		);

		PostComment postComment = post.addComment(
				actor,
				reqBody.content
		);

		postService.flush();

		return new RsData<>(
				"201-1",
				"%d번 댓글이 작성되었습니다.".formatted(postComment.getId())
		);
	}

	record PostCommentModifyReqBody(
			@NotBlank
			@Length(min = 2)
			String content
	) {
	}
	@PutMapping("/{id}")
	@Transactional
	public RsData<Void> modifyItem(
			@PathVariable long postId,
			@PathVariable long id,
			@RequestBody @Valid PostCommentModifyReqBody reqBody
	) {
		Member actor = rq.checkAuthentication();
		Post post = postService.findById(postId).orElseThrow(
				() -> new ServiceException("404-1", "%d번 글은 존재하지 않습니다.".formatted(postId))
		);
		PostComment postComment = post.getCommentById(id).orElseThrow(
				() -> new ServiceException("404-2", "%d번 댓글은 존재하지 않습니다.".formatted(id))
		);
		if (!postComment.getAuthor().equals(actor))
			throw new ServiceException("403-1", "작성자만 수정할 수 있습니다.");
		postComment.modify(reqBody.content);
		return new RsData<>(
				"200-1",
				"%d번 댓글이 수정되었습니다.".formatted(postComment.getId())
		);
	}

	@DeleteMapping("/{id}")
	@Transactional
	public RsData<Void> deleteItem(
			@PathVariable long postId,
			@PathVariable long id
	) {
		Member actor = rq.checkAuthentication();
		Post post = postService.findById(postId).orElseThrow(
				() -> new ServiceException("404-1", "%d번 글은 존재하지 않습니다.".formatted(postId))
		);
		PostComment postComment = post.getCommentById(id).orElseThrow(
				() -> new ServiceException("404-2", "%d번 댓글은 존재하지 않습니다.".formatted(id))
		);

		if (!actor.isAdmin() && !postComment.getAuthor().equals(actor))
			throw new ServiceException("403-1", "작성자만 삭제할 수 있습니다.");
		post.removeComment(postComment);
		return new RsData<>(
				"200-1",
				"%d번 댓글이 삭제되었습니다.".formatted(id)
		);
	}
}