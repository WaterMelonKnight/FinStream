package io.finstream.api;

import io.finstream.query.FundingRateStateResponse;
import io.finstream.query.MarketQueryService;
import io.finstream.query.MarketStateResponse;
import io.finstream.query.OpenInterestStateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {
    private final MarketQueryService service;

    public MarketController(MarketQueryService service) { this.service = service; }

    @GetMapping("/{symbol}/state")
    public Mono<MarketStateResponse> state(@PathVariable String symbol) {
        return Mono.fromCallable(() -> service.getMarketState(symbol))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{symbol}/funding-rate")
    public Mono<FundingRateStateResponse> fundingRate(@PathVariable String symbol) {
        return Mono.fromCallable(() -> service.getFundingRateState(symbol))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{symbol}/open-interest")
    public Mono<OpenInterestStateResponse> openInterest(@PathVariable String symbol) {
        return Mono.fromCallable(() -> service.getOpenInterestState(symbol))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
