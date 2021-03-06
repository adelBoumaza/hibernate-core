/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * A column in a IdTable.  As these columns mirror the entity id columns, we know a few things about it inherently,
 * such as being non-nullable
 *
 * @author Steve Ebersole
 */
public class IdTableColumn {
	private final IdTable containingTable;
	private final String columnName;
	private final JdbcMapping jdbcMapping;

	public IdTableColumn(
			IdTable containingTable,
			String columnName,
			JdbcMapping jdbcMapping) {
		this.containingTable = containingTable;
		this.columnName = columnName;
		this.jdbcMapping = jdbcMapping;
	}

	public IdTable getContainingTable() {
		return containingTable;
	}

	public String getColumnName() {
		return columnName;
	}

	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	public String getDefaultValue() {
		return null;
	}

	public String getSqlTypeDefinition() {
		// todo (6.0) : this really ought to get the type def from the persister
		return null;
	}
}
