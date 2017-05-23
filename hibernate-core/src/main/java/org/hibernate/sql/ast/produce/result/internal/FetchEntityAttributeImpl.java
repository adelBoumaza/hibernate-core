/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.consume.results.internal.EntityFetchInitializerImpl;
import org.hibernate.sql.ast.consume.results.spi.Initializer;
import org.hibernate.sql.ast.consume.results.spi.InitializerCollector;
import org.hibernate.sql.ast.consume.results.spi.InitializerParent;
import org.hibernate.sql.ast.produce.result.spi.EntityIdentifierReference;
import org.hibernate.sql.ast.produce.result.spi.FetchEntityAttribute;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public class FetchEntityAttributeImpl extends AbstractFetchParent implements FetchEntityAttribute {
	private final FetchParent fetchParent;
	private final FetchStrategy fetchStrategy;

	private final EntityFetchInitializerImpl initializer;

	public FetchEntityAttributeImpl(
			FetchParent fetchParent,
			EntityReference entityReference,
			NavigablePath navigablePath,
			FetchStrategy fetchStrategy) {
		super( entityReference, navigablePath );
		this.fetchParent = fetchParent;
		this.fetchStrategy = fetchStrategy;

		this.initializer = new EntityFetchInitializerImpl(
				fetchParent.getInitializerParentForFetchInitializers(),
				this,
				null,
				false
		);
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public NavigableReference getFetchedNavigableReference() {
		return getNavigableContainerReference();
	}

	@Override
	public SingularPersistentAttributeEntity getFetchedAttributeDescriptor() {
		return (SingularPersistentAttributeEntity) getFetchedNavigableReference().getNavigable();
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public boolean isNullable() {
		throw new NotYetImplementedException(  );
	}

//	@Override
//	public ResolvedFetch resolve(
//			ResolvedFetchParent resolvedFetchParent,
//			Map<AttributeDescriptor, SqlSelectionGroup> sqlSelectionGroupMap,
//			boolean shallow) {
//		return new ResolvedFetchEntityImpl(
//				this,
//				resolvedFetchParent,
//				fetchStrategy,
//				sqlSelectionGroupMap,
//				shallow
//		);
//	}

	@Override
	public EntityTypeImplementor getEntityMetadata() {
		return getFetchedAttributeDescriptor().getAssociatedEntityDescriptor();
	}

	@Override
	public EntityIdentifierReference getIdentifierReference() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		collector.addInitializer( getInitializer() );
		addFetchInitializers( collector );
	}

	@Override
	public Initializer getInitializer() {
		return initializer;
	}

	@Override
	public InitializerParent getInitializerParentForFetchInitializers() {
		return initializer;
	}
}