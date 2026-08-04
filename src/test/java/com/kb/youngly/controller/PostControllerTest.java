package com.kb.youngly.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.youngly.dto.posts.PostApprovalRequestDTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch; // POST로 바꿨다면 post로 변경!
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class) // 스프링 테스트 컨텍스트 프레임워크를 JUnit과 연결
@WebAppConfiguration // WebApplicationContext를 생성하기 위해 필수
/*
 * 🚨 [매우 중요] 본인 프로젝트의 실제 xml 파일 경로에 맞게 아래 locations를 반드시 수정해 줘야 해!
 * 보통 src/main/webapp/WEB-INF/spring 하위에 위치해 있어.
 */
@ContextConfiguration(locations = {
        "file:src/main/webapp/WEB-INF/spring/root-context.xml",
        "file:src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml"
})
@Transactional // 테스트 종료 후 DB 롤백 (데이터 안 더러워짐!)
public class PostControllerTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Before // 각 @Test 메서드가 실행되기 전에 무조건 먼저 실행되는 세팅 구간
    public void setup() {
        // 스프링 레거시에서는 컨텍스트를 주입받아 MockMvc를 직접 빌드해 줘야 해!
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void testApprovePost_Success() throws Exception {
        // given: 테스트할 상황 세팅 (DB에 있는 실제 postId 하나를 적어주세요!)
        Long targetPostId = 1L;

        PostApprovalRequestDTO requestDTO = new PostApprovalRequestDTO();
        requestDTO.setUserId("test_user_999"); // 본인이 아닌 다른 유저 ID
        requestDTO.setApprovalStatus("APPROVE");
        requestDTO.setRejectReason("");

        String jsonRequest = objectMapper.writeValueAsString(requestDTO);

        // when & then: PATCH(또는 POST) 요청을 날리고 결과를 검증
        mockMvc.perform(patch("/api/posts/" + targetPostId + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk()) // 200 OK 상태인지 확인
                .andExpect(content().string("게시글 평가가 성공적으로 반영되었습니다.")); // 리턴 메시지 확인
    }

    @Test
    public void testRejectPost_WithoutReason_Fail() throws Exception {
        // given
        Long targetPostId = 1L;

        PostApprovalRequestDTO requestDTO = new PostApprovalRequestDTO();
        requestDTO.setUserId("test_user_999");
        requestDTO.setApprovalStatus("REJECT");
        requestDTO.setRejectReason(""); // 반려인데 사유를 비워둠!

        String jsonRequest = objectMapper.writeValueAsString(requestDTO);

        // when & then: 방어 로직에 의해 500(또는 설정한 상태 코드) 에러가 터지는지 확인
        mockMvc.perform(patch("/api/posts/" + targetPostId + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is5xxServerError());
    }
}