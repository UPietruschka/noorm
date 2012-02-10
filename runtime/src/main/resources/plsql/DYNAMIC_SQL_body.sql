CREATE OR REPLACE
PACKAGE BODY dynamic_sql AS

  TYPE query_parameters IS TABLE OF ANYDATA INDEX BY VARCHAR2(32);
  l_parameters query_parameters;
  l_sorting_column VARCHAR2(32);
  l_sorting_direction VARCHAR2(4);

  PROCEDURE get_version(p_version OUT VARCHAR2) AS
  BEGIN
    p_version := '${pom.version}';
  END get_version;

  PROCEDURE init AS
  BEGIN
    l_parameters.DELETE;
  END init;

  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN NUMBER) AS
  BEGIN
    IF (p_param_value IS NOT NULL) THEN
      l_parameters(p_param_name) := ANYDATA.ConvertNumber(p_param_value);
    END IF;
  END add_parameter;

  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN VARCHAR2) AS
  BEGIN
    IF (p_param_value IS NOT NULL) THEN
      l_parameters(p_param_name) := ANYDATA.ConvertVarchar2(p_param_value);
    END IF;
  END add_parameter;

  PROCEDURE add_raw_parameter(p_param_name IN CHAR, p_param_value IN RAW) AS
  BEGIN
    IF (p_param_value IS NOT NULL) THEN
      l_parameters(p_param_name) := ANYDATA.ConvertRaw(p_param_value);
    END IF;
  END add_raw_parameter;

  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN DATE) AS
  BEGIN
    IF (p_param_value IS NOT NULL) THEN
      l_parameters(p_param_name) := ANYDATA.ConvertDate(p_param_value);
    END IF;
  END add_parameter;

  PROCEDURE add_parameter(p_param_name IN CHAR, p_param_value IN TIMESTAMP WITH TIME ZONE) AS
  BEGIN
    IF (p_param_value IS NOT NULL) THEN
      l_parameters(p_param_name) := ANYDATA.ConvertTimestampTZ(p_param_value);
    END IF;
  END add_parameter;

  PROCEDURE set_sorting_column(p_sorting_column IN CHAR, p_sorting_direction IN CHAR DEFAULT 'ASC') AS
  BEGIN
    IF (p_sorting_column IS NOT NULL) THEN
      l_sorting_column := p_sorting_column;
      l_sorting_direction := p_sorting_direction;
    END IF;
  END set_sorting_column;

  PROCEDURE execute(p_query_template_name IN CHAR, p_refcursor OUT SYS_REFCURSOR) AS
    template_row dynsql_query_template%ROWTYPE;
    param_name dynsql_query_template.parameter_name%TYPE;
    param_value ANYDATA;
    param_type ANYTYPE;
    l_cursor NUMBER;
    l_return NUMBER;
    final_query VARCHAR2(4000);
    CURSOR c_query_template(p_template_name in dynsql_query_template.template_name%TYPE) IS
      SELECT *
      FROM   dynsql_query_template
      WHERE  template_name = p_template_name
      ORDER  BY line_index;
  BEGIN
    -- Iterate over the query template lines and select the appropriate lines
    OPEN c_query_template(p_query_template_name);
    LOOP
      FETCH c_query_template INTO template_row;
      EXIT WHEN c_query_template%NOTFOUND;
      param_name := template_row.parameter_name;
      IF (template_row.is_mandatory = 1) THEN
        final_query := final_query || template_row.sql_line;
      ELSE
        IF (l_parameters.EXISTS(template_row.parameter_name)) THEN
          final_query := final_query || template_row.sql_line;
        END IF;
      END IF;
    END LOOP;
    IF (c_query_template%ROWCOUNT = 0) THEN
      RAISE_APPLICATION_ERROR(-20160, 'Query template not found: ' || p_query_template_name);
    END IF;
    CLOSE c_query_template;
    -- Set order criteria, if specified
    IF (l_sorting_column IS NOT NULL) THEN
      IF (l_sorting_direction NOT IN ('asc', 'ASC', 'desc', 'DESC')) THEN
        RAISE_APPLICATION_ERROR(-20161, 'Invalid sorting direction: ' || l_sorting_direction);
      END IF;
      final_query := final_query || ' ORDER BY ';
      final_query := final_query || l_sorting_column;
      final_query := final_query || ' ';
      final_query := final_query || l_sorting_direction;
    END IF;
    -- Parse the query
    l_cursor := dbms_sql.open_cursor;
    dbms_sql.parse(l_cursor, final_query, dbms_sql.native);
    -- Iterate over the parameters and bind them to the parsed query
    -- (order does not matter, since bind variables are named)
    param_name := l_parameters.FIRST;
    LOOP
      EXIT WHEN param_name IS NULL;
      param_value := l_parameters(param_name);
      CASE param_value.getType(param_type)
        WHEN dbms_types.typecode_number
        THEN dbms_sql.bind_variable(l_cursor, param_name, param_value.AccessNumber());
        WHEN dbms_types.typecode_varchar2
        THEN dbms_sql.bind_variable(l_cursor, param_name, param_value.AccessVarchar2());
        WHEN dbms_types.typecode_char
        THEN dbms_sql.bind_variable(l_cursor, param_name, param_value.AccessChar());
        WHEN dbms_types.typecode_date
        THEN dbms_sql.bind_variable(l_cursor, param_name, param_value.AccessDate());
        WHEN dbms_types.typecode_timestamp_tz
        THEN dbms_sql.bind_variable(l_cursor, param_name, param_value.AccessTimestampTZ());
        WHEN dbms_types.typecode_raw
        THEN dbms_sql.bind_variable(l_cursor, param_name, param_value.AccessRaw());
        ELSE RAISE_APPLICATION_ERROR(-20162, 'Unsupported data type for parameter: ' || param_name);
      END CASE;
      param_name := l_parameters.NEXT(param_name);
    END LOOP;
    l_return := dbms_sql.EXECUTE(l_cursor);
    p_refcursor := dbms_sql.to_refcursor(l_cursor);
  EXCEPTION WHEN OTHERS THEN
    RAISE_APPLICATION_ERROR(-20163, 'Could not bind parameter: ' || param_name);
  END execute;

END dynamic_sql;
/
