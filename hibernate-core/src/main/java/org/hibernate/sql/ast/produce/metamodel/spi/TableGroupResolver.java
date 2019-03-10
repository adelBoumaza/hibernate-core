/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.annotations.Remove;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

/**
 * @author Steve Ebersole
 *
 * @deprecated Use {@link }org.hibernate.sql.results.spi.DomainResultCreationState.FromClauseAccess} instead
 */
@Remove
@Deprecated
public interface TableGroupResolver {
	/**
	 * Resolve a TableGroup by its unique identifier
	 */
	TableGroup resolveTableGroup(String uid);

	TableGroup resolveTableGroup(NavigablePath navigablePath);
}
