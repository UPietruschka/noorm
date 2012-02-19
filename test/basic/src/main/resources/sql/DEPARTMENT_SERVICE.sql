CREATE OR REPLACE
PACKAGE department_service AS

  TYPE v_departments_refcur IS REF CURSOR RETURN v_departments%ROWTYPE;

  PROCEDURE find_departments_by_id(p_department_id IN NUMBER,
                                   p_department_set OUT v_departments_refcur);

END department_service;
/

CREATE OR REPLACE
PACKAGE BODY department_service AS

  PROCEDURE find_departments_by_id(p_department_id IN NUMBER,
                                   p_department_set OUT v_departments_refcur) AS
  BEGIN
    OPEN p_department_set FOR
    SELECT * FROM V_DEPARTMENTS WHERE department_id = p_department_id;
  END;

END department_service;
/