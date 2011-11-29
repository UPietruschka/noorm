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
  TYPE name_refcur IS REF CURSOR RETURN name_record;
  TYPE parameter_record IS RECORD
    (
    name VARCHAR2(30),
    type_name VARCHAR2(30),
    direction VARCHAR2(18)
    );
  TYPE parameter_refcur IS REF CURSOR RETURN parameter_record;
  TYPE pk_record IS RECORD
    (
    table_name  VARCHAR2(30),
    column_name VARCHAR2(30),
    position NUMBER
    );
  TYPE pk_refcur IS REF CURSOR RETURN pk_record;

  PROCEDURE find_table_metadata(p_table_metadata OUT table_metadata_refcur);

  PROCEDURE find_package_names(p_search_regex IN VARCHAR2, p_package_names OUT name_refcur);

  PROCEDURE find_procedure_names(p_package_name IN VARCHAR2, p_procedure_names OUT name_refcur);

  PROCEDURE find_sequence_names(p_sequence_names OUT name_refcur);

  PROCEDURE find_pk_columns(p_pk_columns OUT pk_refcur);

  PROCEDURE find_procedure_parameters(p_package_name IN VARCHAR2,
                                      p_procedure_name IN VARCHAR2,
                                      p_parameters OUT parameter_refcur);

  PROCEDURE get_package_hash_value(p_package_name IN VARCHAR2,
                                   p_code_hash_value OUT NUMBER);

  PROCEDURE get_parameter_rowtype(p_package_name IN VARCHAR2,
                                  p_procedure_name IN VARCHAR2,
                                  p_parameter_name IN VARCHAR2,
                                  p_rowtype_name OUT VARCHAR2);

END noorm_metadata;
/
