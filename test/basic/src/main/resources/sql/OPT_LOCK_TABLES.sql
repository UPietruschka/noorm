DROP TABLE opt_lock_long;
CREATE TABLE opt_lock_long
(
 id NUMBER  NOT NULL,
 text NVARCHAR2(32),
 version NUMBER
);
ALTER TABLE opt_lock_long ADD CONSTRAINT opt_lock_long_pk PRIMARY KEY (id);

DROP SEQUENCE opt_lock_long_seq;
CREATE SEQUENCE opt_lock_long_seq;

DROP TABLE opt_lock_timestamp;
CREATE TABLE opt_lock_timestamp
(
 id NUMBER  NOT NULL,
 text NVARCHAR2(32),
 version TIMESTAMP DEFAULT SYSTIMESTAMP
);
ALTER TABLE opt_lock_timestamp ADD CONSTRAINT opt_lock_timestamp_pk PRIMARY KEY (id);

DROP SEQUENCE opt_lock_timestamp_seq;
CREATE SEQUENCE opt_lock_timestamp_seq;
