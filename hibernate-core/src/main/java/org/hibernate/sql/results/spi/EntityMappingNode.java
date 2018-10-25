/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * Represents a reference to an entity either as a return, fetch, or collection element or index.
 *
 * @author Steve Ebersole
 */
public interface EntityMappingNode extends ResultSetMappingNode, FetchParent {
	/**
	 * Retrieves the entity persister describing the entity associated with this Return.
	 *
	 * @return The EntityPersister.
	 */
	EntityDescriptor getEntityDescriptor();

	DomainResult getIdentifierResult();

	DomainResult getDiscriminatorResult();

	DomainResult getVersionResult();

	// todo (6.0) : this needs some form of tracking basic attributes to fetch
	//		internally.  that info does not need to be exposed, it just needs to
	//		make its way into the created assemblers
	//
	// 		for now, we assume all basic attributes should be fetched / loaded
}
