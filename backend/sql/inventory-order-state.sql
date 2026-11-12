-- Run against an existing database with inventory_order tables before deploying the
-- STARTED/FINISHED order flow. Existing orders came from completed checks.
-- Skip this script for fresh installations; Hibernate creates the columns and tables.
DECLARE
  v_column_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_column_count
  FROM user_tab_columns
  WHERE table_name = 'INVENTORY_ORDER' AND column_name = 'STATUS';

  IF v_column_count = 0 THEN
    EXECUTE IMMEDIATE
      'ALTER TABLE inventory_order ADD (status VARCHAR2(20) DEFAULT ''FINISHED'' NOT NULL)';
  END IF;

  SELECT COUNT(*) INTO v_column_count
  FROM user_tab_columns
  WHERE table_name = 'INVENTORY_ORDER_LINE' AND column_name = 'STATUS';

  IF v_column_count = 0 THEN
    EXECUTE IMMEDIATE
      'ALTER TABLE inventory_order_line ADD (status VARCHAR2(20) DEFAULT ''FINISHED'' NOT NULL)';
  END IF;

  SELECT COUNT(*) INTO v_column_count
  FROM user_tab_columns
  WHERE table_name = 'INVENTORY_ORDER_LINE'
    AND column_name = 'COMPLETED_AT'
    AND nullable = 'N';
  IF v_column_count > 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE inventory_order_line MODIFY (completed_at NULL)';
  END IF;

  SELECT COUNT(*) INTO v_column_count
  FROM user_tab_columns
  WHERE table_name = 'INVENTORY_ORDER' AND column_name = 'FINISHED_AT';

  IF v_column_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE inventory_order ADD (finished_at TIMESTAMP NULL)';
  END IF;

  UPDATE inventory_order o
  SET finished_at = NVL((SELECT MAX(line.completed_at) FROM inventory_order_line line
                         WHERE line.inventory_order_id = o.id), o.created_at)
  WHERE o.status = 'FINISHED' AND o.finished_at IS NULL;

  SELECT COUNT(*) INTO v_column_count
  FROM user_tab_columns
  WHERE table_name = 'INVENTORY_ORDER_LINE' AND column_name = 'STOCK_ADJUSTMENTS_CALCULATED';

  IF v_column_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE inventory_order_line ADD (stock_adjustments_calculated NUMBER(1) DEFAULT 0 NOT NULL)';
  END IF;
END;
/
