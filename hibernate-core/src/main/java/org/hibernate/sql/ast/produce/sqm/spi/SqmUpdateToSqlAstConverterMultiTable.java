/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.set.SqmAssignment;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.SqlTreeException;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.UpdateStatement.UpdateStatementBuilder;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.SingularAttributeReference;
import org.hibernate.sql.ast.tree.spi.from.AbstractTableGroup;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmUpdateToSqlAstConverterMultiTable
		extends BaseSqmToSqlAstConverter implements SqlSelectionGroupResolutionContext {
	private static final Logger log = Logger.getLogger( SqmUpdateToSqlAstConverterMultiTable.class );

	public static List<SqlAstUpdateDescriptor> interpret(
			SqmUpdateStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			SqlAstBuildingContext sqlAstBuildingContext) {

		final SqmUpdateToSqlAstConverterMultiTable walker = new SqmUpdateToSqlAstConverterMultiTable(
				sqmStatement,
				idTableSelect,
				queryOptions,
				sqlAstBuildingContext
		);

		walker.visitUpdateStatement( sqmStatement );

		return walker.updateStatementBuilderMap.entrySet().stream()
				.map( entry -> entry.getValue().createUpdateDescriptor() )
				.collect( Collectors.toList() );
	}


	private final EntityDescriptor entityDescriptor;
	private final EntityTableGroup entityTableGroup;

	private final QuerySpec idTableSelect;

	private AssignmentContext currentAssignmentContext;

	private Map<TableReference, UpdateStatementBuilder> updateStatementBuilderMap = new HashMap<>();

	private SqmUpdateToSqlAstConverterMultiTable(
			SqmUpdateStatement sqmStatement,
			QuerySpec idTableSelect,
			QueryOptions queryOptions,
			SqlAstBuildingContext sqlAstBuildingContext) {
		super( sqlAstBuildingContext, queryOptions );
		this.idTableSelect = idTableSelect;

		this.entityDescriptor = sqmStatement.getEntityFromElement()
				.getNavigableReference()
				.getExpressableType()
				.getEntityDescriptor();

		// Ask the entity descriptor to create a TableGroup.  This TableGroup
		//		will contain all the individual TableReferences we need..
		//
		// here though we will need to "put into buckets" all the assignments that
		// 		need to be executed against a particular table in the group.  this
		//		should be doable based on a custom SqlAstWalker converter for update
		//		statements which keeps track of the table of the attribute being assigned
		//		in the query so that we know some context for these assigned values.

		this.entityTableGroup = entityDescriptor.createRootTableGroup(
				new TableGroupInfo() {
					@Override
					public String getUniqueIdentifier() {
						return sqmStatement.getEntityFromElement().getUniqueIdentifier();
					}

					@Override
					public String getIdentificationVariable() {
						return sqmStatement.getEntityFromElement().getIdentificationVariable();
					}

					@Override
					public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
						return sqmStatement.getEntityFromElement().getIntrinsicSubclassEntityMetadata();
					}
				},
				new RootTableGroupContext() {
					@Override
					public void addRestriction(Predicate predicate) {
					}

					@Override
					public QuerySpec getQuerySpec() {
						return null;
					}

					@Override
					public TableSpace getTableSpace() {
						return null;
					}

					@Override
					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
						return getSqlAliasBaseManager();
					}

					@Override
					public JoinType getTableReferenceJoinType() {
						return JoinType.INNER;
					}
				}
		);

		final TableGroupMock tableGroup = new TableGroupMock( entityTableGroup );
		tableGroup.applyAffectedTableNames( affectedTableNames()::add );

		primeStack( getTableGroupStack(), tableGroup );
		getFromClauseIndex().crossReference( sqmStatement.getEntityFromElement(), tableGroup );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Assignment visitAssignment(SqmAssignment sqmAssignment) {
		currentAssignmentContext = new AssignmentContext( sqmAssignment );

		try {
			final SingularAttributeReference stateField = (SingularAttributeReference) sqmAssignment.getStateField().accept( this );
			currentAssignmentContext.endStateFieldProcessing();
			final Expression assignedValue = (Expression) sqmAssignment.getStateField().accept( this );

			// todo (6.0) : consider SingularAttributeReference#generateSqlAssignments returning a List of Assignment references
			//		representing the update of its (updatable) columns
			// todo (6.0) : Do we want to allow updates against embedded values as well?
			// 		- JPA defines support for updates only against basic (single-column) types,
			//		this would be an "extension" feature as well as needing to be one of the "strict compliance checks"

			// todo (6.0) : this also illustraes why being able to ask the Navigable for its list of (Sql)Expressions, rather than just SqlSelections, is important
			//		in fact its probably a good idea to have Navigable be the place where we build all
			// 		SqlNode references (TableGroups, Expressions, Assignments, etc

//			final SqlSelectionGroup sqlSelectionGroup = stateField.getNavigable().resolveSqlSelectionGroup(
//					entityTableGroup,
//					this
//			);
//			assert sqlSelectionGroup.getSqlSelections().size() == 1;

			final Assignment assignment = new Assignment(
					// this needs to be a ColumnReference (the Expression) rather than a SqlSelection, as noted above in todos
					//sqlSelectionGroup.getSqlSelections().get( 0 ),
					null,
					assignedValue
			);
			final TableReference assignmentTableReference = currentAssignmentContext.stateFieldTableReference;
			UpdateStatementBuilder concreteUpdateStatementBuilder = updateStatementBuilderMap.get( assignmentTableReference );
			if ( concreteUpdateStatementBuilder == null ) {
				concreteUpdateStatementBuilder = new UpdateStatementBuilder( assignmentTableReference );
				concreteUpdateStatementBuilder.addRestriction(
						new InSubQueryPredicate(
								null,
								idTableSelect,
								false
						)
				);
				updateStatementBuilderMap.put( assignmentTableReference, concreteUpdateStatementBuilder );
			}
			concreteUpdateStatementBuilder.addAssignment( assignment );

			return assignment;
		}
		finally {
			currentAssignmentContext = null;
		}
	}

	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return this;
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		return false;
	}

	private class TableGroupMock extends AbstractTableGroup implements TableGroup {
		private final EntityTableGroup entityTableGroup;

		private TableGroupMock(EntityTableGroup entityTableGroup) {
			super( entityTableGroup.getTableSpace(), entityTableGroup.getUniqueIdentifier() );
			this.entityTableGroup = entityTableGroup;
		}

		@Override
		public TableReference locateTableReference(Table table) {
			final TableReference tableReference = entityTableGroup.locateTableReference( table );

			if ( currentAssignmentContext != null ) {
				currentAssignmentContext.injectTableReference( tableReference );
			}

			return tableReference;
		}

		@Override
		public NavigableReference asExpression() {
			return null;
		}

		@Override
		public void render(SqlAppender sqlAppender, SqlAstWalker walker) {

		}

		@Override
		protected TableReference getPrimaryTableReference() {
			return entityTableGroup.locateTableReference( entityTableGroup.getEntityDescriptor().getPrimaryTable() );
		}

		@Override
		protected List<TableReferenceJoin> getTableReferenceJoins() {
			return null;
		}
	}

	private static class AssignmentContext {
		private final String assignmentText;
		private boolean processingStateField;

		private TableReference stateFieldTableReference;

		private AssignmentContext(SqmAssignment assignment) {
			this.assignmentText = assignment.getStateField().asLoggableText() + " = " +
					assignment.getValue().asLoggableText();

			log.debugf( "Initializing AssignmentContext [%s]", assignmentText );
		}

		private void injectTableReference(TableReference tableReference) {
			if ( processingStateField ) {
				if ( stateFieldTableReference != null ) {
					throw new ConversionException( "Multiple TableReferences found for assignment state-field" );
				}
				else {
					stateFieldTableReference = tableReference;
				}
			}
			else {
				// `stateFieldTableReference != null` already validated during `endStateFieldProcessing`
				if ( !stateFieldTableReference.equals( tableReference ) ) {
					throw new SqlTreeException( "Assignment as part of multi-table update query referenced multiple tables [" + assignmentText + "]" );
				}
				this.stateFieldTableReference = tableReference;
			}
		}

		private void endStateFieldProcessing() {
			if ( stateFieldTableReference == null ) {
				throw new SqlTreeException( "Could not determine backing TableReference for assignment state-field" );
			}

			processingStateField = false;
		}
	}
}