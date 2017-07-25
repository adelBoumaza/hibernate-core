/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementReference implements NavigableReference {
	private final NavigableContainerReference collectionReference;

	private final PersistentCollectionDescriptor collectionMetadata;
	private final ColumnReferenceSource columnReferenceSource;
	private final NavigablePath navigablePath;

	public PluralAttributeElementReference(
			NavigableContainerReference collectionReference,
			PersistentCollectionDescriptor collectionMetadata,
			TableGroup columnReferenceSource,
			NavigablePath navigablePath) {
		this.collectionReference = collectionReference;
		this.collectionMetadata = collectionMetadata;
		this.columnReferenceSource = columnReferenceSource;
		this.navigablePath = navigablePath;
	}

	@Override
	public CollectionElement getType() {
		return getNavigable();
	}

	@Override
	public CollectionElement getSelectable() {
		return getNavigable();
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitPluralAttributeElement( this );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return collectionReference;
	}

	@Override
	public ColumnReferenceSource getContributedColumnReferenceSource() {
		return columnReferenceSource;
	}

	@Override
	public CollectionElement getNavigable() {
		return collectionMetadata.getElementDescriptor();
	}
}
