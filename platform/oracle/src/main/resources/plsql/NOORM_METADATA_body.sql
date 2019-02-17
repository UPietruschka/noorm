CREATE OR REPLACE
PACKAGE BODY noorm_metadata AS

  PROCEDURE get_version(p_version OUT VARCHAR2) AS
  BEGIN
    p_version := '${pom.version}';
  END get_version;

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

END noorm_metadata;
/