package com.kb.youngly.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 금감원 금융시장동향 Open API 설정.
 *
 * [INFO] authKey 는 application.properties 에 두고 커밋하지 않는다.
 *        예) fss.market.auth-key=발급받은32자리키
 */
@Getter
@Component
public class FssMarketProperties {

    @Value("${fss.market.base-url:https://www.fss.or.kr/fss/kr/openApi/api/fnncMrkt.jsp}")
    private String baseUrl;

    @Value("${fss.market.auth-key}")
    private String authKey;

    @Value("${fss.market.api-type:json}")
    private String apiType;
}
