CREATE OR REPLACE
PACKAGE complex_data_service AS

  TYPE complex_data_types_refcur IS REF CURSOR RETURN complex_data_types%ROWTYPE;

  PROCEDURE find_unique_cdt_by_id(p_id IN NUMBER, p_complex_data_type_set OUT complex_data_types_refcur);

  PROCEDURE find_cdt_by_group_id(p_group_id IN NUMBER, p_complex_data_type_set OUT complex_data_types_refcur);

END complex_data_service;
/

CREATE OR REPLACE
PACKAGE BODY complex_data_service AS

  PROCEDURE find_unique_cdt_by_id(p_id IN NUMBER, p_complex_data_type_set OUT complex_data_types_refcur) AS
  BEGIN
    OPEN p_complex_data_type_set FOR
    SELECT * FROM COMPLEX_DATA_TYPES WHERE id = p_id;
  END;

  PROCEDURE find_cdt_by_group_id(p_group_id IN NUMBER, p_complex_data_type_set OUT complex_data_types_refcur) AS
  BEGIN
    OPEN p_complex_data_type_set FOR
    SELECT * FROM COMPLEX_DATA_TYPES WHERE group_id = p_group_id;
  END;

END complex_data_service;
/