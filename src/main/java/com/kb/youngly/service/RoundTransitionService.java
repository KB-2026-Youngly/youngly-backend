package com.kb.youngly.service;

import java.time.LocalDate;
import java.util.List;

/** 종료 라운드의 상태 전환과 다음 라운드 자동 생성을 담당하는 서비스. */
public interface RoundTransitionService {

    /** 지정한 날짜에 자동 전환해야 하는 진행 중 그룹 ID를 조회한다. */
    List<String> findDueGroupIds(LocalDate transitionDate);

    /** 한 그룹의 종료 라운드를 정산 대기로 바꾸고 다음 라운드를 생성한다. */
    boolean transitionToNextRound(String groupId, LocalDate transitionDate);
}
