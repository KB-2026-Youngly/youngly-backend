package com.kb.youngly.mapper;

import com.kb.youngly.enums.AccountType;
import com.kb.youngly.vo.account.AccountTransactionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountTransactionMapper {

    // 개인 입출금·개인연금 계좌 접근 권한 확인 후 kbAccountId 반환
    String findPersonalKbAccountId(
            @Param("userId") String userId,
            @Param("accountId") String accountId,
            @Param("accountType") AccountType accountType
    );

    // 모임통장 소유자·참여자 접근 권한 확인 후 kbAccountId 반환
    String findMoimKbAccountId(
            @Param("userId") String userId,
            @Param("moimAccountId") String moimAccountId
    );

    List<AccountTransactionVO> findByKbAccountId(
            @Param("kbAccountId") String kbAccountId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}