/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmNavigableReference implements SqmNavigableReference {
	private String explicitAlias;

	@Override
	public String getExplicitAlias() {
		return explicitAlias;
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		this.explicitAlias = explicitAlias;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + getNavigablePath().getFullPath() + ")";
	}
}
