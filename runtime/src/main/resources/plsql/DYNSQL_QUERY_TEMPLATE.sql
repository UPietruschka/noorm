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
