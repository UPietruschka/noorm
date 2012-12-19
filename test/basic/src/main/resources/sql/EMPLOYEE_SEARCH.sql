CREATE OR REPLACE
PACKAGE employee_search AS

  TYPE employee_refcur IS REF CURSOR RETURN employees%ROWTYPE;

  PROCEDURE find_employees_by_filter(p_job_title IN VARCHAR2 DEFAULT NULL,
                                     p_last_name IN VARCHAR2 DEFAULT NULL,
                                     p_hire_date_from DATE DEFAULT NULL,
                                     p_hire_date_to DATE DEFAULT NULL,
                                     p_salary_from NUMBER DEFAULT NULL,
                                     p_salary_to NUMBER DEFAULT NULL,
                                     p_employee_set OUT employee_refcur);

  PROCEDURE find_employee_ids(p_department_id IN NUMBER, p_employee_id_set OUT noorm_metadata.id_refcur);

  PROCEDURE find_pageable_emps_by_idlist(p_idlist IN num_array, p_employee_set OUT employee_refcur);

END employee_search;
/

CREATE OR REPLACE
PACKAGE BODY employee_search AS

  PROCEDURE find_employees_by_filter(p_job_title IN VARCHAR2 DEFAULT NULL,
                                     p_last_name IN VARCHAR2 DEFAULT NULL,
                                     p_hire_date_from DATE DEFAULT NULL,
                                     p_hire_date_to DATE DEFAULT NULL,
                                     p_salary_from NUMBER DEFAULT NULL,
                                     p_salary_to NUMBER DEFAULT NULL,
                                     p_employee_set OUT employee_refcur) AS

  BEGIN
    noorm_dynamic_sql.init;
    noorm_dynamic_sql.add_parameter('p_job_title', p_job_title);
    noorm_dynamic_sql.add_parameter('p_last_name', p_last_name);
    noorm_dynamic_sql.add_parameter('p_hire_date_from', p_hire_date_from);
    noorm_dynamic_sql.add_parameter('p_hire_date_to', p_hire_date_to);
    noorm_dynamic_sql.add_parameter('p_salary_from', p_salary_from);
    noorm_dynamic_sql.add_parameter('p_salary_to', p_salary_to);
    noorm_dynamic_sql.execute('EMPLOYEE_QUERY', p_employee_set);
  END;

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