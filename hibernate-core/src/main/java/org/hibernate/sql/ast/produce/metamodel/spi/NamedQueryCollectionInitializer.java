/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import java.io.Serializable;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.query.spi.NativeQueryImplementor;

/**
 * A wrapper around a named query.
 *
 * todo (6.0) : this is essentially a NativeQuery + QueryResultCollection + QueryResultAssemblerCollection + CollectionReferenceInitialzer
 *
 * @author Gavin King
 */
public final class NamedQueryCollectionInitializer implements CollectionInitializer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( NamedQueryCollectionInitializer.class );

	private final String queryName;
	private final PersistentCollectionMetadata persister;

	public NamedQueryCollectionInitializer(String queryName, PersistentCollectionMetadata persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}

	public void initialize(Serializable key, SharedSessionContractImplementor session) throws HibernateException {
		LOG.debugf( "Initializing collection: %s using named query: %s", persister.getRole(), queryName );

		NativeQueryImplementor nativeQuery = session.getNamedNativeQuery( queryName );

		if ( nativeQuery.getParameterMetadata().hasNamedParameters() ) {
			nativeQuery.setParameter(
					nativeQuery.getParameterMetadata().getNamedParameterNames().iterator().next(),
					key,
					persister.getKeyType()
			);
		}
		else {
			nativeQuery.setParameter( 0, key, persister.getKeyType() );
		}

		nativeQuery.setCollectionKey( key ).setHibernateFlushMode( FlushMode.MANUAL ).list();
	}
}