package com.example.demo.service;

import com.example.demo.entity.OrderItem;
import com.example.demo.mapper.OrderItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class OrderItemService {

    private static final int DEFAULT_BATCH_SIZE = 500;

    /** 默认 SIMPLE 模式，用于普通查询/更新 */
    private final OrderItemMapper orderItemMapper;

    /** BATCH 模式，参与 Spring 事务，用于方式一批量更新 */
    private final OrderItemMapper batchOrderItemMapper;

    public OrderItemService(
            OrderItemMapper orderItemMapper,
            @Qualifier("batchSqlSessionTemplate") SqlSessionTemplate batchSqlSessionTemplate) {
        this.orderItemMapper = orderItemMapper;
        this.batchOrderItemMapper = batchSqlSessionTemplate.getMapper(OrderItemMapper.class);
    }

    // ----------------------------------------------------------------
    // 方式一：MyBatis BatchExecutor（Spring 事务托管）
    // ----------------------------------------------------------------

    /**
     * 批量更新 — BATCH 模式 SqlSessionTemplate。
     *
     * <p>batchOrderItemMapper 底层使用 BATCH ExecutorType，
     * 循环调用时 MyBatis 执行 JDBC addBatch()；
     * 事务提交时统一 executeBatch()，减少网络往返。
     * Spring @Transactional 正常控制提交/回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateByExecutor(List<OrderItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return 0;
        }
        for (OrderItem item : items) {
            batchOrderItemMapper.updateById(item);
        }
        // SqlSessionTemplate 会在事务结束时自动 flush；
        // 也可手动 flush 以便在同一事务内立即可见。
        return items.size();
    }

    // ----------------------------------------------------------------
    // 方式二：CASE WHEN 单条 SQL
    // ----------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateByCaseWhen(List<OrderItem> items) {
        return batchExecute(items, DEFAULT_BATCH_SIZE, orderItemMapper::batchUpdateByCaseWhen);
    }

    // ----------------------------------------------------------------
    // 方式三：ON CONFLICT upsert
    // ----------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public int batchUpsert(List<OrderItem> items) {
        return batchExecute(items, DEFAULT_BATCH_SIZE, orderItemMapper::batchUpsert);
    }

    // ----------------------------------------------------------------
    // 通用分批执行器
    // ----------------------------------------------------------------

    private int batchExecute(List<OrderItem> items, int batchSize, BatchExecutor executor) {
        if (CollectionUtils.isEmpty(items)) {
            return 0;
        }
        int total = 0;
        int size = items.size();
        for (int i = 0; i < size; i += batchSize) {
            List<OrderItem> batch = items.subList(i, Math.min(i + batchSize, size));
            total += executor.execute(batch);
            log.debug("batch progress: {}/{}", Math.min(i + batchSize, size), size);
        }
        return total;
    }

    @FunctionalInterface
    private interface BatchExecutor {
        int execute(List<OrderItem> batch);
    }
}
