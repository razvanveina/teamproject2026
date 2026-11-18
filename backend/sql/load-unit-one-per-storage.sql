-- A storage location (HBW, PICK) holds at most one load unit.
-- SQL Developer: select everything and press F5 (Run Script).
CREATE OR REPLACE TRIGGER TRG_STORAGE_CAPACITY
BEFORE INSERT OR UPDATE OF LOCATION_ID
ON LOAD_UNIT
FOR EACH ROW
DECLARE
    v_dtype LOCATION.DTYPE%TYPE;
    v_count NUMBER;
BEGIN
    -- Determine the location type
    SELECT DTYPE
    INTO v_dtype
    FROM LOCATION
    WHERE ID = :NEW.LOCATION_ID;

    -- Only check storage locations
    IF v_dtype = 'STORAGE' THEN
        SELECT COUNT(*)
        INTO v_count
        FROM LOAD_UNIT
        WHERE LOCATION_ID = :NEW.LOCATION_ID
          AND ID <> NVL(:NEW.ID, -1);

        IF v_count >= 1 THEN
            RAISE_APPLICATION_ERROR(-20001, 'Storage location already contains a load unit');
        END IF;
    END IF;
END;
/
