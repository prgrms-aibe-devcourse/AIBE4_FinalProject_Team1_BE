package kr.inventory.domain.store.nts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NtsBusinessStatusRequest(
    @JsonProperty("b_no")
    List<String> businessNumbers
) {
    public static NtsBusinessStatusRequest of(String businessNumber) {
        return new NtsBusinessStatusRequest(List.of(businessNumber));
    }
}
