-- Run after starting the application with the inventory-order entities deployed.
-- Copies existing inventory checks into one-line inventory orders without deleting
-- the original check records or changing their stock-adjustment references.
DECLARE
  v_order_id inventory_order.id%TYPE;
BEGIN
  FOR check_row IN (
    SELECT c.id, c.load_unit_id, c.load_unit_barcode, c.load_unit_type,
           c.scanned_at, l.name AS location_name
    FROM inventory_check c
    LEFT JOIN load_unit lu ON lu.id = c.load_unit_id
    LEFT JOIN location l ON l.id = lu.location_id
    WHERE c.reason_message = 'INVENTORY'
      AND NOT EXISTS (
        SELECT 1
        FROM inventory_order_line line
        WHERE line.inventory_check_id = c.id
      )
    ORDER BY c.id
  ) LOOP
    INSERT INTO inventory_order (created_at, reason_message)
    VALUES (check_row.scanned_at, 'INVENTORY')
    RETURNING id INTO v_order_id;

    INSERT INTO inventory_order_line (
      inventory_order_id, load_unit_id, load_unit_barcode, load_unit_type,
      location_name, completed_at, inventory_check_id
    ) VALUES (
      v_order_id, check_row.load_unit_id, check_row.load_unit_barcode,
      check_row.load_unit_type, check_row.location_name, check_row.scanned_at,
      check_row.id
    );
  END LOOP;

  UPDATE stock_adjustment adjustment
  SET inventory_order_line_id = (
    SELECT line.id
    FROM inventory_order_line line
    WHERE line.inventory_check_id = adjustment.inventory_check_id
  )
  WHERE adjustment.reason_message = 'INVENTORY'
    AND adjustment.inventory_order_line_id IS NULL
    AND EXISTS (
      SELECT 1
      FROM inventory_order_line line
      WHERE line.inventory_check_id = adjustment.inventory_check_id
    );
END;
/
