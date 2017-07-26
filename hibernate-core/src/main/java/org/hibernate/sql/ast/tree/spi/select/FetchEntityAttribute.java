/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.query.NavigablePath;

/**
 * @author Steve Ebersole
 */
public interface FetchEntityAttribute extends EntityReference, FetchAttribute {
	@Override
	SingularPersistentAttributeEntity getFetchedAttributeDescriptor();

	default NavigablePath getNavigablePath() {
		return getFetchParent().getNavigablePath().append( getFetchedAttributeDescriptor().getNavigableName() );
	}
}