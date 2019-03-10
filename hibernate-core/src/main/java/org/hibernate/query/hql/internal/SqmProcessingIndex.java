/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.QueryLogger;
import org.hibernate.query.sqm.AliasCollisionException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.SqmPathRegistry;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmSelection;

/**
 * Container for indexing needed while building an SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmProcessingIndex implements SqmPathRegistry {
	private final SqmCreationProcessingState associatedProcessingState;

	private final Map<NavigablePath, SqmPath> sqmPathByPath = new HashMap<>();

	private final Map<NavigablePath, SqmFrom> sqmFromByPath = new HashMap<>();
	private final Map<String, SqmFrom> sqmFromByAlias = new HashMap<>();

	private final Map<String, SqmSelection> sqmSelectionsByAlias = new HashMap<>();

	public SqmProcessingIndex(SqmCreationProcessingState associatedProcessingState) {
		this.associatedProcessingState = associatedProcessingState;
	}

	@Override
	public void register(SqmPath sqmPath) {
		// Generally we:
		//		1) add the path to the path-by-path map
		//		2) if the path is a from, we add it to the from-by-path map
		//		3) if the path is a from and defines an alias, we add it to the from-by-alias map
		//
		// Regarding part #1 (add to the path-by-path map), it is ok for a SqmFrom to replace a
		// 		non-SqmFrom.  This should equate to, e.g., an implicit join.

		final SqmPath previousPath = sqmPathByPath.put( sqmPath.getNavigablePath(), sqmPath );

		if ( previousPath instanceof SqmFrom ) {
			// this should never happen
			throw new ParsingException(
					String.format(
							Locale.ROOT,
							"Registration for path [%s] overrode previous registration: %s -> %s",
							sqmPath.getNavigablePath(),
							previousPath,
							sqmPath
					)
			);
		}

		if ( sqmPath instanceof SqmFrom ) {
			final SqmFrom sqmFrom = (SqmFrom) sqmPath;
			final SqmFrom previousFromByPath = sqmFromByPath.put( sqmPath.getNavigablePath(), sqmFrom );

			if ( previousFromByPath != null ) {
				// this should never happen
				throw new ParsingException(
						String.format(
								Locale.ROOT,
								"Registration for SqmFrom [%s] overrode previous registration: %s -> %s",
								sqmPath.getNavigablePath(),
								previousFromByPath,
								sqmFrom
						)
				);
			}

			final String alias = sqmPath.getExplicitAlias();
			if ( alias == null ) {
				return;
			}

			final SqmFrom previousFrom = sqmFromByAlias.put( alias, sqmFrom );
			if ( previousFrom != null ) {
				throw new AliasCollisionException(
						String.format(
								Locale.ENGLISH,
								"Alias [%s] used for multiple from-clause elements : %s, %s",
								alias,
								previousFrom,
								sqmPath
						)
				);
			}
		}
	}

	@Override
	public void register(NavigablePath alternatePath, SqmPath sqmPath) {
		final SqmPath previousPath = sqmPathByPath.put( alternatePath, sqmPath );

		if ( previousPath != null ) {
			// this should never happen
			throw new ParsingException(
					String.format(
							Locale.ROOT,
							"Registration for path (alt) [%s] overrode previous registration: %s -> %s",
							alternatePath,
							previousPath,
							sqmPath
					)
			);
		}

		if ( sqmPath instanceof SqmFrom ) {
			final SqmPath previousFrom = sqmFromByPath.put( alternatePath, (SqmFrom) sqmPath );

			if ( previousFrom != null ) {
				// this should never happen
				throw new ParsingException(
						String.format(
								Locale.ROOT,
								"Registration for SqmFrom (alt) [%s] overrode previous registration: %s -> %s",
								alternatePath,
								previousFrom,
								sqmPath
						)
				);
			}
		}
	}

	@Override
	public SqmPath findPath(NavigablePath path) {
		return sqmPathByPath.get( path );
	}

	@Override
	public SqmFrom findFromByPath(NavigablePath navigablePath) {
		return sqmFromByPath.get( navigablePath );
	}

	@Override
	public SqmFrom findFromByAlias(String alias) {
		final SqmFrom registered = sqmFromByAlias.get( alias );
		if ( registered != null ) {
			return registered;
		}

		if ( associatedProcessingState.getParentProcessingState() != null ) {
			return associatedProcessingState.getParentProcessingState().getPathRegistry().findFromByAlias( alias );
		}

		return null;
	}

	@Override
	public SqmFrom findFromExposing(String navigableName) {
		// todo (6.0) : atm this checks every from-element every time, the idea being to make sure there
		//  	is only one such element obviously that scales poorly across larger from-clauses.  Another
		//  	(configurable?) option would be to simply pick the first one as a perf optimization

		SqmFrom found = null;
		for ( SqmFrom fromElement : sqmFromByPath.values() ) {
			if ( definesAttribute( fromElement.getReferencedNavigable(), navigableName ) ) {
				if ( found != null ) {
					throw new IllegalStateException( "Multiple from-elements expose unqualified attribute : " + navigableName );
				}
				found = fromElement;
			}
		}

		if ( found == null ) {
			if ( associatedProcessingState.getParentProcessingState() != null ) {
				QueryLogger.QUERY_LOGGER.debugf(
						"Unable to resolve unqualified attribute [%s] in local from-clause; checking parent ",
						navigableName
				);
				found = associatedProcessingState.getParentProcessingState().getPathRegistry().findFromExposing( navigableName );
			}
		}

		return found;
	}

	@Override
	public SqmPath resolvePath(NavigablePath path, Function<NavigablePath, SqmPath> creator) {
		return sqmPathByPath.computeIfAbsent(
				path,
				navigablePath -> {
					final SqmPath sqmPath = creator.apply( navigablePath );
					register( sqmPath );
					return sqmPath;
				}
		);
	}

	private boolean definesAttribute(NavigableContainer containerType, String name) {
		return containerType.findNavigable( name ) != null;
	}

	public SqmSelection findSelectionByAlias(String alias) {
		return sqmSelectionsByAlias.get( alias );
	}

	public void registerSelection(SqmSelection selection) {
		if ( selection.getAlias() != null ) {
			checkResultVariable( selection );
			sqmSelectionsByAlias.put( selection.getAlias(), selection );
		}
	}

	private void checkResultVariable(SqmSelection selection) {
		final String alias = selection.getAlias();

		if ( sqmSelectionsByAlias.containsKey( alias ) ) {
			throw new AliasCollisionException(
					String.format(
							Locale.ENGLISH,
							"Alias [%s] is already used in same select clause",
							alias
					)
			);
		}

		final SqmFrom registeredFromElement = sqmFromByAlias.get( alias );
		if ( registeredFromElement != null ) {
			if ( !registeredFromElement.equals( selection.getSelectableNode() ) ) {
				throw new AliasCollisionException(
						String.format(
								Locale.ENGLISH,
								"Alias [%s] used in select-clause [%s] also used in from-clause [%s]",
								alias,
								selection.getSelectableNode(),
								registeredFromElement
						)
				);
			}
		}
	}
}
