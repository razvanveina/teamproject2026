-- Lets retrieval_order.status take the new value PICKED (set by the picking station when a box is closed).
-- Hibernate (ddl-auto=update) does not change the status check made by an older create, so every database needs this once.
-- Not needed after ddl-auto=create. Safe to run again.
-- SQL Developer: select everything and press F5 (Run Script).
BEGIN
  FOR c IN (SELECT constraint_name FROM user_constraints
            WHERE table_name = 'RETRIEVAL_ORDER' AND constraint_type = 'C' AND UPPER(search_condition_vc) LIKE 'STATUS IN%') LOOP
    EXECUTE IMMEDIATE 'ALTER TABLE retrieval_order DROP CONSTRAINT ' || c.constraint_name;
  END LOOP;
END;
/
ALTER TABLE retrieval_order ADD CHECK (status IN ('NEW', 'STARTED', 'ACTIVE', 'FINISHED', 'PICKED'));
