DELETE FROM dynsql_query_template;

INSERT INTO dynsql_query_template (template_name, line_index, parameter_name, sql_line, is_mandatory) VALUES ('EMPLOYEE_QUERY', 10, NULL,                ' SELECT    * FROM employees e', 1);
INSERT INTO dynsql_query_template (template_name, line_index, parameter_name, sql_line, is_mandatory) VALUES ('EMPLOYEE_QUERY', 14, 'p_job_title',       ' JOIN      jobs j ON (e.job_id = j.job_id AND j.job_title = :p_job_title)', 0);
INSERT INTO dynsql_query_template (template_name, line_index, parameter_name, sql_line, is_mandatory) VALUES ('EMPLOYEE_QUERY', 16, NULL,                ' WHERE     1= 1', 1);
INSERT INTO dynsql_query_template (template_name, line_index, parameter_name, sql_line, is_mandatory) VALUES ('EMPLOYEE_QUERY', 20, 'p_last_name',       ' AND       e.last_name   = :p_last_name', 0);
INSERT INTO dynsql_query_template (template_name, line_index, parameter_name, sql_line, is_mandatory) VALUES ('EMPLOYEE_QUERY', 30, 'p_hire_date_from',  ' AND       e.hire_date  >= :p_hire_date_from', 0);
INSERT INTO dynsql_query_template (template_name, line_index, parameter_name, sql_line, is_mandatory) VALUES ('EMPLOYEE_QUERY', 40, 'p_hire_date_to',    ' AND       e.hire_date  <= :p_hire_date_to', 0);
INSERT INTO dynsql_query_template (template_name, line_index, parameter_name, sql_line, is_mandatory) VALUES ('EMPLOYEE_QUERY', 50, 'p_salary_from',     ' AND       e.salary     >= :p_salary_from', 0);
INSERT INTO dynsql_query_template (template_name, line_index, parameter_name, sql_line, is_mandatory) VALUES ('EMPLOYEE_QUERY', 60, 'p_salary_to',       ' AND       e.salary     <= :p_salary_to', 0);

commit;

