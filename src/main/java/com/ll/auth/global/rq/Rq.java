package com.ll.auth.global.rq;
import com.ll.auth.domain.member.member.entity.Member;
import com.ll.auth.domain.member.member.service.MemberService;
import com.ll.auth.global.exceptions.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import java.util.Optional;

// Request/Response 를 추상화한 객체
@RequestScope
@Component
@RequiredArgsConstructor
public class Rq {

	private final MemberService memberService;
	private final HttpServletRequest request;

	public Member checkAuthentication() {
		String credentials = request.getHeader("Authorization");
		String apiKey = credentials.substring("Bearer ".length());
		Optional<Member> opActor = memberService.findByApiKey(apiKey);
		if (opActor.isEmpty())
			throw new ServiceException("401-1", "비밀번호가 일치하지 않습니다.");
		return opActor.get();
	}
}