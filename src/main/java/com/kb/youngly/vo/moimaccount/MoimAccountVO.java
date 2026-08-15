package com.kb.youngly.vo.moimaccount;

import com.kb.youngly.enums.MoimAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoimAccountVO {

    private String moimAccountId;

    private String userId;

    private String kbAccountId;

    private String accountName;

    /** 연결된 KB 모임통장의 실제 계좌번호. 출금 원장의 상대 계좌번호로 기록한다. */
    private String accountNumber;

    /** 연결된 KB 모임통장의 은행명. 하드코딩하지 않고 계좌 원본값을 사용한다. */
    private String bankName;

    /** 연결된 KB 모임통장의 예금주명. 필요 시 거래 상대 정보로 사용할 수 있다. */
    private String ownerName;

    private LocalDateTime createdAt;

    private LocalDateTime syncedAt;

    private LocalDateTime updatedAt;

    private MoimAccountStatus accountStatus;
}
