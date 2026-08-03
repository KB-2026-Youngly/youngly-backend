package com.kb.youngly.dto.account;

import com.kb.youngly.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PensionStatusUpdateDTO {

    private AccountStatus accountStatus;

}