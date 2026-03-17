-- =============================================================
-- PostgreSQL 11.5  初始化脚本
-- 执行顺序：
--   Step 1  以 postgres 超级用户连接，执行 Step1~Step3
--   Step 2  切换到 demo_db 数据库，执行 Step4
-- =============================================================

-- -------------------------------------------------------------
-- Step 1: 创建专属用户（role）
-- -------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'demo_user') THEN
        CREATE ROLE demo_user LOGIN PASSWORD 'Demo@2026!';
        RAISE NOTICE 'Role demo_user created.';
    ELSE
        RAISE NOTICE 'Role demo_user already exists, skip create.';
    END IF;
END
$$;

-- 如需修改密码，执行下面这行（取消注释）：
-- ALTER ROLE demo_user WITH PASSWORD 'NewPassword@2026!';

-- -------------------------------------------------------------
-- Step 2: 创建数据库，指定 owner
-- -------------------------------------------------------------
-- 注意：CREATE DATABASE 不能在事务块中执行，需单独运行。
-- 若已存在同名库，该语句会报错，跳过即可。
CREATE DATABASE demo_db
    WITH OWNER     = demo_user
         ENCODING  = 'UTF8'
         LC_COLLATE = 'en_US.UTF-8'
         LC_CTYPE   = 'en_US.UTF-8'
         TEMPLATE  = template0
         CONNECTION LIMIT = -1;

COMMENT ON DATABASE demo_db IS '批量更新演示库';

-- -------------------------------------------------------------
-- Step 3: 授权（在 postgres 库下执行）
-- -------------------------------------------------------------
-- 收回 public schema 上其他用户的默认权限，再按需赋予
REVOKE ALL ON DATABASE demo_db FROM PUBLIC;
GRANT CONNECT ON DATABASE demo_db TO demo_user;
GRANT ALL PRIVILEGES ON DATABASE demo_db TO demo_user;

-- =============================================================
-- !! 以下内容须切换到 demo_db 数据库后执行 !!
--    psql -h 172.19.80.56 -U demo_user -d demo_db
-- =============================================================

-- -------------------------------------------------------------
-- Step 4: Schema 授权（连接 demo_db 后执行）
-- -------------------------------------------------------------
GRANT ALL ON SCHEMA public TO demo_user;

-- -------------------------------------------------------------
-- Step 5: 创建序列（替代 MySQL AUTO_INCREMENT）
-- -------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS order_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- -------------------------------------------------------------
-- Step 6: 建表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_item (
    id          BIGINT        NOT NULL DEFAULT nextval('order_item_id_seq') ,
    order_no    VARCHAR(64)   NOT NULL,
    sku_code    VARCHAR(64)   NOT NULL,
    quantity    INT           NOT NULL DEFAULT 0,
    price       NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    -- 0-待处理 1-处理中 2-完成 3-取消
    status      SMALLINT      NOT NULL DEFAULT 0,
    remark      VARCHAR(256)  DEFAULT NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_order_item PRIMARY KEY (id)
);

-- 序列归属绑定（DROP TABLE 时联动删除序列）
ALTER SEQUENCE order_item_id_seq OWNED BY order_item.id;

-- -------------------------------------------------------------
-- Step 7: 注释
-- -------------------------------------------------------------
COMMENT ON TABLE  order_item             IS '订单明细';
COMMENT ON COLUMN order_item.id          IS '主键';
COMMENT ON COLUMN order_item.order_no    IS '订单号';
COMMENT ON COLUMN order_item.sku_code    IS 'SKU编码';
COMMENT ON COLUMN order_item.quantity    IS '数量';
COMMENT ON COLUMN order_item.price       IS '单价';
COMMENT ON COLUMN order_item.status      IS '状态: 0-待处理 1-处理中 2-完成 3-取消';
COMMENT ON COLUMN order_item.remark      IS '备注';
COMMENT ON COLUMN order_item.create_time IS '创建时间';
COMMENT ON COLUMN order_item.update_time IS '更新时间';

-- -------------------------------------------------------------
-- Step 8: 索引
-- -------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_order_item_order_no ON order_item (order_no);

-- -------------------------------------------------------------
-- Step 9: 把表/序列权限授给 demo_user
-- -------------------------------------------------------------
GRANT ALL PRIVILEGES ON TABLE    order_item         TO demo_user;
GRANT ALL PRIVILEGES ON SEQUENCE order_item_id_seq  TO demo_user;

-- -------------------------------------------------------------
-- Step 10: 自动更新 update_time 触发器
--   PostgreSQL 没有 ON UPDATE CURRENT_TIMESTAMP，需用触发器实现
-- -------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_order_item_update_time ON order_item;
CREATE TRIGGER trg_order_item_update_time
    BEFORE UPDATE ON order_item
    FOR EACH ROW
    EXECUTE PROCEDURE fn_update_time();

-- =============================================================
-- 验证
-- =============================================================
-- SELECT * FROM information_schema.tables WHERE table_name = 'order_item';
-- \d order_item
