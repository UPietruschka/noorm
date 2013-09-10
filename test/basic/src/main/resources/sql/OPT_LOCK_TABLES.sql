DROP SEQUENCE opt_lock_seq;
CREATE SEQUENCE opt_lock_seq;

DROP TABLE opt_lock_long;
CREATE TABLE opt_lock_long
(
 opt_id NUMBER  NOT NULL,
 opt_text NVARCHAR2(32),
 opt_version NUMBER
);
ALTER TABLE opt_lock_long ADD CONSTRAINT opt_lock_long_pk PRIMARY KEY (opt_id);

DROP TABLE opt_lock_timestamp;
CREATE TABLE opt_lock_timestamp
(
 opt_id NUMBER  NOT NULL,
 opt_text NVARCHAR2(32),
 opt_version TIMESTAMP DEFAULT SYSTIMESTAMP
);
ALTER TABLE opt_lock_timestamp ADD CONSTRAINT opt_lock_timestamp_pk PRIMARY KEY (opt_id);

DROP TABLE opt_lock_date;
CREATE TABLE opt_lock_date
(
 opt_id NUMBER  NOT NULL,
 opt_text NVARCHAR2(32),
 opt_version DATE DEFAULT SYSDATE
);
ALTER TABLE opt_lock_date ADD CONSTRAINT opt_lock_date_pk PRIMARY KEY (opt_id);

DROP TABLE opt_variants;
CREATE TABLE opt_variants
(
 opt_var_id NUMBER NOT NULL,
 opt_variant_name CHAR(64) NOT NULL
);
ALTER TABLE opt_variants ADD CONSTRAINT opt_variants_pk PRIMARY KEY (opt_var_id);

INSERT INTO opt_variants VALUES (1, 'Long type version column');
INSERT INTO opt_variants VALUES (2, 'Timestamp/Date type version column');
INSERT INTO opt_variants VALUES (3, 'Full comparison');
COMMIT;
