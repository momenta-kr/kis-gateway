package kr.momenta.gateway.kis.api.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 응답에 추가 필드가 와도 무시
public class InvestmentOpinionApiResponse extends BaseResponse {

    @JsonProperty("output")
    private List<InvestmentOpinionItem> items; // 응답상세(Object Array)

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InvestmentOpinionItem {

        @JsonProperty("stck_bsop_date")
        private String stockBusinessDate; // 주식영업일자 (YYYYMMDD)

        @JsonProperty("invt_opnn")
        private String investmentOpinion; // 투자의견

        @JsonProperty("invt_opnn_cls_code")
        private String investmentOpinionClassCode; // 투자의견구분코드

        @JsonProperty("rgbf_invt_opnn")
        private String previousInvestmentOpinion; // 직전투자의견

        @JsonProperty("rgbf_invt_opnn_cls_code")
        private String previousInvestmentOpinionClassCode; // 직전투자의견구분코드

        @JsonProperty("mbcr_name")
        private String memberCompanyName; // 회원사명

        @JsonProperty("hts_goal_prc")
        private String htsTargetPrice; // HTS목표가격

        @JsonProperty("stck_prdy_clpr")
        private String previousDayClosePrice; // 주식전일종가

        @JsonProperty("stck_nday_esdg")
        private String nDayDisparity; // 주식N일괴리도

        @JsonProperty("nday_dprt")
        private String nDayDisparityRate; // N일괴리율

        @JsonProperty("stft_esdg")
        private String stockFuturesDisparity; // 주식선물괴리도

        @JsonProperty("dprt")
        private String stockFuturesDisparityRate; // 괴리율
    }
}
