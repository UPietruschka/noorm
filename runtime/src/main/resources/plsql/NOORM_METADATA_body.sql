CREATE OR REPLACE
PACKAGE BODY noorm_metadata AS

  PROCEDURE get_version(p_version OUT VARCHAR2) AS
  BEGIN
    p_version := '${pom.version}';
  END get_version;

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
    UNION
    SELECT us.synonym_name,
           tc.column_name,
           tc.data_type,
           tc.data_precision,
           tc.data_scale,
           tc.char_length,
           tc.nullable,
           tc.column_id,
           uc.updatable,
           uc.insertable
    FROM   all_tab_columns tc,
           all_updatable_columns uc,
           user_synonyms us
    WHERE  tc.table_name  = uc.table_name
    AND    tc.owner       = uc.owner
    AND    tc.column_name = uc.column_name
    AND    tc.table_name  = us.table_name
    AND    tc.owner       = us.table_owner
    ORDER  BY table_name, column_id;
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

  PROCEDURE find_sequence_names(p_sequence_names OUT sequence_refcur) AS
  BEGIN
    OPEN p_sequence_names FOR
    SELECT sequence_name, increment_by
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
    AND    uc.constraint_type = 'P'
    UNION
    SELECT us.synonym_name table_name,
           cc.column_name,
           cc.position
    FROM   all_constraints ac,
           all_cons_columns cc,
           user_synonyms us
    WHERE  ac.table_name      = cc.table_name
    AND    ac.constraint_name = cc.constraint_name
    AND    ac.owner           = cc.owner
    AND    ac.table_name      = us.table_name
    AND    ac.owner           = us.table_owner
    AND    ac.constraint_type = 'P';
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
           data_type,
           type_name,
           in_out direction
    FROM   user_arguments
    WHERE  object_name = p_procedure_name
    AND    package_name = p_package_name
    AND    data_level = 0
    AND    argument_name IS NOT NULL
    ORDER  BY sequence;
  END find_procedure_parameters;

  PROCEDURE get_package_hash_value(p_package_name IN VARCHAR2,
                                   p_code_hash_value OUT NUMBER) AS

    l_full_package_text VARCHAR2(32767);
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
    IF l_full_package_text IS NULL THEN
      p_code_hash_value := -1;
    ELSE
      l_full_package_text := regexp_replace(l_full_package_text, '\s*', '');
      p_code_hash_value := DBMS_UTILITY.GET_HASH_VALUE(l_full_package_text, 0, 2147483647);
    END IF;
  END get_package_hash_value;

  /*
    PL/SQL records declared on basis of a rowtype are hard to detect, since no data dictionary view provides
    the required information, thus the only way to get the required information is parsing the source code.
    For explicitly declared record types things are easier, since the internal name of the record type is
    available through data dictionary view USER_ARGUMENTS.
   */
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
    SELECT type_subname INTO p_rowtype_name
    FROM   user_arguments
    WHERE  package_name  = p_package_name
    AND    object_name   = p_procedure_name
    AND    data_type     = 'PL/SQL RECORD';
    IF (p_rowtype_name IS NULL OR p_rowtype_name = 'ID_RECORD') THEN
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
    END IF;
  EXCEPTION
    WHEN OTHERS THEN
      raise_application_error(-20011, 'Error retrieving ref cursor usage in procedure parameters using source code.');
  END get_parameter_rowtype;

  /*
    Returned REF CURSOR type variables can either be mapped to a table or view (ROWTYPE) or to a PL/SQL
    record type as specified locally in a PL/SQL package. For the latter, the corresponding Java Bean must
    be assembled using the information available in data dictionary view USER_ARGUMENTS.
  */
  PROCEDURE find_record_metadata(p_record_metadata OUT table_metadata_refcur) AS
  BEGIN
    OPEN p_record_metadata FOR
    SELECT ref_cursor.type_subname table_name,
           record_elements.argument_name column_name,
           record_elements.data_type,
           record_elements.data_precision,
           record_elements.data_scale,
           record_elements.char_length,
           'Y' nullable,
           record_elements.position column_id,
           'NO' updatable,
           'NO' insertable
    FROM   user_arguments ref_cursor,
           user_arguments record_elements
    WHERE  ref_cursor.type_subname IS NOT NULL
    AND    ref_cursor.type_subname   != 'ID_RECORD'
    AND    ref_cursor.type_name      != 'NOORM_METADATA'
    AND    ref_cursor.data_type       = 'PL/SQL RECORD'
    AND    ref_cursor.object_name     = record_elements.object_name
    AND    ref_cursor.package_name    = record_elements.package_name
    AND    record_elements.data_level = 2
    AND    record_elements.data_type != 'PL/SQL RECORD'
    GROUP  BY ref_cursor.type_subname,
              record_elements.argument_name,
              record_elements.data_type,
              record_elements.data_length,
              record_elements.data_precision,
              record_elements.data_scale,
              record_elements.char_length,
              record_elements.position
    ORDER  BY ref_cursor.type_subname,
              record_elements.position;
  END find_record_metadata;

END noorm_metadata;
/