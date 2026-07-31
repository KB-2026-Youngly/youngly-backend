<<<<<<<< Updated upstream:src/main/java/com/kb/youngly/vo/survey/InterestUserVO.java
package com.kb.youngly.vo.survey;
========
package com.kb.youngly.vo.interest;
>>>>>>>> Stashed changes:src/main/java/com/kb/youngly/vo/interest/InterestUserVO.java

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestUserVO {
    private Long interestId;
    private String userId;
    private LocalDateTime createdAt;
}
