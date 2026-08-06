package com.kb.youngly.dto.market;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FssMarketItem {
    private String contentId;
    private String subject;
    private String publishOrg;
    private String originUrl;
    private String regDate;
    private String atchfileUrl;
    private String atchfileNm;
    @JsonAlias("contentKor")
    private String contentsKor;
}
