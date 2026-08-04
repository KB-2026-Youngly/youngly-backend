package com.kb.youngly.dto.moimaccount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoimAccountRegisterResponseDTO {

    private String moimAccountId;

    private String message;

}