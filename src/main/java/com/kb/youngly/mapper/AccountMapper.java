package com.kb.youngly.mapper;

import com.kb.youngly.dto.account.AccountDTO;
import com.kb.youngly.dto.account.AccountBalanceSyncResponseDTO;
import com.kb.youngly.enums.AccountType;
import com.kb.youngly.vo.account.AccountDetailVO;
import com.kb.youngly.vo.account.AccountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountMapper {
    // 계좌 등록
    int insert(AccountVO account);

    // 내 대표 계좌 조회
    AccountDTO findByUserIdAndType(@Param("userId") String userId,
                                   @Param("accountType") AccountType accountType);
    // 대표 계좌 변경
    int update(AccountVO account);

    // 계좌 삭제
    int delete(String accountId);

    int updateAccountStatus(AccountVO account);

    AccountDetailVO findById(String accountId);

    // 소유자 조건으로 마지막 잔액 동기화 시각 갱신
    int updateSyncedAt(@Param("accountId") String accountId,
                       @Param("userId") String userId);

    // 갱신된 시각과 KB 원장 최신 잔액 조회
    AccountBalanceSyncResponseDTO findBalanceSyncResult(
            @Param("accountId") String accountId,
            @Param("userId") String userId);

}
