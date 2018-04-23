DROP TABLE no_seq_with_pk;
CREATE TABLE no_seq_with_pk
(
 nswp_id NUMBER NOT NULL,
 nswp_text VARCHAR2(32)
);
ALTER TABLE no_seq_with_pk ADD CONSTRAINT no_seq_with_pk_pk PRIMARY KEY (nswp_id);

DROP TABLE no_seq_no_pk;
CREATE TABLE no_seq_no_pk
(
 nsnp_id NUMBER NOT NULL,
 nsnp_text VARCHAR2(32)
);
