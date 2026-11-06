-- Lets load_unit.type take the new value TOTE (the boxes that come out of decanting).
-- Hibernate (ddl-auto=update) does not change the type check made by an older create, so every database needs this once.
-- Not needed after ddl-auto=create. Safe to run again.
-- SQL Developer: select everything and press F5 (Run Script).
BEGIN
  FOR c IN (SELECT constraint_name FROM user_constraints
            WHERE table_name = 'LOAD_UNIT' AND constraint_type = 'C' AND UPPER(search_condition_vc) LIKE 'TYPE IN%') LOOP
    EXECUTE IMMEDIATE 'ALTER TABLE load_unit DROP CONSTRAINT ' || c.constraint_name;
  END LOOP;
END;
/
ALTER TABLE load_unit ADD CHECK (type IN ('PALLET', 'CARTON', 'TOTE'));
