package com.example.demo;

import com.example.demo.entity.OrderItem;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.service.OrderItemService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class OrderItemMapperTest {

    @Autowired
    private OrderItemMapper mapper;

    @Autowired
    private OrderItemService service;

    // ----------------------------------------------------------------
    // 方式一：BatchExecutor
    // ----------------------------------------------------------------

    @Test
    //@Transactional
    void testBatchUpdateByExecutor() {
        List<Long> ids = insertTestData(5);

        List<OrderItem> updates = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            updates.add(new OrderItem()
                    .setId(ids.get(i))
                    .setStatus(1)
                    .setQuantity(100 + i)
                    .setRemark("executor-updated-" + i));
        }

        // BatchExecutor 在当前事务下执行，@Transactional 会自动回滚（测试环境）
        service.batchUpdateByExecutor(updates);

        // 验证：逐条查询确认更新生效
        for (int i = 0; i < ids.size(); i++) {
            OrderItem item = mapper.selectById(ids.get(i));
            Assertions.assertEquals(1, item.getStatus());
            Assertions.assertEquals(100 + i, item.getQuantity());
            System.out.printf("id=%d, status=%d, quantity=%d, remark=%s%n",
                    item.getId(), item.getStatus(), item.getQuantity(), item.getRemark());
        }
    }

    // ----------------------------------------------------------------
    // 方式二：CASE WHEN
    // ----------------------------------------------------------------

    @Test
    //@Transactional
    void testBatchUpdateByCaseWhen() {
        List<Long> ids = insertTestData(5);

        List<OrderItem> updates = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            updates.add(new OrderItem()
                    .setId(ids.get(i))
                    .setStatus(2)
                    .setQuantity(200 + i)
                    .setPrice(new BigDecimal("9.99"))
                    .setRemark("case-when-updated-" + i));
        }

        int affected = service.batchUpdateByCaseWhen(updates);
        System.out.println("case-when affected: " + affected);
        Assertions.assertEquals(5, affected);

        for (int i = 0; i < ids.size(); i++) {
            OrderItem item = mapper.selectById(ids.get(i));
            Assertions.assertEquals(2, item.getStatus());
            Assertions.assertEquals(200 + i, item.getQuantity());
            System.out.printf("id=%d, status=%d, quantity=%d, price=%s%n",
                    item.getId(), item.getStatus(), item.getQuantity(), item.getPrice());
        }
    }

    // ----------------------------------------------------------------
    // 方式三：ON CONFLICT upsert
    // ----------------------------------------------------------------

    @Test
    // @Transactional
    void testBatchUpsert() {
        List<Long> ids = insertTestData(3);

        List<OrderItem> upserts = new ArrayList<>();
        // 更新已有行
        for (Long id : ids) {
            upserts.add(new OrderItem()
                    .setId(id)
                    .setOrderNo("ORD-UPSERT")
                    .setSkuCode("SKU-UPSERT")
                    .setQuantity(999)
                    .setPrice(new BigDecimal("1.00"))
                    .setStatus(3)
                    .setRemark("upserted"));
        }
        // 插入新行（无 id，由序列生成）
        upserts.add(new OrderItem()
                .setOrderNo("ORD-NEW")
                .setSkuCode("SKU-NEW")
                .setQuantity(1)
                .setPrice(new BigDecimal("0.01"))
                .setStatus(0)
                .setRemark("new row via upsert"));

        int affected = service.batchUpsert(upserts);
        System.out.println("upsert affected: " + affected);
        Assertions.assertTrue(affected > 0);

        // 验证已有行被更新
        for (Long id : ids) {
            OrderItem item = mapper.selectById(id);
            Assertions.assertEquals(999, item.getQuantity());
            Assertions.assertEquals(3, item.getStatus());
            System.out.printf("id=%d, status=%d, quantity=%d%n",
                    item.getId(), item.getStatus(), item.getQuantity());
        }
    }

    // ----------------------------------------------------------------
    // 辅助：批量插入测试数据，返回生成的 id 列表
    // ----------------------------------------------------------------
    private List<Long> insertTestData(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            OrderItem item = new OrderItem()
                    .setOrderNo("ORD-TEST-" + i)
                    .setSkuCode("SKU-00" + i)
                    .setQuantity(10 + i)
                    .setPrice(new BigDecimal("19.90"))
                    .setStatus(0)
                    .setRemark("init");
            mapper.insert(item);
            Assertions.assertNotNull(item.getId(), "insert 后 id 必须回填");
            ids.add(item.getId());
            System.out.println("inserted id: " + item.getId());
        }
        return ids;
    }
}
