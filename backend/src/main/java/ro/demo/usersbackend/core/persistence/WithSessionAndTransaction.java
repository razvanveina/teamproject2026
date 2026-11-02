/*
 * Copyright (c) 2018 SSI Schaefer Noell GmbH
 *
 * $Header: $
 */

package ro.demo.usersbackend.core.persistence;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * @author <a href="mailto:rveina@ssi-schaefer-noell.com">rveina</a>
 * @version $Revision: $, $Date: $, $Author: $
 * @param <T>
 */
public abstract class WithSessionAndTransaction<T> {
	protected T returnValue;

	public T run() {
		SessionFactory sessionFactory = SessionFactoryProvider.getSessionFactory();
		Session session = null;
		Transaction tx = null;

		try {
			session = sessionFactory.openSession();
			tx = session.beginTransaction();

			executeBusinessLogic(session);

			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				tx.rollback();
			}
			e.printStackTrace();
			throw new RuntimeException("Error executing business logic", e);
		} finally {
			if (session != null) {
				session.close();
			}
		}

		return returnValue;
	}

	protected abstract void executeBusinessLogic(Session session);

	protected void setReturnValue(T returnValue) {
		this.returnValue = returnValue;
	}
}
