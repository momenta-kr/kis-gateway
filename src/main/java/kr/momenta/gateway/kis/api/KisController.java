package kr.momenta.gateway.kis.api;

import kr.momenta.gateway.kis.api.res.*;
import kr.momenta.gateway.kis.client.KisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/api/kis/v1")
@RestController
public class KisController {

    private final KisClient kisClient;

    @GetMapping("/domestic-stock-current-price")
    public Mono<DomesticStockCurrentPriceResponse> getDomesticStockCurrentPrice(String stockCode) {
        System.out.println(stockCode);
        return kisClient.fetchDomesticStockCurrentPrice(stockCode);
    }

    @GetMapping("/investment-opinion")
    public Mono<InvestmentOpinionApiResponse> getInvestmentOpinion(
            @RequestParam String symbol,
            @RequestParam LocalDateTime now
            ) {
        return kisClient.fetchInvestmentOpinion(symbol, now);
    }

    @GetMapping("/candles")
    public Mono<CandlesResponse> getCandles(
            @RequestParam String excd,
            @RequestParam String symbol,
            @RequestParam int nmin,
            @RequestParam int limit
    ) {
        return kisClient.fetchCandles(excd, symbol, nmin, limit);
    }

    @GetMapping("/domestic-stock-period-prices")
    public Mono<DomesticStockPriceResponse> getCandles(
            @RequestParam String symbol,
            @RequestParam String period,
            @RequestParam String from,
            @RequestParam String to
    ) {
        return kisClient.fetchDomesticStockPeriodPrices(symbol, period, from, to);
    }

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
