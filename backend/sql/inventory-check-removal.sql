-- Run after inventory-order-history.sql and before deploying the entity changes.
-- Existing checks and their stock adjustments are preserved as inventory orders
-- and order lines; this removes only the redundant check table and foreign keys.
DECLARE
  v_constraint_name VARCHAR2(128);
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'INVENTORY_CHECK';
  IF v_count > 0 THEN
    FOR fk IN (
      SELECT c.table_name, c.constraint_name
      FROM user_constraints c
      WHERE c.constraint_type = 'R'
        AND c.table_name IN ('STOCK_ADJUSTMENT', 'INVENTORY_ORDER_LINE')
        AND EXISTS (
          SELECT 1 FROM user_cons_columns cc
          WHERE cc.table_name = c.table_name
            AND cc.constraint_name = c.constraint_name
            AND cc.column_name = 'INVENTORY_CHECK_ID'
        )
    ) LOOP
      EXECUTE IMMEDIATE 'ALTER TABLE ' || fk.table_name ||
        ' DROP CONSTRAINT ' || fk.constraint_name;
    END LOOP;

    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'STOCK_ADJUSTMENT' AND column_name = 'INVENTORY_CHECK_ID';
    IF v_count > 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE stock_adjustment DROP COLUMN inventory_check_id';
    END IF;

    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'STOCK_ADJUSTMENT' AND column_name = 'INVENTORY_ORDER_LINE_ID';
    IF v_count > 0 THEN
      SELECT COUNT(*) INTO v_count FROM stock_adjustment WHERE inventory_order_line_id IS NULL;
      IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE stock_adjustment MODIFY (inventory_order_line_id NOT NULL)';
      END IF;
    END IF;

    SELECT COUNT(*) INTO v_count FROM user_tab_columns
    WHERE table_name = 'INVENTORY_ORDER_LINE' AND column_name = 'INVENTORY_CHECK_ID';
    IF v_count > 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE inventory_order_line DROP COLUMN inventory_check_id';
    END IF;

    EXECUTE IMMEDIATE 'DROP TABLE inventory_check CASCADE CONSTRAINTS PURGE';
  END IF;
END;
/
