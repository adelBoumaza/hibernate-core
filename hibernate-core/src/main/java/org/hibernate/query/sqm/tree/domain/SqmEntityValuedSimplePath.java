/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SqmCreationState;

/**
 * @author Steve Ebersole
 */
public class SqmEntityValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmEntityValuedSimplePath(
			String navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPathSource referencedPathSource = getReferencedPathSource();
		final SqmPathSource subPathSource = referencedPathSource.findSubPathSource( name );

		assert getLhs() == null || creationState.getProcessingStateStack()
				.getCurrent()
				.getPathRegistry()
				.findPath( getLhs().getNavigablePath() ) != null;

		//noinspection unchecked
		return subPathSource.createSqmPath( this, creationState );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEntityValuedPath( this );
	}

	@Override
	public EntityDomainType<T> getNodeType() {
		//noinspection unchecked
		return (EntityDomainType<T>) getReferencedPathSource().getSqmPathType();
	}

	@Override
	public <S extends T> SqmTreatedSimplePath<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		return (SqmTreatedSimplePath<T, S>) treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		//noinspection unchecked
		return new SqmTreatedSimplePath( this, treatTarget, nodeBuilder() );
	}

}
