package com.kb.youngly.dto.group;

import com.kb.youngly.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupListResponse {

    private String groupId;

    private String groupName;

    private Integer groupCount;

    private GroupStatus groupStatus;
}