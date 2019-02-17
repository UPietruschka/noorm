CREATE OR REPLACE
PACKAGE noorm_metadata AS

  TYPE id_record IS RECORD
    (
    id NUMBER
    );
  TYPE id_refcur IS REF CURSOR RETURN id_record;

  PROCEDURE get_version(p_version OUT VARCHAR2);

  PROCEDURE get_parameter_rowtype(p_package_name IN VARCHAR2,
                                  p_procedure_name IN VARCHAR2,
                                  p_parameter_name IN VARCHAR2,
                                  p_rowtype_name OUT VARCHAR2);

END noorm_metadata;
/
