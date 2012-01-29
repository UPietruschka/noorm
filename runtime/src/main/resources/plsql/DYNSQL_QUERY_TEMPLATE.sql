DROP TABLE dynsql_query_template;
CREATE TABLE dynsql_query_template 
    (
    template_name VARCHAR2(32) NOT NULL,
    line_index NUMBER NOT NULL,
    parameter_name VARCHAR2(32),
    sql_line VARCHAR2(512) NOT NULL,
    is_mandatory NUMBER NOT NULL
    )
;

ALTER TABLE dynsql_query_template ADD CONSTRAINT dynsql_query_template_pk PRIMARY KEY (template_name, line_index);

COMMENT ON TABLE dynsql_query_template IS 'Table utilized by package dynamic_sql. This table contains SQL query fragments for dynamic SQL generation';
COMMENT ON COLUMN dynsql_query_template.template_name IS 'Each query template is associated with one and only one dynamic SQL query and uniquely identified by its template name';
COMMENT ON COLUMN dynsql_query_template.line_index IS 'The line number of the query fragment defined with this record';
COMMENT ON COLUMN dynsql_query_template.parameter_name IS 'The name of the bind variable for this fragment, without colon';
COMMENT ON COLUMN dynsql_query_template.sql_line IS 'The SQL fragment';
COMMENT ON COLUMN dynsql_query_template.is_mandatory IS 'Indicator, if this fragment is mandatory for dynamic SQL construction (0 = false, 1 = true)';
