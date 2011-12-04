package org.noorm.generator;

/**
 * Java source generator exception.<br/>
 * @author Ulf Pietruschka / ulf.pietruschka@etenso.com
 */
public final class GeneratorException extends RuntimeException {

	public GeneratorException(final Throwable pCause) {
		super(pCause);
	}

	public GeneratorException(final String pMessage, final Throwable pCause) {
		super(pMessage, pCause);
	}

	public GeneratorException(final String pMessage) {
		super(pMessage);
	}
}
