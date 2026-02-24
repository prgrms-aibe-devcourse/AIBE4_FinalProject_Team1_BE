package kr.inventory.domain.store.nts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NtsBusinessStatusResponse(
    @JsonProperty("status_code")
    String statusCode,

    @JsonProperty("request_cnt")
    Integer requestCnt,

    @JsonProperty("match_cnt")
    Integer matchCnt,

    List<DataItem> data
) {
    public record DataItem(
        @JsonProperty("b_no")
        String bNo,

        @JsonProperty("b_stt")
        String bStt,

        @JsonProperty("b_stt_cd")
        String bSttCd,

        @JsonProperty("tax_type")
        String taxType,

        @JsonProperty("tax_type_cd")
        String taxTypeCd,

        @JsonProperty("end_dt")
        String endDt,

        @JsonProperty("utcc_yn")
        String utccYn,

        @JsonProperty("tax_type_change_dt")
        String taxTypeChangeDt,

        @JsonProperty("invoice_apply_dt")
        String invoiceApplyDt,

        @JsonProperty("rbf_tax_type")
        String rbfTaxType,

        @JsonProperty("rbf_tax_type_cd")
        String rbfTaxTypeCd
    ) {
    }
}
