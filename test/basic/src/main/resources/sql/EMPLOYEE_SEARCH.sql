CREATE OR REPLACE
PACKAGE employee_search AS

  TYPE employee_refcur IS REF CURSOR RETURN employees%ROWTYPE;

  PROCEDURE find_employee_ids(p_department_id IN NUMBER, p_employee_id_set OUT noorm_metadata.id_refcur);

  PROCEDURE find_pageable_emps_by_idlist(p_idlist IN num_array, p_employee_set OUT employee_refcur);

END employee_search;
/

CREATE OR REPLACE
PACKAGE BODY employee_search AS

  PROCEDURE find_employee_ids(p_department_id IN NUMBER, p_employee_id_set OUT noorm_metadata.id_refcur) AS
  BEGIN
    OPEN   p_employee_id_set FOR
    SELECT employee_id id
    FROM   employees
    WHERE  department_id = p_department_id;
  END;

  PROCEDURE find_pageable_emps_by_idlist(p_idlist IN num_array, p_employee_set OUT employee_refcur) AS
  BEGIN
    OPEN   p_employee_set FOR
    SELECT *
    FROM   employees
    WHERE  employee_id IN (SELECT * FROM TABLE(CAST(p_idlist AS num_array)));
  END;

END employee_search;
/