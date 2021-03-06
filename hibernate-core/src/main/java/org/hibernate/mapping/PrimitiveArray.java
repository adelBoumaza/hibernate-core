/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * A primitive array has a primary key consisting of the key columns + index column.
 */
public class PrimitiveArray extends Array {
	public PrimitiveArray(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public boolean isPrimitiveArray() {
		return true;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
