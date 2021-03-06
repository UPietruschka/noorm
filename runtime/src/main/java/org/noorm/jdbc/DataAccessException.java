package org.noorm.jdbc;

/**
 * Data access exception.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public final class DataAccessException extends RuntimeException {

	private static final long serialVersionUID = 7434517703293407685L;

	private final Type type;

	public DataAccessException(final Throwable pCause) {
		super(pCause);
		type = Type.COULD_NOT_ACCESS_DATA;
	}

	public DataAccessException(final String pMessage, final Throwable pCause) {
		super(pMessage, pCause);
		type = Type.COULD_NOT_ACCESS_DATA;
	}

	public DataAccessException(final String pMessage) {
		super(pMessage);
		type = Type.COULD_NOT_ACCESS_DATA;
	}

	public DataAccessException(final Type pType) {
		super(pType.getDescription());
		type = pType;
	}

	public DataAccessException(final Type pType, final String pMessage) {
		super(pType.getDescription() + "; " + pMessage);
		type = pType;
	}

	public DataAccessException(final Type pType, final Throwable pCause) {
		super(pType.getDescription(), pCause);
		type = pType;
	}

	public Type getType() {
		return type;
	}

	public enum Type {

		COULD_NOT_ESTABLISH_CONNECTION(1000L, "COULD_NOT_ESTABLISH_CONNECTION", "Could not establish database connection."),
        NESTED_TRANSACTION_ROLLBACK(1020L, "NESTED_TRANSACTION_ROLLBACK", "Nested transaction rolled back."),
        NO_ACTIVE_DATA_SOURCE(1030L, "NO_ACTIVE_DATA_SOURCE", "No active data source. Use DataSourceProvider.setActiveDataSource to set the active data source."),
        UNKNOWN_DATA_SOURCE(1040L, "UNKNOWN_DATA_SOURCE", "Unknown data source. Cannot activate data source. Data source name unknown."),
        INVALID_DATA_SOURCE_SWITCH(1045L, "INVALID_DATA_SOURCE_SWITCH", "Invalid data source switch. Changing the data source during a user-managed transaction is not supported."),
        DATA_SOURCE_ALREADY_ADDED(1060L, "DATA_SOURCE_ALREADY_ADDED", "Unable to add new data source. A data source with the given name already exists."),
        COULD_NOT_ACCESS_DATA(1100L, "COULD_NOT_ACCESS_DATA", "Could not access data from database."),
		MULTIPLE_RECORDS_FOUND(1200L, "MULTIPLE_RECORDS_FOUND", "Multiple records found for single record query."),
		PARAMETERS_MUST_NOT_BE_NULL(1300L, "PARAMETERS_MUST_NOT_BE_NULL", "Parameters for JDBC call must not be null."),
		UNSUPPORTED_DATATYPE(1400L, "UNSUPPORTED_DATATYPE", "Database / JDBC datatype is not supported."),
		INITIALIZATION_FAILURE(1500L, "INITIALIZATION_FAILURE", "Database initialization failed."),
		STALE_TRANSACTION(1600L, "STALE_TRANSACTION", "Previous transaction in this thread has not been terminated properly."),
		CONNECTION_ACCESS_FAILURE(1700L, "CONNECTION_ACCESS_FAILURE", "Failure accessing database connection."),
		UNSUPPORTED_VERSION_COLUMN_TYPE(1800L, "UNSUPPORTED_VERSION_COLUMN_TYPE", "Unsupported version column type (Only numerical types and date/timestamp types are supported)."),
		VERSION_COLUMN_NULL(1810L, "VERSION_COLUMN_NULL", "Version column is null. Record has not been initialized properly."),
		OPTIMISTIC_LOCK_CONFLICT(1900L, "OPTIMISTIC_LOCK_CONFLICT", "Optimistic lock conflict. Record subject to modification has been "
				+ "modified or deleted by another process atfer it has been loaded for this operation."),
		COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION(2000L, "COULD_NOT_ACCESS_PROPERTY_BY_REFLECTION", "Could not access field of bean by reflection."),
		GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK(2100L, "GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK", "Generic update not supported without primary key definition."),
        GENERIC_UPDATE_FAILED_WITH_NULL_PK(2110L, "GENERIC_UPDATE_FAILED_WITH_NULL_PK", "Generic update not supported with null values in the primary key."),
        GENERIC_DELETE_NOT_SUPPORTED_WITHOUT_PK(2200L, "GENERIC_DELETE_NOT_SUPPORTED_WITHOUT_PK", "Generic delete not supported without primary key definition."),
        GENERIC_DELETE_FAILED_WITH_NULL_PK(2210L, "GENERIC_DELETE_FAILED_WITH_NULL_PK", "Generic delete not supported with null values in the primary key."),
        COULD_NOT_UPDATE_NON_UPDATABLE_BEAN(2300L, "COULD_NOT_UPDATE_NON_UPDATABLE_BEAN", "Could not update bean without any updatable fields."),
		OPERATION_NOT_SUPPORTED_WITH_COMPOSITE_PK(2400L, "OPERATION_NOT_SUPPORTED_WITH_COMPOSITE_PK", "Operation not supported with composite primary key."),
        COULD_NOT_ACCESS_JDBC_METADATA(3000L, "COULD_NOT_ACCESS_JDBC_METADATA", "Could not access JDBC metadata."),
		REFERENCED_JDBC_METADATA_NOT_FOUND(3050L, "REFERENCED_JDBC_METADATA_NOT_FOUND", "Referenced JDBC metadata not found."),
        COULD_NOT_ACQUIRE_TABLE_LOCK(3100L, "COULD_NOT_ACQUIRE_TABLE_LOCK", "Could not acquire table lock."),
        ISOLATED_TABLE_LOCK(3200L, "ISOLATED_TABLE_LOCK", "Acquiring table locks requires explicit (user managed) transaction handling."),
        COULD_NOT_CREATE_LOB(3250L, "COULD_NOT_CREATE_LOB", "Could not create LOB type (Clob, Blob, NClob or SQLXML)."),
        ISOLATED_LOB_CREATION(3300L, "ISOLATED_LOB_CREATION", "Creating Clob, Blob, NClob and SQLXML requires explicit (user managed) transaction handling."),
        UNSUPPORTED_PLATFORM(3400L, "UNSUPPORTED_PLATFORM", "The database platform chosen is not yet supported by NoORM. You may have to add a NoORM platform service provider to your classpath."),
		ILLEGAL_SORT_CRITERIA(3500L, "ILLEGAL_SORT_CRITERIA", "Illegal sort criteria provided.");

        private final Long id;
		private final String code;
		private final String description;

		private Type(final Long pId, final String pCode, final String pDescription) {

			this.id = pId;
			this.code = pCode;
			this.description = pDescription;
		}

		public Long getId() {
			return id;
		}

		public String getCode() {
			return code;
		}

		public String getDescription() {
			return description;
		}
	}
}
