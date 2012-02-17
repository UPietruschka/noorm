CREATE OR REPLACE
PACKAGE job_service AS

  PROCEDURE increase_salary(p_department_id IN NUMBER,
                            p_percentage IN FLOAT);

  PROCEDURE get_employee_count(p_employee_count OUT NUMBER);

END job_service;
/

CREATE OR REPLACE
PACKAGE BODY job_service AS

  PROCEDURE increase_salary(p_department_id IN NUMBER,
                            p_percentage IN FLOAT) AS
  BEGIN
    UPDATE employees
    SET    salary = salary * (1 + p_percentage)
    WHERE  department_id = p_department_id;
  END;

  PROCEDURE get_employee_count(p_employee_count OUT NUMBER) AS
  BEGIN
    SELECT COUNT(*) INTO p_employee_count FROM employees;
  END;

END job_service;
/