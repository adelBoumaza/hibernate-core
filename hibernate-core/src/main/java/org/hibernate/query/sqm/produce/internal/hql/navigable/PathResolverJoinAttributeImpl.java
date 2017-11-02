/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql.navigable;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.spi.ResolutionContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;

/**
 * PathResolver implementation for resolving path references as part of a
 * SqmFromClause (join paths mainly).
 *
 * @author Steve Ebersole
 */
public class PathResolverJoinAttributeImpl extends PathResolverBasicImpl {
	private final SqmFromElementSpace fromElementSpace;
	private final SqmJoinType joinType;
	private final String alias;
	private final boolean fetched;

	public PathResolverJoinAttributeImpl(
			ResolutionContext resolutionContext,
			SqmFromElementSpace fromElementSpace,
			SqmJoinType joinType,
			String alias,
			boolean fetched) {
		super( resolutionContext );
		this.fromElementSpace = fromElementSpace;
		this.joinType = joinType;
		this.alias = alias;
		this.fetched = fetched;
	}

	@Override
	public boolean canReuseImplicitJoins() {
		return false;
	}

	@Override
	protected SqmJoinType getIntermediateJoinType() {
		return joinType;
	}

	protected boolean areIntermediateJoinsFetched() {
		return fetched;
	}

	@Override
	protected SqmNavigableReference resolveTerminalAttributeBinding(
			SqmNavigableContainerReference sourceBinding,
			String terminalName) {
		final Navigable attribute = resolveNavigable( sourceBinding, terminalName );
		return resolveTerminal( sourceBinding, attribute, null );
	}

	private SqmAttributeReference resolveTerminal(
			SqmNavigableContainerReference sourceBinding,
			Navigable navigable,
			EntityValuedExpressableType subclassIndicator) {
		final SqmAttributeReference attributeBinding = (SqmAttributeReference) context().getParsingContext()
				.findOrCreateNavigableReference(
						sourceBinding,
						navigable
				);

		if ( attributeBinding.getExportedFromElement() == null ) {
			// create the join and inject it into the binding
			attributeBinding.injectExportedFromElement(
					context().getFromElementBuilder().buildAttributeJoin(
							attributeBinding,
							alias,
							subclassIndicator,
							getIntermediateJoinType(),
							areIntermediateJoinsFetched(),
							canReuseImplicitJoins()
					)
			);
		}

		return attributeBinding;
	}

	@Override
	protected SqmNavigableReference resolveTreatedTerminal(
			ResolutionContext context,
			SqmNavigableContainerReference sourceBinding,
			String terminalName,
			EntityValuedExpressableType subclassIndicator) {
		final Navigable attribute = resolveNavigable( sourceBinding, terminalName );
		return resolveTerminal( sourceBinding, attribute, subclassIndicator );
	}

	@Override
	protected SqmNavigableReference resolveFromElementAliasAsTerminal(SqmFromExporter exporter) {
		// this can never be valid...
		throw new SemanticException( "Cannot join to aliased FromElement [" + exporter.getExportedFromElement().getIdentificationVariable() + "]" );
	}
}