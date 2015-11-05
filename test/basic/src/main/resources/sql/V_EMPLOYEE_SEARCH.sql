CREATE OR REPLACE VIEW v_employee_search AS
SELECT e.*, j.job_title
FROM   employees e JOIN jobs j ON (e.job_id = j.job_id)
/
