package com.atg.openssp.common.cache.broker;

import java.util.Observable;
import java.util.Observer;

import com.atg.openssp.common.exception.EmptyHostException;

import restful.context.PathBuilder;

/**
 * @author André Schmer
 *
 */
public abstract class DataBrokerObserver implements Observer {

	public DataBrokerObserver() {}

	@Override
	public void update(final Observable o, final Object arg) {
		boolean isCacheSuccees = false;
		try {
			isCacheSuccees = doCaching();
		} finally {
			if (isCacheSuccees) {
				finalWork();
			}
		}
	}

	/**
	 * Enforces the caching.
	 * 
	 * @return true if caching was succeeded, false otherwise.
	 */
	protected abstract boolean doCaching();

	/**
	 * Do some final works on the special caches such as switch the key value store.
	 */
	protected abstract void finalWork();

	/**
	 * @return the context of the restful service to connect with {see PathBuilder}.
	 * @throws EmptyHostException
	 */
	public abstract PathBuilder getRestfulContext() throws EmptyHostException;

}
