DROP TABLE complex_data_types;
CREATE TABLE complex_data_types
(
 id NUMBER NOT NULL,
 group_id NUMBER,
 raw_type_column RAW(1024),
 blob_column BLOB,
 clob_column CLOB,
 converted_clob_column CLOB,
 converted_nclob_column NCLOB,
 converted_blob_column BLOB,
 xml_column XMLTYPE,
 boolean_flag NUMBER(1),
 converted_number NUMBER,
 converted_timestamp TIMESTAMP,
 converted_date DATE,
 converted_xmltype XMLTYPE,
 version NUMBER DEFAULT 1
);
ALTER TABLE complex_data_types ADD CONSTRAINT complex_data_types_pk PRIMARY KEY (id);

DROP SEQUENCE complex_data_types_seq;
CREATE SEQUENCE complex_data_types_seq;
