package com.loopers.batch.ranking;

import com.loopers.batch.ranking.support.PeriodUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RankingAggregationJobConfig {

    private static final Logger log = LoggerFactory.getLogger(RankingAggregationJobConfig.class);

    // -------- Common DTO --------
    public record AggregatedRow(Long productId, long likes, long orders, long views, double score) {}
    public record WeeklyRankItem(Long productId, String yearWeek, int rankNo, double score, long likes, long orders, long views) {}
    public record MonthlyRankItem(Long productId, String yearMonth, int rankNo, double score, long likes, long orders, long views) {}

    @Bean
    public Job weeklyMonthlyRankingJob(JobRepository jobRepository,
                                       Step weeklyRankingStep,
                                       Step monthlyRankingStep) {
        return new JobBuilder("weeklyMonthlyRankingJob", jobRepository)
                .start(weeklyRankingStep)
                .next(monthlyRankingStep)
                .build();
    }

    // ===== Weekly =====

    @Bean
    @JobScope
    public Step weeklyRankingStep(JobRepository jobRepository,
                                  PlatformTransactionManager txManager,
                                  JdbcCursorItemReader<AggregatedRow> weeklyAggregatedReader,
                                  ItemProcessor<AggregatedRow, WeeklyRankItem> weeklyRankingProcessor,
                                  JdbcBatchItemWriter<WeeklyRankItem> weeklyRankingWriter,
                                  StepExecutionListener weeklyCleanupListener) {
        return new StepBuilder("weeklyRankingStep", jobRepository)
                .<AggregatedRow, WeeklyRankItem>chunk(200, txManager)
                .reader(weeklyAggregatedReader)
                .processor(weeklyRankingProcessor)
                .writer(weeklyRankingWriter)
                .listener(weeklyCleanupListener)
                .faultTolerant()
                .skipLimit(0)
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<AggregatedRow> weeklyAggregatedReader(DataSource dataSource,
                                                                      @Value("#{jobParameters['targetDate']}") String targetDateStr) {
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : LocalDate.now().minusDays(1);
        var range = PeriodUtil.weekRange(targetDate);
        String sql = """
            SELECT pm.product_id AS product_id,
                   COALESCE(SUM(pm.likes_count),0)  AS likes,
                   COALESCE(SUM(pm.sales_count),0)  AS orders,
                   COALESCE(SUM(pm.views_count),0)  AS views,
                   (0.1 * COALESCE(SUM(pm.views_count),0)
                  + 0.2 * COALESCE(SUM(pm.likes_count),0)
                  + 0.7 * COALESCE(SUM(pm.sales_count),0)) AS score
              FROM product_metrics pm
             WHERE pm.metric_date BETWEEN ? AND ?
             GROUP BY pm.product_id
             ORDER BY score DESC
            """;
        RowMapper<AggregatedRow> mapper = (rs, rowNum) -> new AggregatedRow(
                rs.getLong("product_id"),
                rs.getLong("likes"),
                rs.getLong("orders"),
                rs.getLong("views"),
                rs.getDouble("score")
        );
        return new JdbcCursorItemReaderBuilder<AggregatedRow>()
                .name("weeklyAggregatedReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setDate(1, Date.valueOf(range.start()));
                    ps.setDate(2, Date.valueOf(range.end()));
                })
                .rowMapper(mapper)
                .maxRows(100) // top-100만 처리
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<AggregatedRow, WeeklyRankItem> weeklyRankingProcessor(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : LocalDate.now().minusDays(1);
        String yearWeek = PeriodUtil.yearWeek(targetDate);
        AtomicInteger rank = new AtomicInteger(0);
        return item -> new WeeklyRankItem(
                item.productId(),
                yearWeek,
                rank.incrementAndGet(),
                item.score(),
                item.likes(),
                item.orders(),
                item.views()
        );
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<WeeklyRankItem> weeklyRankingWriter(DataSource dataSource) {
        String insert = "INSERT INTO mv_product_rank_weekly (product_id, `year_week`, `rank_no`, score, like_count, order_count, view_count, created_at, updated_at) " +
                "VALUES (?,?,?,?,?,?,?, NOW(), NOW())";
        return new JdbcBatchItemWriterBuilder<WeeklyRankItem>()
                .dataSource(dataSource)
                .sql(insert)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setLong(1, item.productId());
                    ps.setString(2, item.yearWeek());
                    ps.setInt(3, item.rankNo());
                    ps.setDouble(4, item.score());
                    ps.setLong(5, item.likes());
                    ps.setLong(6, item.orders());
                    ps.setLong(7, item.views());
                })
                .build();
    }

    @Bean
    @StepScope
    public StepExecutionListener weeklyCleanupListener(JdbcTemplate jdbcTemplate,
                                                       @Value("#{jobParameters['targetDate']}") String targetDateStr) {
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : LocalDate.now().minusDays(1);
        String yearWeek = PeriodUtil.yearWeek(targetDate);
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                int deleted = jdbcTemplate.update("DELETE FROM mv_product_rank_weekly WHERE `year_week` = ?", yearWeek);
                log.info("[weeklyCleanup] Cleared {} rows for {}", deleted, yearWeek);
            }
        };
    }

    // ===== Monthly =====

    @Bean
    @JobScope
    public Step monthlyRankingStep(JobRepository jobRepository,
                                   PlatformTransactionManager txManager,
                                   JdbcCursorItemReader<AggregatedRow> monthlyAggregatedReader,
                                   ItemProcessor<AggregatedRow, MonthlyRankItem> monthlyRankingProcessor,
                                   JdbcBatchItemWriter<MonthlyRankItem> monthlyRankingWriter,
                                   StepExecutionListener monthlyCleanupListener) {
        return new StepBuilder("monthlyRankingStep", jobRepository)
                .<AggregatedRow, MonthlyRankItem>chunk(200, txManager)
                .reader(monthlyAggregatedReader)
                .processor(monthlyRankingProcessor)
                .writer(monthlyRankingWriter)
                .listener(monthlyCleanupListener)
                .faultTolerant()
                .skipLimit(0)
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<AggregatedRow> monthlyAggregatedReader(DataSource dataSource,
                                                                       @Value("#{jobParameters['targetDate']}") String targetDateStr) {
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : LocalDate.now().minusDays(1);
        var range = PeriodUtil.monthRange(targetDate);
        String sql = """
            SELECT pm.product_id AS product_id,
                   COALESCE(SUM(pm.likes_count),0)  AS likes,
                   COALESCE(SUM(pm.sales_count),0)  AS orders,
                   COALESCE(SUM(pm.views_count),0)  AS views,
                   (0.1 * COALESCE(SUM(pm.views_count),0)
                  + 0.2 * COALESCE(SUM(pm.likes_count),0)
                  + 0.7 * COALESCE(SUM(pm.sales_count),0)) AS score
              FROM product_metrics pm
             WHERE pm.metric_date BETWEEN ? AND ?
             GROUP BY pm.product_id
             ORDER BY score DESC
            """;
        RowMapper<AggregatedRow> mapper = (rs, rowNum) -> new AggregatedRow(
                rs.getLong("product_id"),
                rs.getLong("likes"),
                rs.getLong("orders"),
                rs.getLong("views"),
                rs.getDouble("score")
        );
        return new JdbcCursorItemReaderBuilder<AggregatedRow>()
                .name("monthlyAggregatedReader")
                .dataSource(dataSource)
                .sql(sql)
                .preparedStatementSetter(ps -> {
                    ps.setDate(1, Date.valueOf(range.start()));
                    ps.setDate(2, Date.valueOf(range.end()));
                })
                .rowMapper(mapper)
                .maxRows(100)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<AggregatedRow, MonthlyRankItem> monthlyRankingProcessor(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : LocalDate.now().minusDays(1);
        String yearMonth = PeriodUtil.yearMonth(targetDate);
        AtomicInteger rank = new AtomicInteger(0);
        return item -> new MonthlyRankItem(
                item.productId(),
                yearMonth,
                rank.incrementAndGet(),
                item.score(),
                item.likes(),
                item.orders(),
                item.views()
        );
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<MonthlyRankItem> monthlyRankingWriter(DataSource dataSource) {
        String insert = "INSERT INTO mv_product_rank_monthly (product_id, `year_month`, `rank_no`, score, like_count, order_count, view_count, created_at, updated_at) " +
                "VALUES (?,?,?,?,?,?,?, NOW(), NOW())";
        return new JdbcBatchItemWriterBuilder<MonthlyRankItem>()
                .dataSource(dataSource)
                .sql(insert)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setLong(1, item.productId());
                    ps.setString(2, item.yearMonth());
                    ps.setInt(3, item.rankNo());
                    ps.setDouble(4, item.score());
                    ps.setLong(5, item.likes());
                    ps.setLong(6, item.orders());
                    ps.setLong(7, item.views());
                })
                .build();
    }

    @Bean
    @StepScope
    public StepExecutionListener monthlyCleanupListener(JdbcTemplate jdbcTemplate,
                                                        @Value("#{jobParameters['targetDate']}") String targetDateStr) {
        LocalDate targetDate = targetDateStr != null ? LocalDate.parse(targetDateStr) : LocalDate.now().minusDays(1);
        String yearMonth = PeriodUtil.yearMonth(targetDate);
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                int deleted = jdbcTemplate.update("DELETE FROM mv_product_rank_monthly WHERE `year_month` = ?", yearMonth);
                log.info("[monthlyCleanup] Cleared {} rows for {}", deleted, yearMonth);
            }
        };
    }
}
