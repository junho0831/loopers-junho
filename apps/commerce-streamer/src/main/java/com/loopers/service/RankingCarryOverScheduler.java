package com.loopers.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 콜드 스타트 완화를 위한 점수 이월 스케줄러
 * 매일 23:50 (Asia/Seoul)에 다음날 랭킹 키를 미리 생성하면서 점수를 일부 이월합니다.
 */
@Component
public class RankingCarryOverScheduler {

    private static final Logger log = LoggerFactory.getLogger(RankingCarryOverScheduler.class);

    private final RankingService rankingService;

    public RankingCarryOverScheduler(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    public void carryOverForTomorrow() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            rankingService.carryOverPreviousRanking(tomorrow);
            log.info("Completed ranking carry-over for {}", tomorrow);
        } catch (Exception e) {
            log.error("Failed ranking carry-over scheduling", e);
        }
    }
}

