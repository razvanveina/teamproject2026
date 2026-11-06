-- Picking test data. Deletes ALL warehouse data, then creates a small, clear data set.
-- Login accounts and users are NOT touched.
-- SQL Developer: select everything and press F5 (Run Script). It ends with COMMIT.
-- It can be run again any time: it always starts by deleting everything.

-- 1. Delete everything (children first)
UPDATE stock_item SET reservation_id = NULL;
DELETE FROM reservation;
DELETE FROM stock_item;
DELETE FROM retrieval_order_line;
DELETE FROM retrieval_order;
DELETE FROM receiving_order_line;
DELETE FROM receiving_order;
DELETE FROM transport_order;
UPDATE location SET load_unit_id = NULL;
DELETE FROM load_unit;
DELETE FROM article_picking_location;
DELETE FROM article_packaging_unit_type;
DELETE FROM packaging_unit_type;
DELETE FROM article;
DELETE FROM destination_to_gate;
DELETE FROM destination;
DELETE FROM location;

-- 2. Articles
INSERT INTO article (barcode, description) VALUES ('1001', 'Apples');
INSERT INTO article (barcode, description) VALUES ('1002', 'Pears');
INSERT INTO article (barcode, description) VALUES ('1003', 'Milk');
INSERT INTO article (barcode, description) VALUES ('1004', 'Bread');
INSERT INTO article (barcode, description) VALUES ('1005', 'Water');
INSERT INTO article (barcode, description) VALUES ('1006', 'Coffee');
INSERT INTO article (barcode, description) VALUES ('1007', 'Sugar');
INSERT INTO article (barcode, description) VALUES ('1008', 'Rice');

-- 3. Packaging types (generic) and which packaging each article uses.
--    P + article barcode = the article's box; the article barcode itself = a single piece (like receiving).
INSERT INTO packaging_unit_type (name, quantity) VALUES ('Piece', 1);
INSERT INTO packaging_unit_type (name, quantity) VALUES ('Box 10', 10);
INSERT INTO packaging_unit_type (name, quantity) VALUES ('Case 6', 6);
INSERT INTO packaging_unit_type (name, quantity) VALUES ('Crate 5', 5);
INSERT INTO packaging_unit_type (name, quantity) VALUES ('Box 12', 12);
INSERT INTO packaging_unit_type (name, quantity) VALUES ('Box 4', 4);
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) VALUES ('P1001', (SELECT id FROM article WHERE barcode = '1001'), (SELECT id FROM packaging_unit_type WHERE name = 'Box 10'));
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) VALUES ('P1002', (SELECT id FROM article WHERE barcode = '1002'), (SELECT id FROM packaging_unit_type WHERE name = 'Box 10'));
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) VALUES ('P1003', (SELECT id FROM article WHERE barcode = '1003'), (SELECT id FROM packaging_unit_type WHERE name = 'Case 6'));
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) VALUES ('P1004', (SELECT id FROM article WHERE barcode = '1004'), (SELECT id FROM packaging_unit_type WHERE name = 'Crate 5'));
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) VALUES ('P1005', (SELECT id FROM article WHERE barcode = '1005'), (SELECT id FROM packaging_unit_type WHERE name = 'Case 6'));
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) VALUES ('P1006', (SELECT id FROM article WHERE barcode = '1006'), (SELECT id FROM packaging_unit_type WHERE name = 'Box 12'));
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) VALUES ('P1007', (SELECT id FROM article WHERE barcode = '1007'), (SELECT id FROM packaging_unit_type WHERE name = 'Crate 5'));
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) VALUES ('P1008', (SELECT id FROM article WHERE barcode = '1008'), (SELECT id FROM packaging_unit_type WHERE name = 'Box 4'));
INSERT INTO article_packaging_unit_type (barcode, article_id, packaging_unit_type_id) SELECT barcode, id, (SELECT id FROM packaging_unit_type WHERE name = 'Piece') FROM article;

-- 4. Destinations (cities) and which gate serves each city
INSERT INTO destination (name) VALUES ('Bucharest');
INSERT INTO destination (name) VALUES ('Cluj-Napoca');
INSERT INTO destination (name) VALUES ('Timisoara');
INSERT INTO destination (name) VALUES ('Iasi');

-- 5. Locations: transit (IN, gates), storage in the high bay warehouse, picking places
INSERT INTO location (dtype, name) VALUES ('TRANSIT', 'IN');
INSERT INTO location (dtype, name) VALUES ('TRANSIT', 'OUT');
INSERT INTO location (dtype, name) VALUES ('TRANSIT', 'GATE01');
INSERT INTO location (dtype, name) VALUES ('TRANSIT', 'GATE02');
INSERT INTO location (dtype, name) VALUES ('TRANSIT', 'GATE03');
INSERT INTO location (dtype, name) VALUES ('TRANSIT', 'GATE04');
INSERT INTO location (dtype, name) VALUES ('TRANSIT', 'DECANTING');
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'HBW010010101', 1, 1, 1, 1);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'HBW010010102', 1, 1, 1, 2);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'HBW010020101', 1, 2, 1, 1);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'HBW010020102', 1, 2, 1, 2);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'PICK00101', 0, 1, 1, 0);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'PICK00102', 0, 1, 2, 0);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'PICK00103', 0, 1, 3, 0);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'PICK00104', 0, 1, 4, 0);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'PICK00105', 0, 1, 5, 0);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'PICK00201', 0, 2, 1, 0);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'PICK00202', 0, 2, 2, 0);
INSERT INTO location (dtype, name, aisle, x, y, z) VALUES ('STORAGE', 'PICK00203', 0, 2, 3, 0);

INSERT INTO destination_to_gate (destination_id, gate_id) VALUES ((SELECT id FROM destination WHERE name = 'Bucharest'), (SELECT id FROM location WHERE name = 'GATE01'));
INSERT INTO destination_to_gate (destination_id, gate_id) VALUES ((SELECT id FROM destination WHERE name = 'Cluj-Napoca'), (SELECT id FROM location WHERE name = 'GATE02'));
INSERT INTO destination_to_gate (destination_id, gate_id) VALUES ((SELECT id FROM destination WHERE name = 'Timisoara'), (SELECT id FROM location WHERE name = 'GATE03'));
INSERT INTO destination_to_gate (destination_id, gate_id) VALUES ((SELECT id FROM destination WHERE name = 'Iasi'), (SELECT id FROM location WHERE name = 'GATE04'));

-- 6. Load units: one virtual pallet in every picking place (barcode V + place name), real pallets in storage,
--    and two totes in the decanting zone DECANTING (one article per tote, like the decanting app makes them)
INSERT INTO load_unit (barcode, type, location_id) SELECT 'V' || name, 'PALLET', id FROM location WHERE name LIKE 'PICK%';
INSERT INTO load_unit (barcode, type, location_id) VALUES ('10000001', 'PALLET', (SELECT id FROM location WHERE name = 'HBW010010101'));
INSERT INTO load_unit (barcode, type, location_id) VALUES ('10000002', 'PALLET', (SELECT id FROM location WHERE name = 'HBW010010102'));
INSERT INTO load_unit (barcode, type, location_id) VALUES ('10000003', 'PALLET', (SELECT id FROM location WHERE name = 'HBW010020101'));
INSERT INTO load_unit (barcode, type, location_id) VALUES ('T0001', 'TOTE', (SELECT id FROM location WHERE name = 'DECANTING'));
INSERT INTO load_unit (barcode, type, location_id) VALUES ('T0002', 'TOTE', (SELECT id FROM location WHERE name = 'DECANTING'));
--    only the storage places hold a pallet; picking places stay free, so totes can be transported there
UPDATE location l SET load_unit_id = (SELECT id FROM load_unit u WHERE u.location_id = l.id) WHERE name LIKE 'HBW%';

--    which picking place belongs to which article (used by the tote transport)
INSERT INTO article_picking_location (article_id, location_id) VALUES ((SELECT id FROM article WHERE barcode = '1001'), (SELECT id FROM location WHERE name = 'PICK00101'));
INSERT INTO article_picking_location (article_id, location_id) VALUES ((SELECT id FROM article WHERE barcode = '1002'), (SELECT id FROM location WHERE name = 'PICK00102'));
INSERT INTO article_picking_location (article_id, location_id) VALUES ((SELECT id FROM article WHERE barcode = '1003'), (SELECT id FROM location WHERE name = 'PICK00103'));
INSERT INTO article_picking_location (article_id, location_id) VALUES ((SELECT id FROM article WHERE barcode = '1004'), (SELECT id FROM location WHERE name = 'PICK00104'));
INSERT INTO article_picking_location (article_id, location_id) VALUES ((SELECT id FROM article WHERE barcode = '1006'), (SELECT id FROM location WHERE name = 'PICK00105'));
INSERT INTO article_picking_location (article_id, location_id) VALUES ((SELECT id FROM article WHERE barcode = '1007'), (SELECT id FROM location WHERE name = 'PICK00201'));
INSERT INTO article_picking_location (article_id, location_id) VALUES ((SELECT id FROM article WHERE barcode = '1008'), (SELECT id FROM location WHERE name = 'PICK00202'));
INSERT INTO article_picking_location (article_id, location_id) VALUES ((SELECT id FROM article WHERE barcode = '1005'), (SELECT id FROM location WHERE name = 'PICK00203'));

-- 7. Retrieval orders (oldest first), with the statuses the ReservationService uses:
--    NEW -> ACTIVE (stock reserved by the ReservationService, lines ACTIVE) -> STARTED (box scanned) -> PICKED (box closed).
--    Line quantities are in pieces. An ACTIVE order is ready for picking only when ALL its reserved stock is in the picking zone.
--    SO-1001 ACTIVE:  Apples 10 (picking) + Water 6 (still in storage)                -> NOT ready
--    SO-1002 ACTIVE:  Apples 12 = 1 box of 10 + 2 pieces, Pears 5 = 5 pieces           -> ready  <- the station must pick this one
--    SO-1003 ACTIVE:  Milk 14 = 2 cases of 6 + 2 pieces, Coffee 12 = 1 box, Bread 2   -> ready (next one)
--    SO-1004 ACTIVE:  Sugar 10 = 1 crate in picking + 1 crate still in storage        -> NOT ready
--    SO-1005 NEW:     Rice 4, no reservation yet; the ReservationService will reserve it from storage -> NOT ready
--    SO-1006 ACTIVE:  Rice 8 = 2 boxes of 4 in tote T0001 at DECANTING -> NOT ready until the tote transport takes T0001 to PICK00202
INSERT INTO retrieval_order (order_id, status, destination_id) VALUES ('SO-1001', 'ACTIVE', (SELECT id FROM destination WHERE name = 'Bucharest'));
INSERT INTO retrieval_order (order_id, status, destination_id) VALUES ('SO-1002', 'ACTIVE', (SELECT id FROM destination WHERE name = 'Cluj-Napoca'));
INSERT INTO retrieval_order (order_id, status, destination_id) VALUES ('SO-1003', 'ACTIVE', (SELECT id FROM destination WHERE name = 'Timisoara'));
INSERT INTO retrieval_order (order_id, status, destination_id) VALUES ('SO-1004', 'ACTIVE', (SELECT id FROM destination WHERE name = 'Iasi'));
INSERT INTO retrieval_order (order_id, status, destination_id) VALUES ('SO-1005', 'NEW', (SELECT id FROM destination WHERE name = 'Bucharest'));
INSERT INTO retrieval_order (order_id, status, destination_id) VALUES ('SO-1006', 'ACTIVE', (SELECT id FROM destination WHERE name = 'Iasi'));

INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1001'), (SELECT id FROM article WHERE barcode = '1001'), 10, 'ACTIVE');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1001'), (SELECT id FROM article WHERE barcode = '1005'), 6, 'ACTIVE');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1002'), (SELECT id FROM article WHERE barcode = '1001'), 12, 'ACTIVE');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1002'), (SELECT id FROM article WHERE barcode = '1002'), 5, 'ACTIVE');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1003'), (SELECT id FROM article WHERE barcode = '1003'), 14, 'ACTIVE');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1003'), (SELECT id FROM article WHERE barcode = '1006'), 12, 'ACTIVE');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1003'), (SELECT id FROM article WHERE barcode = '1004'), 2, 'ACTIVE');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1004'), (SELECT id FROM article WHERE barcode = '1007'), 10, 'ACTIVE');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1005'), (SELECT id FROM article WHERE barcode = '1008'), 4, 'NEW');
INSERT INTO retrieval_order_line (order_id, article_id, quantity, status) VALUES ((SELECT id FROM retrieval_order WHERE order_id = 'SO-1006'), (SELECT id FROM article WHERE barcode = '1008'), 8, 'ACTIVE');

-- 8. Stock. stock_item.article_packaging_unit_type_id points to article_packaging_unit_type, quantity = number of packs (a box of 6 with quantity 1 = 6 pieces).
--    Every reserved stock item is followed by its reservation (MAX(id) = the stock item just inserted).
--    Free stock
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1001'), 2, (SELECT id FROM load_unit WHERE barcode = 'VPICK00101'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = '1002'), 10, (SELECT id FROM load_unit WHERE barcode = 'VPICK00102'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1003'), 3, (SELECT id FROM load_unit WHERE barcode = 'VPICK00103'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = '1004'), 8, (SELECT id FROM load_unit WHERE barcode = 'VPICK00104'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1006'), 1, (SELECT id FROM load_unit WHERE barcode = 'VPICK00105'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1005'), 19, (SELECT id FROM load_unit WHERE barcode = '10000001'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1008'), 10, (SELECT id FROM load_unit WHERE barcode = '10000002'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1007'), 9, (SELECT id FROM load_unit WHERE barcode = '10000003'));
--    SO-1001: Apples 1 box of 10 in picking, Water 1 case of 6 in storage
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1001'), 1, (SELECT id FROM load_unit WHERE barcode = 'VPICK00101'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1001' AND a.barcode = '1001'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1005'), 1, (SELECT id FROM load_unit WHERE barcode = '10000001'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1001' AND a.barcode = '1005'));
--    SO-1002: Apples 1 box of 10 + 2 pieces, Pears 5 pieces, all in picking
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1001'), 1, (SELECT id FROM load_unit WHERE barcode = 'VPICK00101'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1002' AND a.barcode = '1001'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = '1001'), 2, (SELECT id FROM load_unit WHERE barcode = 'VPICK00101'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1002' AND a.barcode = '1001'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = '1002'), 5, (SELECT id FROM load_unit WHERE barcode = 'VPICK00102'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1002' AND a.barcode = '1002'));
--    SO-1003: Milk 2 cases of 6 + 2 pieces, Coffee 1 box of 12, Bread 2 pieces, all in picking
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1003'), 2, (SELECT id FROM load_unit WHERE barcode = 'VPICK00103'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1003' AND a.barcode = '1003'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = '1003'), 2, (SELECT id FROM load_unit WHERE barcode = 'VPICK00103'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1003' AND a.barcode = '1003'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1006'), 1, (SELECT id FROM load_unit WHERE barcode = 'VPICK00105'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1003' AND a.barcode = '1006'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = '1004'), 2, (SELECT id FROM load_unit WHERE barcode = 'VPICK00104'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1003' AND a.barcode = '1004'));
--    SO-1004: Sugar 1 crate of 5 in picking + 1 crate of 5 in storage
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1007'), 1, (SELECT id FROM load_unit WHERE barcode = 'VPICK00201'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1004' AND a.barcode = '1007'));
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1007'), 1, (SELECT id FROM load_unit WHERE barcode = '10000003'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1004' AND a.barcode = '1007'));
--    SO-1006: Rice 2 boxes of 4 in tote T0001 (decanting zone), reserved
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1008'), 2, (SELECT id FROM load_unit WHERE barcode = 'T0001'));
INSERT INTO reservation (stock_item_id, retrieval_order_line_id) VALUES ((SELECT MAX(id) FROM stock_item), (SELECT l.id FROM retrieval_order_line l JOIN retrieval_order o ON o.id = l.order_id JOIN article a ON a.id = l.article_id WHERE o.order_id = 'SO-1006' AND a.barcode = '1008'));
--    Tote T0002: Coffee 2 boxes of 12, not reserved -> the tote transport leaves it in the decanting zone
INSERT INTO stock_item (article_packaging_unit_type_id, quantity, load_unit_id) VALUES ((SELECT id FROM article_packaging_unit_type WHERE barcode = 'P1006'), 2, (SELECT id FROM load_unit WHERE barcode = 'T0002'));
UPDATE stock_item SET reservation_id = (SELECT r.id FROM reservation r WHERE r.stock_item_id = stock_item.id) WHERE id IN (SELECT stock_item_id FROM reservation);

-- 9. One receiving order, so the receiving screen has something too
INSERT INTO receiving_order (order_id, status) VALUES ('PO-2001', 'NEW');
INSERT INTO receiving_order_line (order_id, article_id, quantity, received_quantity, status) VALUES ((SELECT id FROM receiving_order WHERE order_id = 'PO-2001'), (SELECT id FROM article WHERE barcode = '1005'), 60, 0, 'NEW');

COMMIT;
