CREATE OR REPLACE
PACKAGE noorm_metadata AS

  TYPE table_metadata_record IS RECORD
    (
    table_name VARCHAR2(30),
    column_name VARCHAR2(30),
    data_type VARCHAR2(106),
    data_precision NUMBER,
    data_scale NUMBER,
    char_length NUMBER,
    nullable VARCHAR2(1),
    column_id NUMBER,
    updatable VARCHAR2(3),
    insertable VARCHAR2(3)
    );
  TYPE table_metadata_refcur IS REF CURSOR RETURN table_metadata_record;
  TYPE id_record IS RECORD
    (
    id NUMBER
    );
  TYPE id_refcur IS REF CURSOR RETURN id_record;
  TYPE name_record IS RECORD
    (
    name VARCHAR2(128)
    );
  TYPE parameter_record IS RECORD
    (
    name VARCHAR2(30),
    data_type VARCHAR2(30),
    type_name VARCHAR2(30),
    direction VARCHAR2(18)
    );
  TYPE parameter_refcur IS REF CURSOR RETURN parameter_record;

  PROCEDURE get_version(p_version OUT VARCHAR2);

  PROCEDURE get_parameter_rowtype(p_package_name IN VARCHAR2,
                                  p_procedure_name IN VARCHAR2,
                                  p_parameter_name IN VARCHAR2,
                                  p_rowtype_name OUT VARCHAR2);

END noorm_metadata;
/
