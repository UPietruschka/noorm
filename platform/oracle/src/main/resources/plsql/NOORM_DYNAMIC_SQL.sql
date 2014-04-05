CREATE OR REPLACE
PACKAGE noorm_dynamic_sql AS

  noorm_unknown_query_template EXCEPTION;
  noorm_invalid_sorting EXCEPTION;
  noorm_unsupported_datatype EXCEPTION;
  noorm_could_not_bin_parameter EXCEPTION;
  PRAGMA EXCEPTION_INIT(noorm_unknown_query_template, -20160);
  PRAGMA EXCEPTION_INIT(noorm_invalid_sorting, -20161);
  PRAGMA EXCEPTION_INIT(noorm_unsupported_datatype, -20162);
  PRAGMA EXCEPTION_INIT(noorm_could_not_bin_parameter, -20163);

  PROCEDURE get_version(p_version OUT VARCHAR2);
  PROCEDURE init;
  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN NUMBER);
  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN VARCHAR2);
  -- CHAR, VARCHAR2 and RAW are subject to implicit conversion between each other, thus, overloading
  -- does not work without some hint for the procedure (resp. type) of choice. This is accomplished
  -- by an explicit notation of the parameter name, or by another name for the procedure.
  PROCEDURE add_raw_parameter(p_param_name IN CHAR, p_param_value IN RAW);
  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN DATE);
  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN TIMESTAMP WITH TIME ZONE);
  PROCEDURE set_sorting_column(p_sorting_column IN CHAR, p_sorting_direction IN CHAR DEFAULT 'ASC');
  PROCEDURE execute(p_query_template_name IN CHAR, p_refcursor OUT sys_refcursor);

END noorm_dynamic_sql;
/
