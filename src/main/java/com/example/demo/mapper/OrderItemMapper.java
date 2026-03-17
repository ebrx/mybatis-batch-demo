package com.example.demo.mapper;

import com.example.demo.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OrderItem Mapper
 *
 * <p>批量更新三种方式：
 * <ol>
 *   <li>{@link #updateById} — 单行更新，配合 Service 层 MyBatis BatchExecutor 实现批量，
 *       每条 SQL 独立、字段可选更新，PostgreSQL 原生支持。</li>
 *   <li>{@link #batchUpdateByCaseWhen} — 单条 SQL，用 CASE WHEN 按主键分支赋值，
 *       一次 IO，适合列固定、行数适中（≤ 1000）的场景。</li>
 *   <li>{@link #batchUpsert} — INSERT ... ON CONFLICT DO UPDATE，PostgreSQL upsert。</li>
 * </ol>
 */
@Mapper
public interface OrderItemMapper {

    // ----------------------------------------------------------------
    // 单行 CRUD
    // ----------------------------------------------------------------

    int insert(OrderItem item);

    OrderItem selectById(@Param("id") Long id);

    List<OrderItem> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 单行更新（供 BatchExecutor 循环调用）。
     * 字段为 null 时跳过，只更新非 null 字段。
     */
    int updateById(OrderItem item);

    // ----------------------------------------------------------------
    // 方式一：Service 层 BatchExecutor + updateById（见 OrderItemService）
    // ----------------------------------------------------------------

    // ----------------------------------------------------------------
    // 方式二：单条 CASE WHEN SQL
    // ----------------------------------------------------------------

    /**
     * 批量更新 — 单条 CASE WHEN SQL，无需多语句支持，一次数据库交互。
     *
     * @param items 待更新列表，元素必须含有效 id
     * @return 影响行数
     */
    int batchUpdateByCaseWhen(@Param("items") List<OrderItem> items);

    // ----------------------------------------------------------------
    // 方式三：INSERT ... ON CONFLICT DO UPDATE（PostgreSQL upsert）
    // ----------------------------------------------------------------

    /**
     * 批量 upsert — INSERT ... ON CONFLICT (id) DO UPDATE SET ...
     * 主键存在 → 更新；不存在 → 插入。
     *
     * @param items 待 upsert 列表
     * @return 影响行数
     */
    int batchUpsert(@Param("items") List<OrderItem> items);
}
