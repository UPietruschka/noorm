CREATE OR REPLACE
PACKAGE BODY noorm_metadata AS

  PROCEDURE find_table_metadata(p_table_metadata OUT table_metadata_refcur) AS
  BEGIN
	OPEN p_table_metadata FOR
    SELECT tc.table_name,
           tc.column_name,
           tc.data_type,
           tc.data_precision,
           tc.data_scale,
           tc.char_length,
           tc.nullable,
           tc.column_id,
           uc.updatable,
           uc.insertable
    FROM   user_tab_columns tc,
           user_updatable_columns uc
    WHERE  tc.table_name  = uc.table_name
    AND    tc.column_name = uc.column_name
    ORDER  BY tc.table_name, tc.column_id;
  END find_table_metadata;

  PROCEDURE find_package_names(p_search_regex IN VARCHAR2, p_package_names OUT name_refcur) AS
  BEGIN
    OPEN p_package_names FOR
    SELECT object_name name
    FROM   user_objects
    WHERE  object_type = 'PACKAGE'
    AND    REGEXP_LIKE(object_name, p_search_regex);
  END find_package_names;

  PROCEDURE find_procedure_names(p_package_name IN VARCHAR2, p_procedure_names OUT name_refcur) AS
  BEGIN
    OPEN p_procedure_names FOR
    SELECT procedure_name name
    FROM   user_procedures
    WHERE  object_name = p_package_name
    AND    procedure_name IS NOT NULL;
  END find_procedure_names;

  PROCEDURE find_sequence_names(p_sequence_names OUT name_refcur) AS
  BEGIN
    OPEN p_sequence_names FOR
    SELECT sequence_name name
    FROM   user_sequences;
  END find_sequence_names;

  PROCEDURE find_pk_columns(p_pk_columns OUT pk_refcur) AS
  BEGIN
    OPEN p_pk_columns FOR
    SELECT cc.table_name,
           cc.column_name,
           cc.position
    FROM   user_constraints uc,
           user_cons_columns cc
    WHERE  uc.table_name      = cc.table_name
    AND    uc.constraint_name = cc.constraint_name
    AND    uc.constraint_type = 'P';
  END find_pk_columns;

  /*
    Unfortunately, PL/SQL record definitions are only subject to the PL/SQL compiler interpretation
    and not available in the Oracle data dictionary. For reverse engineering PL/SQL procedure calls
    based on PL/SQL records (which include implicit record definitions using %ROWTYPE) the list of
    fields in the record can be retrieved using data dictionary view USER_ARGUMENTS, but without a
    reference to the declaring row-type, if any.
    For this reason, evaluating the referenced row-type is done by comparing the given list with all
    explicitly declared row-types, i.e. tables and views. Currently, this limits the supported record
    definitions to row-types declared by tables and views.
  */
  PROCEDURE find_procedure_parameters(p_package_name IN VARCHAR2,
                                      p_procedure_name IN VARCHAR2,
                                      p_parameters OUT parameter_refcur) AS
  BEGIN
    OPEN p_parameters FOR
    SELECT argument_name name,
           NVL(type_name, data_type) type_name,
           in_out direction
    FROM   user_arguments
    WHERE  object_name = p_procedure_name
    AND    package_name = p_package_name
    AND    data_level = 0
    AND    argument_name IS NOT NULL
    ORDER  BY sequence;
  END find_procedure_parameters;

  PROCEDURE get_parameter_rowtype(p_package_name IN VARCHAR2,
                                  p_procedure_name IN VARCHAR2,
                                  p_parameter_name IN VARCHAR2,
                                  p_rowtype_name OUT VARCHAR2) AS

    l_full_package_text VARCHAR2(32767);
    l_ref_cursor_search_regex VARCHAR2(512);
    l_rowtype_search_regex VARCHAR2(512);
    l_ref_cursor_name VARCHAR2(30);
    CURSOR c_package_source IS
      SELECT text
      FROM   user_source
      WHERE  type = 'PACKAGE'
      AND    name = UPPER(p_package_name)
      ORDER  BY line;
  BEGIN
    l_full_package_text := '';
    FOR source_line IN c_package_source LOOP
      l_full_package_text := concat(l_full_package_text, source_line.text);
    END LOOP;
    -- The full package source is matched with a regular expression to extract the token
    -- we are interested in. The part preceding the token we are searching for is enclosed
    -- by brackets as well as the token itself.
    l_ref_cursor_search_regex := concat('(.*PROCEDURE *', p_procedure_name);
    l_ref_cursor_search_regex := concat(l_ref_cursor_search_regex, ' *\([[:alnum:][:space:]''_,]*');
    l_ref_cursor_search_regex := concat(l_ref_cursor_search_regex, p_parameter_name);
    l_ref_cursor_search_regex := concat(l_ref_cursor_search_regex, ' *OUT *)([[:alnum:]_\.]*)(.*)');
    l_ref_cursor_name := regexp_replace(l_full_package_text, l_ref_cursor_search_regex, '\2', 1, 1, 'in');
    -- The ref cursor name is the name of local ref cursor type definition and must be resolved to the ROWTYPE
    IF (l_ref_cursor_name = 'noorm_metadata.id_refcur') THEN
      p_rowtype_name := 'NOORM_METADATA_ID_RECORD';
    ELSE
      l_rowtype_search_regex := concat('(.*TYPE *', l_ref_cursor_name);
      l_rowtype_search_regex := concat(l_rowtype_search_regex, ' *IS *REF *CURSOR *RETURN *)([[:alnum:]_]*)(.*)');
      p_rowtype_name := regexp_replace(l_full_package_text, l_rowtype_search_regex, '\2', 1, 1, 'in');
    END IF;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20011, 'Error retrieving ref cursor usage in procedure parameters using source code.');
  END get_parameter_rowtype;

END noorm_metadata;
/