package kr.momenta.gateway.kis.client;

import kr.momenta.gateway.kis.api.res.*;
import kr.momenta.gateway.kis.property.KisProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@RequiredArgsConstructor
@Component
public class KisClient {

    private final WebClient kisWebClient;
    private final KisTokenService kisTokenService;
    private final KisProperties kisProperties;


    public Mono<IndustryIndexPriceResponse> fetchIndustryIndexPrice() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/quotations/inquire-index-category-price")
                                                .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                                                .queryParam("FID_INPUT_ISCD", "0001")
                                                .queryParam("FID_COND_SCR_DIV_CODE", "20214")
                                                .queryParam("FID_MRKT_CLS_CODE", "K")
                                                .queryParam("FID_BLNG_CLS_CODE", "3")
                                                .build()
                                )
                                .headers(commonHeaders("FHPUP02140000", token))
                                .retrieve()
                                .bodyToMono(IndustryIndexPriceResponse.class)
                );
    }

    /**
     * 국내주식 등락률 순위
     */
    public Mono<FluctuationResponse> fetchTopGainers() {
        return fetchFluctuation(true);
    }

    /**
     * 국내주식 등락률 순위
     */
    public Mono<FluctuationResponse> fetchTopLosers() {
        return fetchFluctuation(false);
    }

    private Mono<FluctuationResponse> fetchFluctuation(boolean isUp) {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/fluctuation")
                                                .queryParam("fid_rsfl_rate2", "")
                                                .queryParam("fid_cond_mrkt_div_code", "J")
                                                .queryParam("fid_cond_scr_div_code", "20170")
                                                .queryParam("fid_input_iscd", "0001")
                                                .queryParam("fid_rank_sort_cls_code", isUp ? "0" : "1")
                                                .queryParam("fid_input_cnt_1", "0")
                                                .queryParam("fid_prc_cls_code", isUp ? "0" : "1")
                                                .queryParam("fid_input_price_1", "")
                                                .queryParam("fid_input_price_2", "")
                                                .queryParam("fid_vol_cnt", "")
                                                .queryParam("fid_trgt_cls_code", "0")
                                                .queryParam("fid_trgt_exls_cls_code", "0")
                                                .queryParam("fid_div_cls_code", "0")
                                                .queryParam("fid_rsfl_rate1", "")
                                                .build())
                                .headers(commonHeaders("FHPST01700000", token))
                                .retrieve()
                                .bodyToMono(FluctuationResponse.class)
                );
    }

    public Mono<IndexPriceResponse> fetchIndexPrice() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/fluctuation")
                                                .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                                                .queryParam("FID_INPUT_ISCD", "0001")
                                                .build())
                                .headers(commonHeaders("FHPUP02100000", token))
                                .retrieve()
                                .bodyToMono(IndexPriceResponse.class)
                );
    }

    public Mono<MarketCapRankResponse> fetchMarketCapRanking() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/market-cap")
                                                .queryParam("fid_input_price_2", "")
                                                .queryParam("fid_cond_mrkt_div_code", "J")
                                                .queryParam("fid_cond_scr_div_code", "20174")
                                                .queryParam("fid_div_cls_code", "0")
                                                .queryParam("fid_input_iscd", "0001")
                                                .queryParam("fid_trgt_cls_code", "0")
                                                .queryParam("fid_trgt_exls_cls_code", "0")
                                                .queryParam("fid_input_price_1", "")
                                                .queryParam("fid_vol_cnt", "")
                                                .build())
                                .headers(commonHeaders("FHPST01740000", token))
                                .retrieve()
                                .bodyToMono(MarketCapRankResponse.class));
    }

    public Mono<VolumeRankResponse> fetchVolumeRanking() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/quotations/volume-rank")
                                                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                                .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                                                .queryParam("FID_INPUT_ISCD", "0000")
                                                .queryParam("FID_DIV_CLS_CODE", "0")
                                                .queryParam("FID_BLNG_CLS_CODE", "3")
                                                .queryParam("FID_TRGT_CLS_CODE", "111111111")
                                                .queryParam("FID_TRGT_EXLS_CLS_CODE", "0000000000")
                                                .queryParam("FID_INPUT_PRICE_1", "")
                                                .queryParam("FID_INPUT_PRICE_2", "")
                                                .queryParam("FID_VOL_CNT", "")
                                                .queryParam("FID_INPUT_DATE_1", "")

                                                .build())
                                .headers(commonHeaders("FHPST01710000", token))
                                .retrieve()
                                .bodyToMono(VolumeRankResponse.class));
    }

    public Mono<StocksOfInterestRankResponse> fetchStocksOfInterestRanking() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/top-interest-stock")
                                                .queryParam("fid_input_iscd_2", "000000")
                                                .queryParam("fid_cond_mrkt_div_code", "J")
                                                .queryParam("fid_cond_scr_div_code", "20180")
                                                .queryParam("fid_input_iscd", "0001")
                                                .queryParam("fid_trgt_cls_code", "0")
                                                .queryParam("fid_trgt_exls_cls_code", "0")
                                                .queryParam("fid_input_price_1", "")
                                                .queryParam("fid_input_price_2", "")
                                                .queryParam("fid_vol_cnt", "")
                                                .queryParam("fid_div_cls_code", "0")
                                                .queryParam("fid_input_cnt_1", "1")
                                                .build())
                                .headers(commonHeaders("FHPST01800000", token))
                                .retrieve()
                                .bodyToMono(StocksOfInterestRankResponse.class));
    }

    public Mono<TradeStrengthResponse> fetchTradeStrengthRanking() {
        return kisTokenService.getAccessToken()
                .flatMap(token ->
                        kisWebClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("/uapi/domestic-stock/v1/ranking/volume-power")
                                                .queryParam("fid_trgt_exls_cls_code", "0")
                                                .queryParam("fid_cond_mrkt_div_code", "J")
                                                .queryParam("fid_cond_scr_div_code", "20168")
                                                .queryParam("fid_input_iscd", "0000")
                                                .queryParam("fid_div_cls_code", "0")
                                                .queryParam("fid_input_price_1", "")
                                                .queryParam("fid_input_price_2", "")
                                                .queryParam("fid_vol_cnt", "")
                                                .queryParam("fid_trgt_cls_code", "0")
                                                .build())
                                .headers(commonHeaders("FHPST01680000", token))
                                .retrieve()
                                .bodyToMono(TradeStrengthResponse.class));
    }


    private @NonNull Consumer<HttpHeaders> commonHeaders(String transactionId, String token) {
        return httpHeaders -> {
            httpHeaders.setBearerAuth(token);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("appkey", kisProperties.getAppKey());
            httpHeaders.set("appsecret", kisProperties.getAppSecret());
            httpHeaders.set("tr_id", transactionId);
            httpHeaders.set("custtype", "P");
        };
    }
}
