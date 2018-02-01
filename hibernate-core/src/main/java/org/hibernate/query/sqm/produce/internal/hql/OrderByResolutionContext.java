/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.internal.FromElementBuilder;
import org.hibernate.query.sqm.produce.spi.FromElementLocator;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.produce.spi.ResolutionContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;

/**
 * @author Steve Ebersole
 */
public class OrderByResolutionContext implements ResolutionContext, FromElementLocator {
	private final ParsingContext parsingContext;
	private final SqmFromClause fromClause;
	private final SqmSelectClause selectClause;

	public OrderByResolutionContext(ParsingContext parsingContext, SqmFromClause fromClause, SqmSelectClause selectClause) {
		this.parsingContext = parsingContext;
		this.fromClause = fromClause;
		this.selectClause = selectClause;
	}

	@Override
	public SqmNavigableReference findNavigableReferenceByIdentificationVariable(String identificationVariable) {
		for ( SqmFromElementSpace fromElementSpace : fromClause.getFromElementSpaces() ) {
			if ( fromElementSpace.getRoot().getIdentificationVariable().equals( identificationVariable ) ) {
				return fromElementSpace.getRoot().getNavigableReference();
			}

			for ( SqmJoin joinedFromElement : fromElementSpace.getJoins() ) {
				if ( joinedFromElement.getIdentificationVariable().equals( identificationVariable ) ) {
					return joinedFromElement.getNavigableReference();
				}
			}
		}

		// otherwise there is none
		return null;
	}

	@Override
	public SqmNavigableReference findNavigableReferenceExposingAttribute(String attributeName) {
		for ( SqmFromElementSpace fromElementSpace : fromClause.getFromElementSpaces() ) {
			if ( exposesAttribute( fromElementSpace.getRoot(), attributeName ) ) {
				return fromElementSpace.getRoot().getNavigableReference();
			}

			for ( SqmJoin joinedFromElement : fromElementSpace.getJoins() ) {
				if ( exposesAttribute( joinedFromElement, attributeName ) ) {
					return joinedFromElement.getNavigableReference();
				}
			}
		}

		// otherwise there is none
		return null;
	}

	private boolean exposesAttribute(SqmFrom sqmFrom, String attributeName) {
		final Navigable navigable = sqmFrom.getNavigableReference().getReferencedNavigable();
		return NavigableContainer.class.isInstance( navigable )
				&& ( (NavigableContainer) navigable ).findNavigable( attributeName ) != null;
	}

	@Override
	public FromElementLocator getFromElementLocator() {
		return this;
	}

	@Override
	public FromElementBuilder getFromElementBuilder() {
		throw new SemanticException( "order-by clause cannot define implicit joins" );
	}

	@Override
	public ParsingContext getParsingContext() {
		return parsingContext;
	}
}
