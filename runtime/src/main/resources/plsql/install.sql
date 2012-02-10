-- Oracle installation script for all PL/SQL packages and associated views and types.

spool install_noorm.log

-- Types

@@NUM_ARRAY

-- Generic Packages

@@DYNSQL_QUERY_TEMPLATE
@@DYNAMIC_SQL
@@DYNAMIC_SQL_body
@@NOORM_METADATA
@@NOORM_METADATA_body

spool off
