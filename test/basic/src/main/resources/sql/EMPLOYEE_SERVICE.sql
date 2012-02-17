CREATE OR REPLACE
PACKAGE employee_service AS

  TYPE employee_refcur IS REF CURSOR RETURN employees%ROWTYPE;
  TYPE emp_details_view_refcur IS REF CURSOR RETURN emp_details_view%ROWTYPE;
  TYPE job_refcur IS REF CURSOR RETURN jobs%ROWTYPE;
  TYPE job_history_refcur IS REF CURSOR RETURN job_history%ROWTYPE;

  PROCEDURE find_all_employees(p_employee_set OUT employee_refcur);

  PROCEDURE find_unique_employee_by_id(p_id IN NUMBER, p_employee_set OUT employee_refcur);

  PROCEDURE find_employees_by_lastname(p_last_name IN VARCHAR2, p_employee_set OUT employee_refcur);

  PROCEDURE find_all_employee_details(p_employee_detail_set OUT emp_details_view_refcur);

  PROCEDURE find_job_by_id(p_id IN VARCHAR2, p_job_set OUT job_refcur);

  PROCEDURE find_job_history_by_emp_id(p_id IN NUMBER, p_job_history_set OUT job_history_refcur);

END employee_service;
/

CREATE OR REPLACE
PACKAGE BODY employee_service AS

  PROCEDURE find_all_employees(p_employee_set OUT employee_refcur) AS
  BEGIN
    OPEN p_employee_set FOR
    SELECT * FROM employees;
  END;

  PROCEDURE find_unique_employee_by_id(p_id IN NUMBER, p_employee_set OUT employee_refcur) AS
  BEGIN
    OPEN p_employee_set FOR
    SELECT * FROM employees
    WHERE  employee_id = p_id;
  END;

  PROCEDURE find_employees_by_lastname(p_last_name IN VARCHAR2, p_employee_set OUT employee_refcur) AS
  BEGIN
    OPEN p_employee_set FOR
    SELECT * FROM employees
    WHERE  last_name = p_last_name;
  END;

  PROCEDURE find_all_employee_details(p_employee_detail_set OUT emp_details_view_refcur) AS
  BEGIN
    OPEN p_employee_detail_set FOR
    SELECT * FROM emp_details_view;
  END;

  PROCEDURE find_job_history_by_emp_id(p_id IN NUMBER, p_job_history_set OUT job_history_refcur) AS
  BEGIN
    OPEN p_job_history_set FOR
    SELECT * FROM job_history
    WHERE  employee_id = p_id;
  END;

  PROCEDURE find_job_by_id(p_id IN VARCHAR2, p_job_set OUT job_refcur) AS
  BEGIN
    OPEN p_job_set FOR
    SELECT * FROM jobs
    WHERE  job_id = p_id;
  END;

END employee_service;
/