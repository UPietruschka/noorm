CREATE OR REPLACE
PACKAGE dynamic_sql AS 

  dynsql_configuration_exception EXCEPTION;
  PRAGMA EXCEPTION_INIT(dynsql_configuration_exception, -20150);

  PROCEDURE get_version(p_version OUT VARCHAR2);
  PROCEDURE init;
  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN NUMBER);
  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN VARCHAR2);
  -- CHAR, VARCHAR2 and RAW are subject to implicit conversion between each other, thus, overloading
  -- does not work without some hint for the procedure (resp. type) of choice. This is accomplished
  -- by an explicit notation of the parameter name, or by another name for the procedure.
  PROCEDURE add_raw_parameter(p_param_name IN CHAR, p_param_value IN RAW);
  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN TIMESTAMP WITH TIME ZONE);
  PROCEDURE set_sorting_column(p_sorting_column IN CHAR, p_sorting_direction IN CHAR DEFAULT 'ASC');
  PROCEDURE execute(p_query_template_name IN CHAR, p_refcursor OUT sys_refcursor);

END dynamic_sql;
/
