/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.sql.ast.produce.spi.TableGroupProducer;

/**
 * @author Steve Ebersole
 */
public interface CollectionElementEntity<E> extends CollectionElement<E>, EntityValuedNavigable<E>, TableGroupProducer {
	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionElementEntity( this );
	}
}