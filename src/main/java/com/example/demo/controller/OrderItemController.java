package com.example.demo.controller;

import com.example.demo.entity.OrderItem;
import com.example.demo.service.OrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示用 Controller，暴露三种批量更新接口。
 */
@RestController
@RequestMapping("/api/order-items")
@RequiredArgsConstructor
public class OrderItemController {

    private final OrderItemService orderItemService;

    /**
     * 方式一：BatchExecutor
     * POST /api/order-items/batch-update/executor
     */
    @PostMapping("/batch-update/executor")
    public ResponseEntity<Map<String, Object>> batchUpdateByExecutor(
            @RequestBody List<OrderItem> items) {
        int affected = orderItemService.batchUpdateByExecutor(items);
        Map<String, Object> result = new HashMap<>();
        result.put("affected", affected);
        result.put("mode", "batch-executor");
        return ResponseEntity.ok(result);
    }

    /**
     * 方式二：CASE WHEN 单条 SQL
     * POST /api/order-items/batch-update/case-when
     */
    @PostMapping("/batch-update/case-when")
    public ResponseEntity<Map<String, Object>> batchUpdateByCaseWhen(
            @RequestBody List<OrderItem> items) {
        int affected = orderItemService.batchUpdateByCaseWhen(items);
        Map<String, Object> result = new HashMap<>();
        result.put("affected", affected);
        result.put("mode", "case-when");
        return ResponseEntity.ok(result);
    }

    /**
     * 方式三：ON CONFLICT upsert
     * POST /api/order-items/batch-upsert
     */
    @PostMapping("/batch-upsert")
    public ResponseEntity<Map<String, Object>> batchUpsert(
            @RequestBody List<OrderItem> items) {
        int affected = orderItemService.batchUpsert(items);
        Map<String, Object> result = new HashMap<>();
        result.put("affected", affected);
        result.put("mode", "on-conflict-upsert");
        return ResponseEntity.ok(result);
    }
}
