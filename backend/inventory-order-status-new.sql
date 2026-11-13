-- Run once if inventory-order-admin-flow.sql was already applied with PLANNED.
ALTER TABLE inventory_order_line DROP CONSTRAINT SYS_C007149;
UPDATE inventory_order_line SET status = 'NEW' WHERE status = 'PLANNED';
ALTER TABLE inventory_order_line MODIFY (status DEFAULT 'NEW');
ALTER TABLE inventory_order_line ADD CONSTRAINT ck_inventory_order_line_status
    CHECK (status IN ('NEW', 'STARTED', 'FINISHED'));
COMMIT;
