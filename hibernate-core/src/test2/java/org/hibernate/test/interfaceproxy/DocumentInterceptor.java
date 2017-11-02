/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: DocumentInterceptor.java 7860 2005-08-11 21:58:23Z oneovthafew $
package org.hibernate.test.interfaceproxy;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Iterator;

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Gavin King
 */
public class DocumentInterceptor implements Interceptor {

	@Override
	public boolean onLoad(
			Object entity, Serializable id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		return false;
	}

	@Override
	public boolean onFlushDirty(
			Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		if ( entity instanceof Document ) {
			currentState[2] = Calendar.getInstance();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean onSave(
			Object entity,
			Serializable id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
		if ( entity instanceof Document ) {
			state[3] = state[2] = Calendar.getInstance();
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void onDelete(
			Object entity,
			Serializable id,
			Object[] state,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) throws CallbackException {
	}

	@Override
	public void preFlush(Iterator entities) throws CallbackException {

	}

	@Override
	public void postFlush(Iterator entities) throws CallbackException {

	}

	@Override
	public Boolean isTransient(Object entity) {
		return null;
	}

	@Override
	public int[] findDirty(
			Object entity,
			Serializable id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			JavaTypeDescriptor[] javaTypeDescriptors) {
		return null;
	}

	@Override
	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) throws CallbackException {
		return null;
	}

	@Override
	public String getEntityName(Object object) throws CallbackException {
		return null;
	}

	@Override
	public Object getEntity(String entityName, Serializable id)	throws CallbackException {
		return null;
	}

	@Override
	public void afterTransactionBegin(Transaction tx) {}

	@Override
	public void afterTransactionCompletion(Transaction tx) {}

	@Override
	public void beforeTransactionCompletion(Transaction tx) {}

	@Override
	public String onPrepareStatement(String sql) {
		return sql;
	}

	@Override
	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {}

	@Override
	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {}

	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {}

}