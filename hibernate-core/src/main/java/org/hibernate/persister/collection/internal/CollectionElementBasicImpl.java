/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.persister.collection.spi.AbstractCollectionElement;
import org.hibernate.persister.collection.spi.CollectionElementBasic;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.model.relational.spi.Column;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.ast.produce.result.internal.QueryResultScalarImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionElementBasicImpl<J>
		extends AbstractCollectionElement<J>
		implements CollectionElementBasic<J> {
	private static final Logger log = Logger.getLogger( CollectionElementBasicImpl.class );

	private final Column column;
	private final BasicType<J> basicType;
	private final AttributeConverterDefinition attributeConverter;

	public CollectionElementBasicImpl(
			CollectionPersister persister,
			Collection mappingBinding,
			PersisterCreationContext creationContext) {
		super( persister );

		final SimpleValue simpleElementValueMapping = (SimpleValue) mappingBinding.getElement();

		this.column = creationContext.resolveColumn( simpleElementValueMapping );

		// todo (6.0) : resolve SimpleValue -> BasicType
		this.basicType = null;

		this.attributeConverter = simpleElementValueMapping.getAttributeConverterDescriptor();

		log.debugf(
				"AttributeConverter [%s] being injected for elements of the '%s' collection; was : %s",
				attributeConverter.getAttributeConverter(),
				getContainer().getRole(),
				this.attributeConverter
		);
	}

	@Override
	public AttributeConverterDefinition getAttributeConverter() {
		return attributeConverter;
	}

	@Override
	public ElementClassification getClassification() {
		return ElementClassification.BASIC;
	}

	@Override
	public BasicType<J> getBasicType() {
		return basicType;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionElementBasic( this );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultScalarImpl(
				selectedExpression,
				sqlSelectionResolver.resolveSqlSelection(
						columnReferenceSource.resolveColumnReference( getBoundColumn() )
				),
				resultVariable,
				getBasicType()
		);
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( getBoundColumn() );
	}
}
