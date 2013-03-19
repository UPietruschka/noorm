DROP TABLE opt_lock_long;
CREATE TABLE opt_lock_long
(
 opt_id NUMBER  NOT NULL,
 opt_text NVARCHAR2(32),
 opt_version NUMBER
);
ALTER TABLE opt_lock_long ADD CONSTRAINT opt_lock_long_pk PRIMARY KEY (opt_id);

DROP SEQUENCE opt_lock_long_seq;
CREATE SEQUENCE opt_lock_long_seq;

DROP TABLE opt_lock_timestamp;
CREATE TABLE opt_lock_timestamp
(
 opt_id NUMBER  NOT NULL,
 opt_text NVARCHAR2(32),
 opt_version TIMESTAMP DEFAULT SYSTIMESTAMP
);
ALTER TABLE opt_lock_timestamp ADD CONSTRAINT opt_lock_timestamp_pk PRIMARY KEY (opt_id);

DROP SEQUENCE opt_lock_timestamp_seq;
CREATE SEQUENCE opt_lock_timestamp_seq;

DROP TABLE opt_variants;
CREATE TABLE opt_variants
(
 opt_var_id NUMBER NOT NULL,
 opt_variant_name CHAR(32) NOT NULL
);
ALTER TABLE opt_variants ADD CONSTRAINT opt_variants_pk PRIMARY KEY (opt_id);

INSERT INTO opt_variants VALUES (1, 'Version column');
INSERT INTO opt_variants VALUES (2, 'Checksum');
INSERT INTO opt_variants VALUES (3, 'Full comparison');
COMMIT;
