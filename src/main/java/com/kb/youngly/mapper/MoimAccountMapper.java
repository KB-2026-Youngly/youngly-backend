package com.kb.youngly.mapper;

import com.kb.youngly.dto.account.AccountSearchDTO;
import com.kb.youngly.dto.account.KbAccountDTO;
import com.kb.youngly.dto.moimaccount.MoimAccountDTO;
import com.kb.youngly.vo.moimaccount.MoimAccountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MoimAccountMapper {

    // 모임통장 등록
    int insert(MoimAccountVO account);

    // 내 모임통장 조회
    List<MoimAccountDTO> findByUserId(String userId);

    // 모임통장 연동 비활성화
    int updateStatus(MoimAccountVO account);

    // ID로 조회
    MoimAccountVO findById(String moimAccountId);

    MoimAccountVO findByKbAccountId(String kbAccountId);

    List<KbAccountDTO> search(AccountSearchDTO dto);

    //모임통장 이름 변경
    int updateAccountName(@Param("moimAccountId") String moimAccountId, @Param("userId") String userId, @Param("accountName") String accountName);
}