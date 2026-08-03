package com.kb.youngly.vo.point;

import com.kb.youngly.enums.AccPart;
import com.kb.youngly.enums.BaseCharacter;
import com.kb.youngly.enums.ItemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectibleItemVO {
    private Long itemId;
    private ItemCategory itemCategory;
    private String itemName;
    private String imageUrl;
    private BigDecimal dropRate;
    private BaseCharacter baseCharacter;
    private AccPart accPart;
    private LocalDateTime createdAt;
}
