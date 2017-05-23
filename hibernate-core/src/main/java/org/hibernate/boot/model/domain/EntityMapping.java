/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.EntityMode;
import org.hibernate.boot.model.relational.MappedTable;

/**
 * @author Steve Ebersole
 */
public interface EntityMapping extends IdentifiableTypeMapping {
	String getEntityName();
	String getJpaEntityName();

	EntityMode getEntityMode();
	String getExplicitTuplizerClassName();

	Class getEntityPersisterClass();
	void setEntityPersisterClass(Class entityPersisterClass);

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	MappedTable getRootTable();
}