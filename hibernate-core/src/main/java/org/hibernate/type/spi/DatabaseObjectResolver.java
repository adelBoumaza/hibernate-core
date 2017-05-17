/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.persister.model.relational.spi.Column;
import org.hibernate.persister.model.relational.spi.Table;

/**
 * @author Steve Ebersole
 */
public interface DatabaseObjectResolver {
	Table resolveTable(MappedTable mappedTable);

	Column resolveColumn(MappedColumn mappedColumn);
}
