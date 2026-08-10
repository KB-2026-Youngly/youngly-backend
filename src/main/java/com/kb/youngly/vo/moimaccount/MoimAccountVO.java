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

    private LocalDateTime createdAt;

    private LocalDateTime syncedAt;

    private LocalDateTime updatedAt;

    private MoimAccountStatus accountStatus;
}
