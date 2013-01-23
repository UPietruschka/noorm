CREATE OR REPLACE VIEW v_employees AS
SELECT emp.*, dep.department_name, loc.city
FROM   employees emp,
       departments dep,
       locations loc
WHERE  emp.department_id = dep.department_id
AND    dep.location_id   = loc.location_id
/
