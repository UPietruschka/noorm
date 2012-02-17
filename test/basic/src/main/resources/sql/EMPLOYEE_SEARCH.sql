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
    dynamic_sql.init;
    dynamic_sql.add_parameter('p_job_title', p_job_title);
    dynamic_sql.add_parameter('p_last_name', p_last_name);
    dynamic_sql.add_parameter('p_hire_date_from', p_hire_date_from);
    dynamic_sql.add_parameter('p_hire_date_to', p_hire_date_to);
    dynamic_sql.add_parameter('p_salary_from', p_salary_from);
    dynamic_sql.add_parameter('p_salary_to', p_salary_to);
    dynamic_sql.execute('EMPLOYEE_QUERY', p_employee_set);
  END;

END employee_search;
/