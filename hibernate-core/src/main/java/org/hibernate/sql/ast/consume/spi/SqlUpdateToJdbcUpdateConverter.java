/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.tree.spi.UpdateStatement;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.ParameterSpec;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * @author Steve Ebersole
 */
public class SqlUpdateToJdbcUpdateConverter
		extends AbstractSqlAstToJdbcOperationConverter
		implements SqlMutationToJdbcMutationConverter {

	// todo (6.0) : do we need to limit rendering the qualification alias for columns?
	//		we could also control this when we build the SQL AST

	public static JdbcUpdate interpret(
			SqlAstUpdateDescriptor sqlAst,
			SessionFactoryImplementor sessionFactory) {
		final SqlUpdateToJdbcUpdateConverter walker = new SqlUpdateToJdbcUpdateConverter( sessionFactory );

		walker.processUpdateStatement( sqlAst.getSqlAstStatement() );

		return new JdbcUpdate() {
			@Override
			public String getSql() {
				return walker.getSql();
			}

			@Override
			public List<ParameterSpec> getJdbcParameters() {
				return walker.getParameterSpecs();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return walker.getAffectedTableNames();
			}
		};
	}

	public SqlUpdateToJdbcUpdateConverter(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	private void processUpdateStatement(UpdateStatement updateStatement) {
		appendSql( "update " );

		final PhysicalTable targetTable = (PhysicalTable) updateStatement.getTargetTable().getTable();
		final String tableName = getSessionFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getQualifiedObjectNameFormatter()
				.format(
						targetTable.getQualifiedTableName(),
						getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect()
				);

		appendSql( tableName );

		// todo (6.0) : need to render the target column list
		// 		for now we do not render


		boolean firstPass = true;
		for ( Assignment assignment : updateStatement.getAssignments() ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				appendSql( ", " );
			}

			visitAssignment( assignment );
		}
	}


	@Override
	@SuppressWarnings("unchecked")
	public void visitAssignment(Assignment assignment) {
		assignment.getColumnReference().accept( this );
		appendSql( " = " );
		assignment.getAssignedValue().accept( this );
	}


}
