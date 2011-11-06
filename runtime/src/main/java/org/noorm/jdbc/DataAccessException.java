package org.noorm.jdbc;

/**
 * Data access exception.
 *
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public final class DataAccessException extends RuntimeException {

	private static final long serialVersionUID = 7434517703293407685L;

	private Type type;

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

	public static enum Type {

		COULD_NOT_ESTABLISH_CONNECTION(1000L, "COULD_NOT_ESTABLISH_CONNECTION", "Could not establish database connection."),
		COULD_NOT_ACCESS_DATA(1100L, "COULD_NOT_ACCESS_DATA", "Could not access data from database."),
		MULTIPLE_RECORDS_FOUND(1200L, "MULTIPLE_RECORDS_FOUND", "Multiple records found for single record query."),
		PARAMETERS_MUST_NOT_BE_NULL(1300L, "PARAMETERS_MUST_NOT_BE_NULL", "Parameters for JDBC call must not be null."),
		UNSUPPORTED_DATATYPE(1400L, "UNSUPPORTED_DATATYPE", "Database / JDBC datatype is not supported."),
		INITIALIZATION_FAILURE(1500L, "INITIALIZATION_FAILURE", "Database initialization failed."),
		STALE_TRANSACTION(1600L, "STALE_TRANSACTION", "Previous transaction in this thread has not been terminated properly."),
		CONNECTION_ACCESS_FAILURE(1700L, "CONNECTION_ACCESS_FAILURE", "Failure accessing database connection."),
		UNSUPPORTED_VERSION_COLUMN_TYPE(1800L, "UNSUPPORTED_VERSION_COLUMN_TYPE", "Unsupported version column type (Only 'Long' supported yet)."),
		OPTIMISTIC_LOCK_CONFLICT(1900L, "OPTIMISTIC_LOCK_CONFLICT", "Optimistic lock conflict. Record subject to modification has been "
				+ "changed or modfied by another process atfer it has been loaded for this operation."),
		COULD_NOT_ACCESS_PK_BY_REFLECTION(2000L, "COULD_NOT_ACCESS_PK_BY_REFLECTION", "Could not access method of bean by reflection."),
		GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK(2100L, "GENERIC_UPDATE_NOT_SUPPORTED_WITHOUT_PK", "Generic update not supported without primary key definition."),
		GENERIC_DELETE_NOT_SUPPORTED_WITHOUT_PK(2200L, "GENERIC_DELETE_NOT_SUPPORTED_WITHOUT_PK", "Generic delete not supported without primary key definition."),
		COULD_NOT_UPDATE_NON_UPDATABLE_BEAN(2300L, "COULD_NOT_UPDATE_NON_UPDATABLE_BEAN", "Could not update bean without any updatable fields.");

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
