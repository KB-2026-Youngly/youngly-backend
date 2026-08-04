package com.kb.youngly.mapper;

import com.kb.youngly.dto.account.AccountDTO;
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

}
