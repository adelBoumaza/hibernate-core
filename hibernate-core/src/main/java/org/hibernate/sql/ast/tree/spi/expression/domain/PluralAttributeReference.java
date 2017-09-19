/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeReference implements NavigableReference {
	private final NavigableContainerReference containerReference;
	private final PluralPersistentAttribute referencedAttribute;
	private final NavigablePath navigablePath;

	public PluralAttributeReference(
			NavigableContainerReference containerReference,
			PluralPersistentAttribute referencedAttribute,
			NavigablePath navigablePath) {
		this.containerReference = containerReference;
		this.referencedAttribute = referencedAttribute;
		this.navigablePath = navigablePath;
	}

	@Override
	public ColumnReferenceQualifier getSqlExpressionQualifier() {
		// todo (6.0) : we need a combined TableSpace to act as the qualifier
		//		combining collection-table, element table and index table
		throw new NotYetImplementedException(  );
	}

	@Override
	public Navigable getNavigable() {
		return referencedAttribute;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return containerReference;
	}
}