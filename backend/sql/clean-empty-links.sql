-- Run this BEFORE not-null-links.sql when that script says NOT DONE.
-- It deletes only the rows that cannot exist with the new mandatory links:
--   rows where a mandatory link is empty, and the rows that point to them (e.g. the lines of an order without destination).
-- Everything else stays. Articles, locations, destinations and receiving orders are never deleted.
-- SQL Developer: select everything and press F5 (Run Script). Read the result in "Script Output".
SET SERVEROUTPUT ON

DECLARE
  -- Runs one statement and prints how many rows it changed
  PROCEDURE run(p_title VARCHAR2, p_sql VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE p_sql;
    DBMS_OUTPUT.PUT_LINE(RPAD(p_title, 60) || SQL%ROWCOUNT || ' row(s)');
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -942 THEN
        DBMS_OUTPUT.PUT_LINE(RPAD(p_title, 60) || 'skipped, a table does not exist');
      ELSE
        DBMS_OUTPUT.PUT_LINE(RPAD(p_title, 60) || 'ERROR ' || SQLERRM);
      END IF;
  END;
BEGIN
  -- 1. Reservations: empty link, or pointing to a stock item / order line that will be deleted
  run('Stock items: forget reservations that will be deleted',
      'UPDATE STOCK_ITEM SET RESERVATION_ID = NULL WHERE RESERVATION_ID IN (
         SELECT ID FROM RESERVATION WHERE STOCK_ITEM_ID IS NULL OR RETRIEVAL_ORDER_LINE_ID IS NULL
           OR STOCK_ITEM_ID IN (SELECT ID FROM STOCK_ITEM WHERE ARTICLE_ID IS NULL OR LOAD_UNIT_ID IS NULL OR PACKAGING_UNIT_TYPE_ID IS NULL
                                  OR LOAD_UNIT_ID IN (SELECT ID FROM LOAD_UNIT WHERE LOCATION_ID IS NULL)
                                  OR PACKAGING_UNIT_TYPE_ID IN (SELECT ID FROM PACKAGING_UNIT_TYPE WHERE ARTICLE_ID IS NULL))
           OR RETRIEVAL_ORDER_LINE_ID IN (SELECT ID FROM RETRIEVAL_ORDER_LINE WHERE ARTICLE_ID IS NULL OR ORDER_ID IS NULL
                                  OR ORDER_ID IN (SELECT ID FROM RETRIEVAL_ORDER WHERE DESTINATION_ID IS NULL)))');
  run('Reservations deleted',
      'DELETE FROM RESERVATION WHERE STOCK_ITEM_ID IS NULL OR RETRIEVAL_ORDER_LINE_ID IS NULL
         OR STOCK_ITEM_ID IN (SELECT ID FROM STOCK_ITEM WHERE ARTICLE_ID IS NULL OR LOAD_UNIT_ID IS NULL OR PACKAGING_UNIT_TYPE_ID IS NULL
                                OR LOAD_UNIT_ID IN (SELECT ID FROM LOAD_UNIT WHERE LOCATION_ID IS NULL)
                                OR PACKAGING_UNIT_TYPE_ID IN (SELECT ID FROM PACKAGING_UNIT_TYPE WHERE ARTICLE_ID IS NULL))
         OR RETRIEVAL_ORDER_LINE_ID IN (SELECT ID FROM RETRIEVAL_ORDER_LINE WHERE ARTICLE_ID IS NULL OR ORDER_ID IS NULL
                                OR ORDER_ID IN (SELECT ID FROM RETRIEVAL_ORDER WHERE DESTINATION_ID IS NULL))');

  -- 2. Stock items: empty link, or on a pallet / packaging type that will be deleted
  run('Stock items deleted',
      'DELETE FROM STOCK_ITEM WHERE ARTICLE_ID IS NULL OR LOAD_UNIT_ID IS NULL OR PACKAGING_UNIT_TYPE_ID IS NULL
         OR LOAD_UNIT_ID IN (SELECT ID FROM LOAD_UNIT WHERE LOCATION_ID IS NULL)
         OR PACKAGING_UNIT_TYPE_ID IN (SELECT ID FROM PACKAGING_UNIT_TYPE WHERE ARTICLE_ID IS NULL)');

  -- 3. Transport orders: empty link, or for a pallet that will be deleted
  run('Transport orders deleted',
      'DELETE FROM TRANSPORT_ORDER WHERE LOAD_UNIT_ID IS NULL OR SOURCE_ID IS NULL OR TARGET_ID IS NULL
         OR LOAD_UNIT_ID IN (SELECT ID FROM LOAD_UNIT WHERE LOCATION_ID IS NULL)');

  -- 4. Retrieval order lines and retrieval orders without destination
  run('Retrieval order lines deleted',
      'DELETE FROM RETRIEVAL_ORDER_LINE WHERE ARTICLE_ID IS NULL OR ORDER_ID IS NULL
         OR ORDER_ID IN (SELECT ID FROM RETRIEVAL_ORDER WHERE DESTINATION_ID IS NULL)');
  run('Retrieval orders deleted',
      'DELETE FROM RETRIEVAL_ORDER WHERE DESTINATION_ID IS NULL');

  -- 5. Receiving order lines without article or order
  run('Receiving order lines deleted',
      'DELETE FROM RECEIVING_ORDER_LINE WHERE ARTICLE_ID IS NULL OR ORDER_ID IS NULL');

  -- 6. Packaging unit types without article
  run('Packaging unit types deleted',
      'DELETE FROM PACKAGING_UNIT_TYPE WHERE ARTICLE_ID IS NULL');

  -- 7. Pallets without location (first free the storage places that point to them)
  run('Locations: forget pallets that will be deleted',
      'UPDATE LOCATION SET LOAD_UNIT_ID = NULL WHERE LOAD_UNIT_ID IN (SELECT ID FROM LOAD_UNIT WHERE LOCATION_ID IS NULL)');
  run('Pallets deleted',
      'DELETE FROM LOAD_UNIT WHERE LOCATION_ID IS NULL');

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('Saved. Now run not-null-links.sql.');
END;
/
