DROP TABLE "case_sensitive_table";
CREATE TABLE "case_sensitive_table"
(
 "id" NUMBER NOT NULL,
 "c1" NUMBER NOT NULL,
 "c2" VARCHAR2(64) NOT NULL,
 "version" NUMBER DEFAULT 1
);
ALTER TABLE "case_sensitive_table" ADD CONSTRAINT "case_sensitive_table_pk" PRIMARY KEY ("id");

DROP SEQUENCE "case_sensitive_table_seq";
CREATE SEQUENCE "case_sensitive_table_seq";
