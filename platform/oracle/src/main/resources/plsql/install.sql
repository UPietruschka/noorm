-- Oracle installation script for all PL/SQL packages and associated views and types.

spool install_noorm.log

-- Metadata Package

@@NOORM_METADATA
@@NOORM_METADATA_body

spool off
