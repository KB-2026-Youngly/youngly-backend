package com.kb.youngly.dto.market;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 금감원 금융시장동향 API 응답 루트.
 *
 * 실제 응답은 {@code reponse.result[]} 구조이며, resultCode 는 {@code "1"} 이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FssMarketApiResponse {

    @JsonProperty("reponse")
    @JsonAlias("response")
    private Body response;

    public Body getResponse() {
        return response;
    }

    public void setResponse(Body response) {
        this.response = response;
    }

    public List<FssMarketItem> resolveItems() {
        if (response == null || response.getResult() == null) {
            return Collections.emptyList();
        }
        return response.getResult();
    }

    public int resolveResultCnt() {
        return resolveItems().size();
    }

    public boolean isSuccessful() {
        return response != null && response.isSuccessful();
    }

    public String getResultCode() {
        return response == null ? null : response.getResultCode();
    }

    public String getResultMsg() {
        return response == null ? null : response.getResultMsg();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private static final Set<String> SUCCESS_CODES = Set.of("1", "00", "000", "0000");

        private String resultCode;
        private String resultMsg;
        private Integer resultCnt;
        private List<FssMarketItem> result;

        public boolean isSuccessful() {
            return resultCode != null && SUCCESS_CODES.contains(resultCode.trim());
        }

        public String getResultCode() {
            return resultCode;
        }

        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }

        public String getResultMsg() {
            return resultMsg;
        }

        public void setResultMsg(String resultMsg) {
            this.resultMsg = resultMsg;
        }

        public Integer getResultCnt() {
            return resultCnt;
        }

        public void setResultCnt(Integer resultCnt) {
            this.resultCnt = resultCnt;
        }

        public List<FssMarketItem> getResult() {
            return result;
        }

        public void setResult(List<FssMarketItem> result) {
            this.result = result;
        }
    }
}
