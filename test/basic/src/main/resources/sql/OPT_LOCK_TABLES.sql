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
