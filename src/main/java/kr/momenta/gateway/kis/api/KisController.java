package kr.momenta.gateway.kis.api;

import kr.momenta.gateway.kis.api.res.*;
import kr.momenta.gateway.kis.client.KisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RequestMapping("/api/kis/v1")
@RestController
public class KisController {

    private final KisClient kisClient;

    @GetMapping("/top-gainers")
    public Mono<FluctuationResponse> getTopGainers() {
        return kisClient.fetchTopGainers();
    }

    @GetMapping("/top-losers")
    public Mono<FluctuationResponse> getTopLosers() {
        return kisClient.fetchTopLosers();
    }

    @GetMapping("/index-price")
    public Mono<IndexPriceResponse> getIndexPrice() {
        return kisClient.fetchIndexPrice();
    }

    @GetMapping("/industry-index-price")
    public Mono<IndustryIndexPriceResponse> getIndustryIndexPrice() {
        return kisClient.fetchIndustryIndexPrice();
    }

    @GetMapping("/market-cap-rank")
    public Mono<MarketCapRankResponse> getMarketCapRanking() {
        return kisClient.fetchMarketCapRanking();
    }

    @GetMapping("/volume-rank")
    public Mono<VolumeRankResponse> getVolumeRanking() {
        return kisClient.fetchVolumeRanking();
    }

    @GetMapping("/stocks-of-interest-rank")
    public Mono<StocksOfInterestRankResponse> getStocksOfInterestRanking() {
        return kisClient.fetchStocksOfInterestRanking();
    }

    @GetMapping("/trade-strength-rank")
    public Mono<TradeStrengthResponse> getTradeStrengthRanking() {
        return kisClient.fetchTradeStrengthRanking();
    }








}
