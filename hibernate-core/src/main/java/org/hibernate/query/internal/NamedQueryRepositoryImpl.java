/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.named.NamedQueryRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class NamedQueryRepositoryImpl implements NamedQueryRepository {
	private static final Logger log = Logger.getLogger( NamedQueryRepository.class );

	private final Map<String, NamedHqlQueryMemento> hqlMementoMap;
	private final Map<String, NamedNativeQueryMemento> sqlMementoMap;
	private final Map<String, NamedCallableQueryMemento> callableMementoMap;
	private final Map<String, NamedResultSetMappingMemento> resultSetMappingMementoMap;

	public NamedQueryRepositoryImpl(
			Map<String,NamedHqlQueryMemento> hqlMementoMap,
			Map<String,NamedNativeQueryMemento> sqlMementoMap,
			Map<String,NamedCallableQueryMemento> callableMementoMap,
			Map<String,NamedResultSetMappingMemento> resultSetMappingMementoMap) {
		this.hqlMementoMap = hqlMementoMap;
		this.sqlMementoMap = sqlMementoMap;
		this.callableMementoMap = callableMementoMap;
		this.resultSetMappingMementoMap = resultSetMappingMementoMap;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// HQL mementos

	@Override
	public NamedHqlQueryMemento getHqlQueryMemento(String queryName) {
		return hqlMementoMap.get( queryName );
	}

	@Override
	public void visitHqlQueryMementos(Consumer<NamedHqlQueryMemento> action) {
		hqlMementoMap.values().forEach( action );
	}

	@Override
	public synchronized void registerHqlQueryMemento(String name, NamedHqlQueryMemento descriptor) {
		hqlMementoMap.put( name, descriptor );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SQL mementos

	@Override
	public NamedNativeQueryMemento getNativeQueryMemento(String queryName) {
		return sqlMementoMap.get( queryName );
	}

	@Override
	public void visitNativeQueryMementos(Consumer<NamedNativeQueryMemento> action) {
		sqlMementoMap.values().forEach( action );
	}

	@Override
	public synchronized void registerNativeQueryMemento(String name, NamedNativeQueryMemento descriptor) {
		sqlMementoMap.put( name, descriptor );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// callable mementos

	@Override
	public NamedCallableQueryMemento getCallableQueryMemento(String name) {
		return callableMementoMap.get( name );
	}

	@Override
	public void visitCallableQueryMementos(Consumer<NamedCallableQueryMemento> action) {
		callableMementoMap.values().forEach( action );
	}

	@Override
	public synchronized void registerCallableQueryMemento(String name, NamedCallableQueryMemento memento) {
		callableMementoMap.put( name, memento );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Result-set mapping mementos

	@Override
	public NamedResultSetMappingMemento getResultSetMappingMemento(String mappingName) {
		return resultSetMappingMementoMap.get( mappingName );
	}

	@Override
	public void visitResultSetMappingMementos(Consumer<NamedResultSetMappingMemento> action) {
		resultSetMappingMementoMap.values().forEach( action );
	}

	@Override
	public void registerResultSetMappingMemento(String name, NamedResultSetMappingMemento memento) {
		resultSetMappingMementoMap.put( name, memento );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named query checking

	public Map<String, HibernateException> checkNamedQueries(QueryEngine queryEngine) {
		Map<String,HibernateException> errors = new HashMap<>();

		final HqlTranslator sqmProducer = queryEngine.getHqlTranslator();
		final QueryInterpretationCache interpretationCache = queryEngine.getInterpretationCache();
		final boolean cachingEnabled = interpretationCache.isEnabled();

		// Check named HQL queries
		log.debugf( "Checking %s named HQL queries", hqlMementoMap.size() );
		for ( NamedHqlQueryMemento hqlMemento : hqlMementoMap.values() ) {
			try {
				log.debugf( "Checking named HQL query: %s", hqlMemento.getRegistrationName() );
				hqlMemento.validate( queryEngine );

				// todo (6.0) : need to cache these; however atm that requires producing a SqmQueryImpl
				// queryEngine.getQueryInterpretationCache().getHQLQueryPlan( hqlMemento.getQueryString(), false, Collections.EMPTY_MAP );
			}
			catch ( HibernateException e ) {
				errors.put( hqlMemento.getRegistrationName(), e );
			}
		}

		// Check native-sql queries
		log.debugf( "Checking %s named SQL queries", sqlMementoMap.size() );
		for ( NamedNativeQueryMemento memento : sqlMementoMap.values() ) {
			memento.validate( queryEngine );
//			// this will throw an error if there's something wrong.
//			try {
//				log.debugf( "Checking named SQL query: %s", memento.getRegistrationName() );
//				// TODO : would be really nice to cache the spec on the query-def so as to not have to re-calc the hash;
//				// currently not doable though because of the resultset-ref stuff...
//				NativeSQLQuerySpecification spec;
//				if ( memento.getResultSetMappingName() != null ) {
//					NamedResultSetMappingMemento resultSetMappingMemento = getResultSetMappingMemento( memento.getResultSetMappingName() );
//					if ( resultSetMappingMemento == null ) {
//						throw new MappingException( "Unable to find resultset-ref resultSetMappingMemento: " + memento.getResultSetMappingName() );
//					}
//					spec = new NativeSQLQuerySpecification(
//							namedSQLQueryDefinition.getQueryString(),
//							resultSetMappingMemento.getQueryReturns(),
//							namedSQLQueryDefinition.getQuerySpaces()
//					);
//				}
//				else {
//					spec =  new NativeSQLQuerySpecification(
//							namedSQLQueryDefinition.getQueryString(),
//							namedSQLQueryDefinition.getQueryReturns(),
//							namedSQLQueryDefinition.getQuerySpaces()
//					);
//				}
//				queryEngine.getNativeSQLQueryPlan( spec );
//			}
//			catch ( HibernateException e ) {
//				errors.put( namedSQLQueryDefinition.getName(), e );
//			}
		}

		return errors;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shutdown

	@Override
	public void close() {
		hqlMementoMap.clear();
		sqlMementoMap.clear();
		callableMementoMap.clear();
		resultSetMappingMementoMap.clear();
	}
}
