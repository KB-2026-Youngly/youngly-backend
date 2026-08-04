package com.kb.youngly.dto.account;
import com.kb.youngly.enums.AccountStatus;
import com.kb.youngly.vo.account.AccountVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRegisterDTO {

    private String kbAccountId;
}
