/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal.sqm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.ScrollMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.streams.StingArrayCollector;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.JpaTupleBuilder;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.consume.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.ast.consume.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.ast.consume.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.ast.consume.internal.RowTransformerTupleImpl;
import org.hibernate.sql.ast.consume.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.ast.consume.internal.TupleElementImpl;
import org.hibernate.sql.ast.consume.spi.JdbcSelect;
import org.hibernate.sql.ast.consume.spi.RowTransformer;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.spi.SqlSelectPlan;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;

/**
 * @author Steve Ebersole
 */
public class ConcreteSqmSelectQueryPlan<R> implements SelectQueryPlan<R> {
	private final SqmSelectStatement sqm;
	private final RowTransformer<R> rowTransformer;

	public ConcreteSqmSelectQueryPlan(
			SqmSelectStatement sqm,
			Class<R> resultType,
			QueryOptions queryOptions) {
		this.sqm = sqm;

		this.rowTransformer = determineRowTransformer( sqm, resultType, queryOptions );
	}

	@SuppressWarnings("unchecked")
	private RowTransformer<R> determineRowTransformer(
			SqmSelectStatement sqm,
			Class<R> resultType,
			QueryOptions queryOptions) {
		if ( resultType == null || resultType.isArray() ) {
			if ( queryOptions.getTupleTransformer() != null ) {
				return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
			}
			else {
				return RowTransformerPassThruImpl.instance();
			}
		}

		// NOTE : if we get here, a result-type of some kind (other than Object[].class) was specified

		if ( Tuple.class.isAssignableFrom( resultType ) ) {
			// resultType is Tuple..
			if ( queryOptions.getTupleTransformer() == null ) {
				final List<TupleElement<?>> tupleElementList = new ArrayList<>();
				for ( SqmSelection selection : sqm.getQuerySpec().getSelectClause().getSelections() ) {
					tupleElementList.add(
							new TupleElementImpl(
									selection.getExpression().getExpressionType().getJavaTypeDescriptor().getJavaType(),
									selection.getAlias()
							)
					);
				}
				return (RowTransformer<R>) new RowTransformerTupleImpl( tupleElementList );
//				return (RowTransformer<R>) new RowTransformerTupleImpl(
//						sqm.getQuerySpec().getSelectClause().getSelections()
//								.stream()
//								.map( selection -> (TupleElement<?>) new TupleElementImpl(
//										( (SqmTypeImplementor) selection.asExpression().getExpressionType() ).getDomainType().getReturnedClass(),
//										selection.getAlias()
//								) )
//								.collect( Collectors.toList() )
//				);
			}

			// there can be a TupleTransformer IF it is a JpaTupleBuilder,
			// otherwise this is considered an error
			if ( queryOptions.getTupleTransformer() instanceof JpaTupleBuilder ) {
				return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
			}

			throw new IllegalArgumentException(
					"Illegal combination of Tuple resultType and (non-JpaTupleBuilder) TupleTransformer : " +
							queryOptions.getTupleTransformer()
			);
		}

		// NOTE : if we get here we have a resultType of some kind

		if ( queryOptions.getTupleTransformer() != null ) {
			// aside from checking the type parameters for the given TupleTransformer
			// there is not a decent way to verify that the TupleTransformer returns
			// the same type.  We rely on the API here and assume the best
			return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
		}
		else if ( sqm.getQuerySpec().getSelectClause().getSelections().size() > 1 ) {
			throw new IllegalQueryOperationException( "Query defined multiple selections, return cannot be typed (other that Object[] or Tuple)" );
		}
		else {
			return RowTransformerSingularReturnImpl.instance();
		}
	}

	@SuppressWarnings("unchecked")
	private RowTransformer makeRowTransformerTupleTransformerAdapter(
			SqmSelectStatement sqm,
			QueryOptions queryOptions) {
		return new RowTransformerTupleTransformerAdapter<>(
				sqm.getQuerySpec().getSelectClause().getSelections()
						.stream()
						.map( SqmSelection::getAlias )
						.collect( StingArrayCollector.INSTANCE ),
				queryOptions.getTupleTransformer()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<R> performList(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		verifyQueryIsSelect();

		final Callback callback = afterLoadAction -> {
			// do nothing here
		};

		final JdbcSelect jdbcSelect = buildJdbcSelect(
				persistenceContext,
				queryOptions,
				inputParameterBindings,
				callback
		);


		return new JdbcSelectExecutorStandardImpl().list(
				jdbcSelect,
				queryOptions,
				inputParameterBindings,
				rowTransformer,
				callback,
				persistenceContext
		);
	}

	private JdbcSelect buildJdbcSelect(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings, Callback callback) {
		// todo (6.0) : SqmSelectToSqlAstConverter needs to account for the EntityGraph hint
		final SqlSelectPlan interpretation = SqmSelectToSqlAstConverter.interpret(
				sqm,
				queryOptions,
				new SqlAstBuildingContext() {
					@Override
					public SessionFactoryImplementor getSessionFactory() {
						return persistenceContext.getFactory();
					}

					@Override
					public Callback getCallback() {
						return callback;
					}
				}
		);

		// todo (6.0) : is there any benefit to building SqlSelectPlan and then JdbcSelect in successive steps?
		//		as opposed to combining into one step
		return SqlSelectAstToJdbcSelectConverter.interpret(
				interpretation,
				persistenceContext,
				inputParameterBindings,
				Collections.emptyList()
		);
	}

	private void verifyQueryIsSelect() {
		if ( !SqmSelectStatement.class.isInstance( sqm ) ) {
			throw new IllegalQueryOperationException(
					"Query is not a SELECT statement [" + sqm.getClass().getSimpleName() + "]"
			);
		}
	}

	@Override
	public Iterator<R> performIterate(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings) {
		verifyQueryIsSelect();

		// todo : implement
		throw new NotYetImplementedException( "Query#iterate not yet implemented" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public ScrollableResultsImplementor performScroll(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			QueryParameterBindings inputParameterBindings,
			ScrollMode scrollMode) {
		verifyQueryIsSelect();

		final Callback callback = afterLoadAction -> {
			// do nothing here
		};

		// todo : SqmSelectToSqlAstConverter needs to account for the EntityGraph hint
		final JdbcSelect jdbcSelect = buildJdbcSelect(
				persistenceContext,
				queryOptions,
				inputParameterBindings,
				callback
		);

		return new JdbcSelectExecutorStandardImpl().scroll(
				jdbcSelect,
				scrollMode,
				queryOptions,
				inputParameterBindings,
				rowTransformer,
				callback,
				persistenceContext
		);
	}
}