/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEntityValuedSimplePath extends AbstractSqmSimplePath {
	public SqmEntityValuedSimplePath(
			String uid,
			NavigablePath navigablePath,
			EntityValuedNavigable<?> referencedNavigable,
			SqmPath lhs) {
		super( uid, navigablePath, referencedNavigable, lhs );
	}

	public SqmEntityValuedSimplePath(
			String uid,
			NavigablePath navigablePath,
			EntityValuedNavigable<?> referencedNavigable,
			SqmPath lhs, String explicitAlias) {
		super( uid, navigablePath, referencedNavigable, lhs, explicitAlias );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityValuedPath( this );
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return null;
	}

	@Override
	public EntityValuedNavigable getReferencedNavigable() {
		return (EntityValuedNavigable) super.getReferencedNavigable();
	}

	@Override
	public EntityValuedNavigable getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public Supplier<? extends EntityValuedNavigable> getInferableType() {
		return this::getReferencedNavigable;
	}

	@Override
	public EntityJavaDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState,
//			DomainResultCreationContext creationContext) {
//		return new EntityResultImpl(
//				getNavigablePath(),
//				getReferencedNavigable(),
//				resultVariable,
//				creationState,
//				creationContext
//		);
//	}
}
