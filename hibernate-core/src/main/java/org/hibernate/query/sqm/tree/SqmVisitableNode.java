/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.sqm.SemanticQueryWalker;

/**
 * Optional contract for SqmNode implementations that can be visited
 * by a SemanticQueryWalker.
 *
 * @author Steve Ebersole
 */
public interface SqmVisitableNode extends SqmNode {
	/**
	 * Accept the walker per visitation
	 */
	<X> X accept(SemanticQueryWalker<X> walker);
}
