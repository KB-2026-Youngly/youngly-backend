package com.kb.youngly.support;

import com.kb.youngly.enums.ItemCategory;
import com.kb.youngly.mapper.CharacterMapper;
import com.kb.youngly.vo.character.OwnedCharacterVO;
import com.kb.youngly.vo.point.CollectibleItemVO;
import com.kb.youngly.vo.point.UserItemVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryCharacterMapper implements CharacterMapper {

    private final Set<String> users = new HashSet<>();
    private final Map<Long, CollectibleItemVO> items = new LinkedHashMap<>();
    private final List<UserItemVO> userItems = new ArrayList<>();
    private final List<String> callLog = new ArrayList<>();
    private long nextUserItemId = 1L;
    private int insertResult = 1;

    public void addUser(String userId) {
        users.add(userId);
    }

    public void addItem(CollectibleItemVO item) {
        items.put(item.getItemId(), item);
    }

    public void addOwnedItem(String userId, Long itemId, LocalDateTime acquiredAt) {
        userItems.add(UserItemVO.builder()
                .userItemId(nextUserItemId++)
                .itemId(itemId)
                .userId(userId)
                .isEquipped(false)
                .createdAt(acquiredAt)
                .build());
    }

    public List<UserItemVO> getUserItems() {
        return new ArrayList<>(userItems);
    }

    public List<String> getCallLog() {
        return new ArrayList<>(callLog);
    }

    public void setInsertResult(int insertResult) {
        this.insertResult = insertResult;
    }

    @Override
    public String lockUserForUpdate(String userId) {
        callLog.add("lockUserForUpdate");
        return users.contains(userId) ? userId : null;
    }

    @Override
    public List<CollectibleItemVO> findUnownedCharacters(String userId) {
        callLog.add("findUnownedCharacters");
        Set<Long> ownedItemIds = new HashSet<>();
        for (UserItemVO userItem : userItems) {
            if (userId.equals(userItem.getUserId())) {
                ownedItemIds.add(userItem.getItemId());
            }
        }

        return items.values().stream()
                .filter(item -> item.getItemCategory() == ItemCategory.CHARACTER)
                .filter(item -> !ownedItemIds.contains(item.getItemId()))
                .sorted(Comparator.comparing(CollectibleItemVO::getItemId))
                .toList();
    }

    @Override
    public int insertUserItem(UserItemVO userItem) {
        callLog.add("insertUserItem");
        if (insertResult != 1) {
            return insertResult;
        }
        userItem.setUserItemId(nextUserItemId++);
        userItem.setCreatedAt(LocalDateTime.now());
        userItems.add(userItem);
        return 1;
    }

    @Override
    public List<OwnedCharacterVO> findOwnedCharacters(String userId) {
        callLog.add("findOwnedCharacters");
        return userItems.stream()
                .filter(userItem -> userId.equals(userItem.getUserId()))
                .filter(userItem -> {
                    CollectibleItemVO item = items.get(userItem.getItemId());
                    return item != null && item.getItemCategory() == ItemCategory.CHARACTER;
                })
                .sorted(Comparator
                        .comparing(UserItemVO::getCreatedAt, Comparator.reverseOrder())
                        .thenComparing(UserItemVO::getUserItemId, Comparator.reverseOrder()))
                .map(userItem -> {
                    CollectibleItemVO item = items.get(userItem.getItemId());
                    return OwnedCharacterVO.builder()
                            .characterId(item.getItemId())
                            .name(item.getItemName())
                            .imageUrl(item.getImageUrl())
                            .acquiredAt(userItem.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
