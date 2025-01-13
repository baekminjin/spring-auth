package com.ll.auth.domain.post.comment.controller;

import com.ll.auth.domain.member.member.entity.Member;
import com.ll.auth.domain.post.comment.dto.PostCommentDto;
import com.ll.auth.domain.post.comment.entity.PostComment;
import com.ll.auth.domain.post.post.entity.Post;
import com.ll.auth.domain.post.post.service.PostService;
import com.ll.auth.global.exceptions.ServiceException;
import com.ll.auth.global.rq.Rq;
import com.ll.auth.global.rsData.RsData;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class ApiV1PostCommentController {
	@Autowired
	@Lazy
	private ApiV1PostCommentController self;
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
	public RsData<Void> writeItem(
			@PathVariable long postId,
			@RequestBody @Valid PostCommentWriteReqBody reqBody
	) {
		PostComment postComment = self._writeItem(postId, reqBody);

		return new RsData<>(
				"201-1",
				"%d번 댓글이 작성되었습니다.".formatted(postComment.getId())
				//트랜잭션이 끝나야 인서트가 실행되는데 그 전에 id를 가져오면 null이 나오는 오류
				// 트랜잭션은 self / 트랜잭션 메소드를 만들어서 끝나면 호출 하여 해결
		);
	}

	@Transactional
	public PostComment _writeItem(
			long postId,
			PostCommentWriteReqBody reqBody
	) {
		Member actor = rq.checkAuthentication();

		Post post = postService.findById(postId).orElseThrow(
				() -> new ServiceException("404-1", "%d번 글은 존재하지 않습니다.".formatted(postId))
		);

		return post.addComment(
				actor,
				reqBody.content
		);
		//인서트 발생
	}
}