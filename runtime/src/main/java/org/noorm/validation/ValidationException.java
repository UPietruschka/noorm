package org.noorm.validation;

/**
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 *         Date: 28.11.11
 *         Time: 16:16
 */
public class ValidationException extends RuntimeException {

	public ValidationException(final String pMessage) {
		super(pMessage);
	}

	public ValidationException(final Throwable pThrowable) {
		super (pThrowable);
	}

	public ValidationException(final String pMessage, final Throwable pThrowable) {
		super(pMessage, pThrowable);
	}
}
