-- Run once against the Oracle schema before starting the updated backend.

ALTER TABLE inventory_order ADD (code VARCHAR2(100));
UPDATE inventory_order SET code = 'IO-' || id WHERE code IS NULL;
ALTER TABLE inventory_order MODIFY (code NOT NULL);
ALTER TABLE inventory_order ADD CONSTRAINT uk_inventory_order_code UNIQUE (code);
ALTER TABLE inventory_order MODIFY (created_at DEFAULT SYSDATE);
ALTER TABLE inventory_order MODIFY (status DEFAULT 'STARTED');
ALTER TABLE inventory_order MODIFY (reason_message DEFAULT 'INVENTORY');

ALTER TABLE inventory_order_line ADD (location_id NUMBER(10));
UPDATE inventory_order_line line
SET location_id = (
    SELECT load_unit.location_id
    FROM load_unit
    WHERE load_unit.id = line.load_unit_id
)
WHERE line.load_unit_id IS NOT NULL;
ALTER TABLE inventory_order_line ADD CONSTRAINT fk_inventory_line_location
    FOREIGN KEY (location_id) REFERENCES location(id);
ALTER TABLE inventory_order_line MODIFY (location_id NOT NULL);
ALTER TABLE inventory_order_line MODIFY (load_unit_barcode NULL);
ALTER TABLE inventory_order_line MODIFY (status DEFAULT 'NEW');
ALTER TABLE inventory_order_line MODIFY (stock_adjustments_calculated DEFAULT 0);
ALTER TABLE inventory_order_line MODIFY (stock_updated DEFAULT 0);
