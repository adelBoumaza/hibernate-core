/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.io.Serializable;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.LoadingCollectionEntry;

/**
 * Represents a collection currently being loaded.
 *
 * @author Steve Ebersole
 */
public class LoadingCollectionEntryImpl implements LoadingCollectionEntry {
	private final CollectionPersister collectionDescriptor;
	private final CollectionInitializer initializer;
	private final Object key;
	private final PersistentCollection collectionInstance;

	public LoadingCollectionEntryImpl(
			CollectionPersister collectionDescriptor,
			CollectionInitializer initializer,
			Object key,
			PersistentCollection collectionInstance) {
		this.collectionDescriptor = collectionDescriptor;
		this.initializer = initializer;
		this.key = key;
		this.collectionInstance = collectionInstance;

		collectionInstance.beforeInitialize( getCollectionDescriptor(), -1 );
		collectionInstance.beginRead();
	}

	@Override public CollectionPersister getCollectionDescriptor() {
		return collectionDescriptor;
	}

	/**
	 * Access to the initializer that is responsible for initializing this collection
	 */
	@Override public CollectionInitializer getInitializer() {
		return initializer;
	}

	@Override public Serializable getKey() {
		// todo (6.0) : change from Serializable to Object
		return (Serializable) key;
	}

	@Override public PersistentCollection getCollectionInstance() {
		return collectionInstance;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getCollectionDescriptor().getNavigableRole().getFullPath() + "#" + getKey() + ")";
	}

	@Override public void finishLoading(ExecutionContext executionContext) {
		collectionInstance.endRead();

		final SharedSessionContractImplementor session = executionContext.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final CollectionPersister collectionDescriptor = getCollectionDescriptor();

		CollectionEntry collectionEntry = persistenceContext.getCollectionEntry( collectionInstance );
		if ( collectionEntry == null ) {
			collectionEntry = persistenceContext.addInitializedCollection(
					collectionDescriptor,
					getCollectionInstance(),
					getKey()
			);
		}
		else {
			collectionEntry.postInitialize( collectionInstance );
		}

		if ( collectionDescriptor.getCollectionType().hasHolder() ) {
			persistenceContext.addCollectionHolder( collectionInstance );
		}


		// todo (6.0) : there is other logic still needing to be implemented here.  caching, etc
		// 		see org.hibernate.engine.loading.internal.CollectionLoadContext#endLoadingCollection in 5.x
	}
}
