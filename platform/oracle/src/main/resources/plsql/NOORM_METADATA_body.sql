CREATE OR REPLACE
PACKAGE BODY noorm_metadata AS

  PROCEDURE get_version(p_version OUT VARCHAR2) AS
  BEGIN
    p_version := '${pom.version}';
  END get_version;

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
    WHEN NO_DATA_FOUND THEN RETURN;
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