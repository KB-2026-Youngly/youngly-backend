package com.kb.youngly.vo.account;

import com.kb.youngly.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountVO {
    private String accountId;
    private String userId;
    private String kbAccountId;
    private AccountStatus accountStatus;
    private String accountName;
    /** 연결된 KB 계좌의 실제 계좌번호. 거래 원장의 상대 계좌번호 기록에 사용한다. */
    private String accountNumber;
    /** 연결된 KB 계좌의 은행명. 거래 원장의 상대 은행명 기록에 사용한다. */
    private String bankName;
    /** 연결된 KB 계좌의 예금주명. 상대방 표시 이름으로 사용한다. */
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime syncedAt;
    private LocalDateTime updatedAt;
}
