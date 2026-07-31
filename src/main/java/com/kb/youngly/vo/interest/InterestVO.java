<<<<<<<< Updated upstream:src/main/java/com/kb/youngly/vo/survey/InterestVO.java
package com.kb.youngly.vo.survey;
========
package com.kb.youngly.vo.interest;
>>>>>>>> Stashed changes:src/main/java/com/kb/youngly/vo/interest/InterestVO.java

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestVO {
    private Long interestId;
    private String interestName;
    private Boolean isInvestment;
    private LocalDateTime createdAt;
}
