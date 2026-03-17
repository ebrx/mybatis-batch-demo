package com.example.demo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单明细实体
 */
@Data
@Accessors(chain = true)
public class OrderItem {

    private Long id;

    private String orderNo;

    private String skuCode;

    private Integer quantity;

    private BigDecimal price;

    /** 0-待处理 1-处理中 2-完成 3-取消 */
    private Integer status;

    private String remark;

    private Date createTime;

    private Date updateTime;
}
