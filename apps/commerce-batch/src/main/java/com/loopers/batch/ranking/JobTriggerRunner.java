package com.loopers.batch.ranking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * IntelliJ 실행에서도 확실히 Job을 트리거하기 위한 Runner.
 * spring.batch.job.name=weeklyMonthlyRankingJob 이 전달된 경우에만 실행합니다.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class JobTriggerRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(JobTriggerRunner.class);

    private final JobLauncher jobLauncher;
    private final Job weeklyMonthlyRankingJob;

    public JobTriggerRunner(JobLauncher jobLauncher,
                            @Qualifier("weeklyMonthlyRankingJob") Job weeklyMonthlyRankingJob) {
        this.jobLauncher = jobLauncher;
        this.weeklyMonthlyRankingJob = weeklyMonthlyRankingJob;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> jobNames = args.getOptionValues("spring.batch.job.name");
        if (jobNames == null || jobNames.isEmpty()) {
            log.info("No spring.batch.job.name provided. Skipping explicit launch.");
            return;
        }

        String targetJob = jobNames.get(0);
        if (!"weeklyMonthlyRankingJob".equals(targetJob)) {
            log.info("Job name '{}' does not match 'weeklyMonthlyRankingJob'. Skipping explicit launch.", targetJob);
            return;
        }

        String targetDate = LocalDate.now().minusDays(1).toString();
        List<String> dateArgs = args.getOptionValues("targetDate");
        if (dateArgs != null && !dateArgs.isEmpty()) {
            targetDate = dateArgs.get(0);
        }

        JobParameters params = new JobParametersBuilder()
                .addString("targetDate", targetDate)
                // 동일 파라미터로 재실행 가능하도록 unique 파라미터 추가
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        log.info("Explicitly launching job '{}' with targetDate={} ...", targetJob, targetDate);
        jobLauncher.run(weeklyMonthlyRankingJob, params);
        log.info("Job '{}' launched.", targetJob);
    }
}
