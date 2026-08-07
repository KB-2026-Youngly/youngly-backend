package com.kb.youngly.mapper;

import com.kb.youngly.dto.group.GroupListResponse;
import com.kb.youngly.vo.group.GroupVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMapper {

    /**
     * 그룹 생성
     */
    void insertGroup(GroupVO group);

    /**
     * 그룹 목록 조회
     */
    List<GroupListResponse> findGroupsByUserId(String userId);

    /**
     * 그룹 상세 조회
     */
    GroupVO findGroupById(String groupId);

    /**
     * 그룹 수정
     */
    void updateGroup(GroupVO group);

    /**
     * 그룹 종료
     */
    void finishGroup(String groupId);

    /**
     * 그룹 참여
     */
    GroupVO findGroupByInviteCode(String inviteCode);

    /**
     * 초대코드 재발급
     */
    void updateInviteCode(
            @Param("groupId") String groupId,
            @Param("inviteCode") String inviteCode);

    boolean existsInviteCode(String inviteCode);
}