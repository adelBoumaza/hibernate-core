/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql.navigable;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.domain.AbstractSqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.from.SqmDowncast;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;

/**
 * Models an "incidental downcast", as opposed to an intrinsic downcast.  An
 * intrinsic downcast occurs in the from-clause - the downcast target becomes
 * an intrinsic part of the FromElement (see {@link SqmFrom#getIntrinsicSubclassEntityMetadata()}.
 * An incidental downcast, on the other hand, occurs outside the from-clause.
 * <p/>
 * For example,
 * {@code .. from Person p where treat(p.address as USAddress).zip=? ...} represents
 * such an intrinsic downcast of Address to one of its subclasses named USAddress.
 *
 * todo (6.0) : maybe "localized downcast" is a better term here, rather than "incidental downcast".
 * 		tbh, "incidental" makes me think more of Hibernate's "implicit downcast" feature.
 *
 * @author Steve Ebersole
 */
public class TreatedNavigableReference extends AbstractSqmNavigableReference implements SqmNavigableReference,
		SqmNavigableContainerReference {
	private final SqmNavigableReference baseBinding;
	private final EntityValuedExpressableType subclassIndicator;

	public TreatedNavigableReference(SqmNavigableReference baseBinding, EntityValuedExpressableType subclassIndicator) {
		this.baseBinding = baseBinding;
		this.subclassIndicator = subclassIndicator;

		baseBinding.addDowncast( new SqmDowncast( subclassIndicator ) );
	}

	public EntityValuedExpressableType getSubclassIndicator() {
		return subclassIndicator;
	}

	@Override
	public Navigable getReferencedNavigable() {
		return subclassIndicator;
	}

	@Override
	public SqmNavigableContainerReference getSourceReference() {
		return baseBinding.getSourceReference();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return baseBinding.getNavigablePath();
	}

	@Override
	public ExpressableType getExpressionType() {
		return subclassIndicator;
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return baseBinding.accept( walker );
	}

	@Override
	public String asLoggableText() {
		return "TREAT( " + baseBinding.asLoggableText() + " AS " + subclassIndicator.asLoggableText() + " )";
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return ( (SqmFromExporter) baseBinding ).getExportedFromElement();
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		( (SqmFromExporter) baseBinding ).injectExportedFromElement( sqmFrom );
	}
}