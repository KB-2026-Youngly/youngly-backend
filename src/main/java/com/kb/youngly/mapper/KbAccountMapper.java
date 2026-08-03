package com.kb.youngly.mapper;

import com.kb.youngly.dto.account.AccountSearchDTO;
import com.kb.youngly.vo.account.KbAccountVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface KbAccountMapper {
    /**
     * 이름 + 생년월일로 KB 계좌 조회
     */
    List<KbAccountVO> findByNameAndBirthday(AccountSearchDTO dto);

    /**
     등록할떄 은행명 + 계좌번호 확인을 조회
     */
    KbAccountVO findById(String kbAccountId);
}
