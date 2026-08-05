package com.kb.youngly.dto.round;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 첫 번째 라운드 생성 요청 DTO.
 * 두 번째 라운드부터는 직전 라운드 종료일을 기준으로 시작일을 자동 계산하므로 생략할 수 있다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoundRequest {

    /** 첫 번째 라운드를 시작할 날짜. 이후 회차에서는 사용하지 않는다. */
    private LocalDate startDate;
}
