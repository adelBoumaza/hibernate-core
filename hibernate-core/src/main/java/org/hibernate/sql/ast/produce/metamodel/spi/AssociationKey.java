/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import java.util.List;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.spi.Column;

/**
 * Used to uniquely identify a foreign key, so that we don't join it more than once creating circularities.  Note
 * that the table+columns refers to the association owner.  These are used to help detect bi-directional associations
 * since the Hibernate runtime metamodel (persisters) do not inherently know this information.  For example, consider
 * the Order -> Customer and Customer -> Order(s) bi-directional association; both would be mapped to the
 * {@code ORDER_TABLE.CUST_ID} column.  That is the purpose of this struct.
 * <p/>
 * Bit of a misnomer to call this an association attribute.  But this follows the legacy use of AssociationKey
 * from old JoinWalkers to denote circular join detection
 *
 * @author Steve Ebersole
 * @author Gail Badner
 * @author Gavin King
 * @author Andrea Boriero
 */
public class AssociationKey {
	// todo (6.0) : just use the FK's uid...
	//		1) currently FK has no uid (heck prior to 6 runtime did not even have FK), so we'd have to add that
	//		2) seems most reasonable to have these kinds of "uid"s use UUID.  It's self-documenting and still
	// 			gives same good performance for Map keys as the String version (at worse using UUID#toString`
	//			for the keys)

	private final String table;
	private final List<Column> columns;

	/**
	 * Create the AssociationKey.
	 *
	 * @param table The table part of the association key
	 * @param columns The columns that define the association key
	 */
	public AssociationKey(String table, List<Column> columns) {
		this.table = table;
		this.columns = columns;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final AssociationKey that = (AssociationKey) o;
		return table.equals( that.table ) && columns.equals( that.columns );

	}

	@Override
	public int hashCode() {
		return table.hashCode();
	}

	private String str;

	@Override
	public String toString() {
		if ( str == null ) {
			str = "AssociationKey(table=" + table + ", columns={" + StringHelper.join( ",", columns ) + "})";
		}
		return str;
	}
}
