-- Adds the INVENTORY reason to inventory check records.
-- Run in Oracle SQL Developer using F5 (Run Script).
DECLARE
  v_column_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_column_count
  FROM user_tab_columns
  WHERE table_name = 'INVENTORY_CHECK' AND column_name = 'REASON_MESSAGE';

  IF v_column_count = 0 THEN
    EXECUTE IMMEDIATE
      'ALTER TABLE inventory_check ADD (reason_message VARCHAR2(20) DEFAULT ''INVENTORY'' NOT NULL)';
  END IF;
END;
/
