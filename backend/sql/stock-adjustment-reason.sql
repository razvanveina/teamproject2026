-- Ensures stock adjustments created by inventory checks have the INVENTORY reason.
-- Run in Oracle SQL Developer using F5 (Run Script).
DECLARE
  v_column_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_column_count
  FROM user_tab_columns
  WHERE table_name = 'STOCK_ADJUSTMENT' AND column_name = 'REASON_MESSAGE';

  IF v_column_count = 0 THEN
    EXECUTE IMMEDIATE
      'ALTER TABLE stock_adjustment ADD (reason_message VARCHAR2(20) DEFAULT ''INVENTORY'' NOT NULL)';
  END IF;

  FOR c IN (
    SELECT constraint_name
    FROM user_constraints
    WHERE table_name = 'STOCK_ADJUSTMENT'
      AND constraint_type = 'C'
      AND UPPER(search_condition_vc) LIKE '%REASON_MESSAGE%'
  ) LOOP
    EXECUTE IMMEDIATE 'ALTER TABLE stock_adjustment DROP CONSTRAINT ' || c.constraint_name;
  END LOOP;

  UPDATE stock_adjustment SET reason_message = 'INVENTORY'
  WHERE reason_message IS NULL OR reason_message <> 'INVENTORY';
  COMMIT;

  EXECUTE IMMEDIATE
    'ALTER TABLE stock_adjustment MODIFY (reason_message DEFAULT ''INVENTORY'' NOT NULL)';
  EXECUTE IMMEDIATE
    'ALTER TABLE stock_adjustment ADD CONSTRAINT chk_stock_adjustment_reason ' ||
    'CHECK (reason_message = ''INVENTORY'')';
END;
/
