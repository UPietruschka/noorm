CREATE OR REPLACE
FUNCTION get_employee_count(p_department_id IN NUMBER) RETURN NUMBER AS
  l_count NUMBER;
  CURSOR c_employee_count(p_department_id IN NUMBER) IS
    SELECT count(*) FROM employees WHERE department_id = p_department_id;
BEGIN
  OPEN c_employee_count(p_department_id);
  FETCH c_employee_count INTO l_count;
  RETURN l_count;
END;
/

CREATE OR REPLACE VIEW V_DEPARTMENTS AS
SELECT dep.department_id,
       dep.department_name,
       dep.manager_id,
       dep.location_id,
       loc.city,
       man.first_name,
       man.last_name,
       get_employee_count(dep.department_id) employee_count
FROM   departments dep,
       locations loc,
       employees man
WHERE  dep.location_id   = loc.location_id
AND    dep.manager_id    = man.employee_id
/