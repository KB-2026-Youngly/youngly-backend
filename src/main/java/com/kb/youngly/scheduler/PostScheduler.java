package com.kb.youngly.scheduler;

import com.kb.youngly.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostScheduler {

    private final PostMapper postMapper;

    /**
     * 자정 기준 전날 PENDING 게시글 일괄 자동 승인 스케줄러
     * - 매일 자정(00:00:00)에 1회 실행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void autoApproveTimedOutPosts() {
        System.out.println("[SCHEDULER] ⏰ 다음날 자정 인증 게시글 자동 승인 스케줄러 작동 중...");

        try {
            // 매퍼를 호출해서 업데이트 쿼리 실행
            int updatedCount = postMapper.updatePostsToAutoApproved();

            if (updatedCount > 0) {
                System.out.println("[SCHEDULER] ✨ 총 " + updatedCount + "개의 게시글이 자동 승인 처리되었습니다.");
            }
        } catch (Exception e) {
            System.err.println("[SCHEDULER ERROR] ❌ 자동 승인 처리 중 에러 발생: " + e.getMessage());
        }
    }
}