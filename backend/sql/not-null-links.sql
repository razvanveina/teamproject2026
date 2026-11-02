-- Makes the links between the WMS tables mandatory (NOT NULL), like the @JoinColumn(nullable = false) in the entities.
-- Hibernate (ddl-auto=update) does not change columns that already exist, so every database needs this once.
-- Safe for any database: it changes nothing else, deletes nothing, and can be run again.
-- SQL Developer: select everything and press F5 (Run Script). Read the result in "Script Output".
SET SERVEROUTPUT ON

DECLARE
  PROCEDURE make_mandatory(p_table VARCHAR2, p_column VARCHAR2) IS
    v_empty NUMBER;
  BEGIN
    -- Rows where the link is empty would block the change, so count them first
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || p_table || ' WHERE ' || p_column || ' IS NULL' INTO v_empty;
    IF v_empty > 0 THEN
      DBMS_OUTPUT.PUT_LINE('NOT DONE  ' || p_table || '.' || p_column || ': ' || v_empty
          || ' row(s) have it empty. Fill or delete them, then run this script again.');
      RETURN;
    END IF;

    EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' MODIFY (' || p_column || ' NOT NULL)';
    DBMS_OUTPUT.PUT_LINE('OK        ' || p_table || '.' || p_column);
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -1442 THEN
        DBMS_OUTPUT.PUT_LINE('OK        ' || p_table || '.' || p_column || ' (was already mandatory)');
      ELSIF SQLCODE = -942 THEN
        DBMS_OUTPUT.PUT_LINE('SKIPPED   ' || p_table || ': the table does not exist, Hibernate will create it correctly');
      ELSE
        DBMS_OUTPUT.PUT_LINE('ERROR     ' || p_table || '.' || p_column || ': ' || SQLERRM);
      END IF;
  END;
BEGIN
  make_mandatory('STOCK_ITEM', 'ARTICLE_ID');
  make_mandatory('STOCK_ITEM', 'LOAD_UNIT_ID');
  make_mandatory('STOCK_ITEM', 'PACKAGING_UNIT_TYPE_ID');
  make_mandatory('PACKAGING_UNIT_TYPE', 'ARTICLE_ID');
  make_mandatory('RESERVATION', 'STOCK_ITEM_ID');
  make_mandatory('RESERVATION', 'RETRIEVAL_ORDER_LINE_ID');
  make_mandatory('RETRIEVAL_ORDER', 'DESTINATION_ID');
  make_mandatory('RETRIEVAL_ORDER_LINE', 'ARTICLE_ID');
  make_mandatory('RETRIEVAL_ORDER_LINE', 'ORDER_ID');
  make_mandatory('RECEIVING_ORDER_LINE', 'ARTICLE_ID');
  make_mandatory('RECEIVING_ORDER_LINE', 'ORDER_ID');
  make_mandatory('TRANSPORT_ORDER', 'LOAD_UNIT_ID');
  make_mandatory('TRANSPORT_ORDER', 'SOURCE_ID');
  make_mandatory('TRANSPORT_ORDER', 'TARGET_ID');
  make_mandatory('LOAD_UNIT', 'LOCATION_ID');
END;
/
