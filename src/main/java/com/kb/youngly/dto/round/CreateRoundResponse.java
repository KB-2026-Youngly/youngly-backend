package com.kb.youngly.dto.round;

import com.kb.youngly.enums.RoundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** 생성된 라운드의 주요 정보와 이력이 생성된 참여자 수를 반환하는 DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoundResponse {

    /** DB에서 자동 생성된 라운드 식별자. */
    private Long roundId;

    /** 라운드가 속한 그룹 식별자. */
    private String groupId;

    /** 해당 그룹 안에서 1부터 순차 증가하는 회차 번호. */
    private Integer roundNo;

    /** 프론트엔드가 지정한 라운드 시작일. */
    private LocalDate startDate;

    /** 시작일을 포함하여 그룹의 roundCycleDays일 동안 진행되도록 계산한 종료일. */
    private LocalDate endDate;

    /** 생성 시점에는 항상 ONGOING이다. */
    private RoundStatus roundStatus;

    /** round_history가 생성된 참여자 수. */
    private Integer participantCount;
}
