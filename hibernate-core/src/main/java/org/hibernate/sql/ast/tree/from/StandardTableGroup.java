/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlAliasBase;

/**
 * @author Steve Ebersole
 */
public class StandardTableGroup extends AbstractTableGroup {
	private final TableReference primaryTableReference;
	private final List<TableReferenceJoin> tableJoins;

	public StandardTableGroup(
			String navigablePath,
			RootTableGroupProducer tableGroupProducer,
			LockMode lockMode,
			TableReference primaryTableReference,
			List<TableReferenceJoin> tableJoins,
			SqlAliasBase sqlAliasBase,
			SessionFactoryImplementor sessionFactory) {
		super( navigablePath, tableGroupProducer, lockMode, sqlAliasBase, sessionFactory );
		this.primaryTableReference = primaryTableReference;
		this.tableJoins = tableJoins;
	}

	@Override
	public RootTableGroupProducer getModelPart() {
		return (RootTableGroupProducer) super.getModelPart();
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		// todo (6.0) : if we implement dynamic TableReference creation, this still needs to return the expressions for all mapped tables not just the ones with a TableReference at this time
		nameCollector.accept( getPrimaryTableReference().getTableExpression() );
		for ( TableReferenceJoin tableReferenceJoin : tableJoins ) {
			nameCollector.accept( tableReferenceJoin.getJoinedTableReference().getTableExpression() );
		}
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return primaryTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return tableJoins;
	}
}
