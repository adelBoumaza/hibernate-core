/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.produce.internal.UniqueIdGenerator;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.internal.StandardSqlExpressionResolver;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.NavigablePathStack;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.RootTableGroupProducer;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.domain.BasicValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EmbeddableValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class MetamodelSelectBuilderProcess
		implements QueryResultCreationContext, SqlAstBuildingContext {

	public static SqlAstSelectDescriptor createSelect(
			SessionFactoryImplementor sessionFactory,
			NavigableContainer rootNavigableContainer,
			List<Navigable<?>> navigablesToSelect,
			Navigable restrictedNavigable,
			QueryResult queryResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		final MetamodelSelectBuilderProcess process = new MetamodelSelectBuilderProcess(
				sessionFactory,
				rootNavigableContainer,
				navigablesToSelect,
				restrictedNavigable,
				queryResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions
		);

		return process.execute();
	}

	private final SessionFactoryImplementor sessionFactory;
	private final NavigableContainer rootNavigableContainer;
	private final List<Navigable<?>> navigablesToSelect;
	private final Navigable restrictedNavigable;
	private final QueryResult queryResult;
	private final int numberOfKeysToLoad;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;


	private final Stack<TableSpace> tableSpaceStack = new StandardStack<>();
	private final Stack<TableGroup> tableGroupStack = new StandardStack<>();
	private final Stack<FetchParent> fetchParentStack = new StandardStack<>();
	private final NavigablePathStack navigablePathStack = new NavigablePathStack();
	private final Set<String> affectedTables = new HashSet<>();

	private final QuerySpec rootQuerySpec = new QuerySpec( true );

	private final StandardSqlExpressionResolver sqlExpressionResolver = new StandardSqlExpressionResolver(
			() -> rootQuerySpec,
			expression -> expression,
			(expression, selection) -> {}
	);

	private MetamodelSelectBuilderProcess(
			SessionFactoryImplementor sessionFactory,
			NavigableContainer rootNavigableContainer,
			List<Navigable<?>> navigablesToSelect,
			Navigable restrictedNavigable,
			QueryResult queryResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		this.sessionFactory = sessionFactory;
		this.rootNavigableContainer = rootNavigableContainer;
		this.navigablesToSelect = navigablesToSelect;
		this.restrictedNavigable = restrictedNavigable;
		this.queryResult = queryResult;
		this.numberOfKeysToLoad = numberOfKeysToLoad;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions != null ? lockOptions : LockOptions.NONE;
	}

	private SqlAstSelectDescriptor execute() {
		navigablePathStack.push( rootNavigableContainer );

		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec );

		final UniqueIdGenerator uidGenerator = new UniqueIdGenerator();
		final String uid = uidGenerator.generateUniqueId();

		final SqlAliasBaseManager aliasBaseManager = new SqlAliasBaseManager();
		final TableSpace rootTableSpace = rootQuerySpec.getFromClause().makeTableSpace();
		tableSpaceStack.push( rootTableSpace );

		final TableGroup rootTableGroup = makeRootTableGroup( uid, rootQuerySpec, rootTableSpace, aliasBaseManager );
		rootTableSpace.setRootTableGroup( rootTableGroup );
		tableGroupStack.push( rootTableGroup );

		final List<QueryResult> queryResults;

		if ( navigablesToSelect != null && ! navigablesToSelect.isEmpty() ) {
			queryResults = new ArrayList<>();
			for ( Navigable navigable : navigablesToSelect ) {
				final NavigableReference navigableReference = makeNavigableReference( rootTableGroup, navigable );
				queryResults.add(
						navigable.createQueryResult(
								navigableReference,
								null,
								this
						)
				);
			}
		}
		else {
			// use the one passed to the constructor or create one (maybe always create and pass?)
			//		allows re-use as they can be re-used to save on memory - they
			//		do not share state between
			final QueryResult queryResult;
			if ( this.queryResult != null ) {
				// used the one passed to the constructor
				queryResult = this.queryResult;
			}
			else {
				// create one
				queryResult = rootNavigableContainer.createQueryResult(
						rootTableSpace.getRootTableGroup().getNavigableReference(),
						null,
						this
				);
			}

			queryResults = Collections.singletonList( queryResult );

			// todo (6.0) : process fetches & entity-graphs
			fetchParentStack.push( (FetchParent) queryResult );
		}

		// add the id/uk/fk restriction
		rootQuerySpec.addRestriction(
				AstNodeHelper.createRestriction(
						numberOfKeysToLoad,
						rootTableGroup,
						this,
						restrictedNavigable
				)
		);

		return new SqlAstSelectDescriptorImpl(
				selectStatement,
				queryResults,
				affectedTables
		);
	}

	public static NavigableReference makeNavigableReference(TableGroup rootTableGroup, Navigable navigable) {
		if ( navigable instanceof BasicValuedNavigable ) {
			return new BasicValuedNavigableReference(
					(NavigableContainerReference) rootTableGroup.getNavigableReference(),
					(BasicValuedNavigable) navigable,
					rootTableGroup.getNavigableReference().getNavigablePath().append( navigable.getNavigableName() )
			);
		}

		if ( navigable instanceof EmbeddedValuedNavigable ) {
			return new EmbeddableValuedNavigableReference(
					(NavigableContainerReference) rootTableGroup.getNavigableReference(),
					(EmbeddedValuedNavigable) navigable,
					rootTableGroup.getNavigableReference().getNavigablePath().append( navigable.getNavigableName() ),
					LockMode.NONE
			);
		}

		if ( navigable instanceof EntityValuedNavigable ) {
			// todo (6.0) : join?
			return new EntityValuedNavigableReference(
					(NavigableContainerReference) rootTableGroup.getNavigableReference(),
					(EntityValuedNavigable) navigable,
					rootTableGroup.getNavigableReference().getNavigablePath().append( navigable.getNavigableName() ),
					rootTableGroup,
					LockMode.NONE
			);
		}

		throw new NotYetImplementedFor6Exception();
	}

	private TableGroup makeRootTableGroup(
			String uid,
			QuerySpec querySpec,
			TableSpace rootTableSpace,
			SqlAliasBaseManager aliasBaseManager) {
		return ( (RootTableGroupProducer) rootNavigableContainer ).createRootTableGroup(
				new TableGroupInfo() {
					@Override
					public String getUniqueIdentifier() {
						return uid;
					}

					@Override
					public String getIdentificationVariable() {
						// todo (6.0) : is "root" a reserved word?
						return "root";
					}

					@Override
					public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
						return null;
					}
				},
				new RootTableGroupContext() {
					@Override
					public void addRestriction(Predicate predicate) {
						querySpec.addRestriction( predicate );
					}

					@Override
					public QuerySpec getQuerySpec() {
						return querySpec;
					}

					@Override
					public TableSpace getTableSpace() {
						return rootTableSpace;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return aliasBaseManager;
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						return null;
					}

					@Override
					public LockOptions getLockOptions() {
						return lockOptions;
					}
				}
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstBuildingContext

	private List<AfterLoadAction> afterLoadActions;

	private final Callback sqlAstCreationCallback = new Callback() {
		@Override
		public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
			if ( afterLoadActions == null ) {
				afterLoadActions = new ArrayList<>();
			}
			afterLoadActions.add( afterLoadAction );
		}
	};

	@Override
	public Callback getCallback() {
		return sqlAstCreationCallback;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return QueryOptions.NONE;
	}

	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return sqlExpressionResolver;
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryResultCreationContext

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}
}
