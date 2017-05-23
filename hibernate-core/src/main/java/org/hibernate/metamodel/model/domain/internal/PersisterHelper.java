/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.domain.spi.CollectionElement.ElementClassification;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata.CollectionClassification;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.Disposition;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.SingularAttributeClassification;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.metamodel.model.relational.spi.UnionSubclassTable;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelNodeFactory;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.query.sqm.tree.SqmPropertyPath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.internal.EntityTypeImpl;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.EntityType;
import org.hibernate.type.Type;

/**
 * For now mainly a helper for reflection into stuff not exposed on the entity/collection persister
 * contracts
 *
 * @author Steve Ebersole
 */
public class PersisterHelper {



	public static <O,N> PersistentAttribute<O,N> buildAttribute(
			RuntimeModelCreationContext creationContext,
			ManagedTypeImplementor<O> container,
			PersistentAttributeMapping attributeMapping) {
		if ( attributeMapping.getValueMapping() instanceof Collection ) {
			return buildPluralAttribute(
					creationContext,
					container,
					attributeMapping
			);
		}
		else {
			return buildSingularAttribute(
					creationContext,
					container,
					attributeMapping
			);
		}
	}

	@SuppressWarnings("unchecked")
	public static <O,N> AbstractPersistentAttribute<O,N> buildSingularAttribute(
			RuntimeModelCreationContext creationContext,
			ManagedTypeImplementor<O> source,
			PersistentAttributeMapping attributeMapping) {
		if ( attributeMapping.getValueMapping() instanceof Any ) {
			throw new NotYetImplementedException();
		}
		else if ( attributeMapping.getValueMapping() instanceof EmbeddedValueMapping ) {
			return new SingularPersistentAttributeEmbedded<>(
					source,
					attributeMapping.getName(),
					resolvePropertyAccess( source, attributeMapping, creationContext ),
					Disposition.NORMAL,
					creationContext.getPersisterFactory().createEmbeddablePersister(
							(Component) attributeMapping.getValueMapping(),
							source,
							attributeMapping.getName(),
							creationContext
					)
			);
		}
		else if ( attributeMapping.getValueMapping() instanceof ToOne ) {
			final ToOne toOne = (ToOne) attributeMapping.getValueMapping();

			if ( attributeMapping.getValueMapping() instanceof OneToOne ) {
				// the Classification here should be ONE_TO_ONE which could represent either a real PK one-to-one
				//		or a unique-FK one-to-one (logical).  If this is a real one-to-one then we should have
				//		no columns passed here and should instead use the LHS (source) PK column(s)
				assert columns == null || columns.size() == 0;
				columns = ( (EntityTypeImplementor) source ).getHierarchy().getIdentifierDescriptor().getColumns();
			}
			assert columns != null && columns.size() > 0;

			return new SingularPersistentAttributeEntity(
					source,
					attributeMapping.getName(),
					resolvePropertyAccess( creationContext, attributeMapping ),
					attributeMapping.getValueMapping() instanceof OneToOne || ( (ManyToOne) attributeMapping.getValueMapping() ).isLogicalOneToOne()
							? SingularAttributeClassification.ONE_TO_ONE
							: SingularAttributeClassification.MANY_TO_ONE,
					makeEntityType( creationContext, toOne ),
					Disposition.NORMAL,
					creationContext.getTypeConfiguration().findEntityPersister( toOne.getReferencedEntityName() ),
					columns
			);
		}
		else {
			assert attributeMapping.getValueMapping() instanceof SimpleValue;

			final SimpleValue simpleValue = (SimpleValue) attributeMapping.getValueMapping();

			final AttributeConverterDefinition attributeConverterInfo = simpleValue.getAttributeConverterDescriptor();

			return new SingularPersistentAttributeBasic<>(
					source,
					attributeMapping.getName(),
					resolvePropertyAccess( creationContext, attributeMapping ),
					resolveBasicType( creationContext, simpleValue ),
					Disposition.NORMAL,
					attributeConverterInfo,
					columns
			);

		}
	}















	public static org.hibernate.loader.PropertyPath convert(SqmPropertyPath propertyPath) {
		if ( propertyPath.getParent() == null ) {
			return new org.hibernate.loader.PropertyPath( null, propertyPath.getLocalPath() );
		}
		org.hibernate.loader.PropertyPath parent = convert( propertyPath.getParent() );
		return parent.append( propertyPath.getLocalPath() );
	}






	private final Method subclassTableSpanMethod;
	private final Method subclassPropertyTableNumberMethod;
	private final Method subclassPropertyColumnsMethod;
	private final Method subclassPropertyFormulasMethod;

	/**
	 * Singleton access
	 */
	public static final PersisterHelper INSTANCE = new PersisterHelper();

	private PersisterHelper() {
		try {
			subclassTableSpanMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassTableSpan" );
			subclassTableSpanMethod.setAccessible( true );

			subclassPropertyTableNumberMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyTableNumber", int.class );
			subclassPropertyTableNumberMethod.setAccessible( true );

			subclassPropertyColumnsMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyColumnReaderClosure" );
			subclassPropertyColumnsMethod.setAccessible( true );

			subclassPropertyFormulasMethod = AbstractEntityPersister.class.getDeclaredMethod( "getSubclassPropertyFormulaTemplateClosure" );
			subclassPropertyFormulasMethod.setAccessible( true );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to initialize access to AbstractEntityPersister#getSubclassTableSpan", e );
		}
	}

	public int extractSubclassTableCount(EntityTypeImplementor persister) {
		try {
			return (Integer) subclassTableSpanMethod.invoke( persister );
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassTableSpan [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassTableSpan [" + persister.toString() + "]",
					e
			);
		}
	}

	public int getSubclassPropertyTableNumber(EntityTypeImplementor persister, int subclassPropertyNumber) {
		try {
			return (Integer) subclassPropertyTableNumberMethod.invoke( persister, subclassPropertyNumber );
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public Table getPropertyTable(EntityTypeImplementor persister, String attributeName, Table[] tables) {
		final String tableName = ( (OuterJoinLoadable) persister ).getPropertyTableName( attributeName );
		for ( Table table : tables ) {
			if ( table instanceof UnionSubclassTable ) {
				if ( ( (UnionSubclassTable) table ).includes( tableName ) ) {
					return table;
				}
			}
			if ( table.getTableExpression().equals( tableName ) ) {
				return table;
			}
		}
		throw new HibernateException(
				"Could not locate Table for attribute [" + persister.getEntityName() + ".'" + attributeName + "]"
		);
	}

	public String[] getSubclassPropertyColumnExpressions(EntityTypeImplementor persister, int subclassPropertyNumber) {
		try {
			final String[][] columnExpressions = (String[][]) subclassPropertyColumnsMethod.invoke( persister );
			return columnExpressions[subclassPropertyNumber];
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public String[] getSubclassPropertyFormulaExpressions(EntityTypeImplementor persister, int subclassPropertyNumber) {
		try {
			final String[][] columnExpressions = (String[][]) subclassPropertyFormulasMethod.invoke( persister );
			return columnExpressions[subclassPropertyNumber];
		}
		catch (InvocationTargetException e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e.getTargetException()
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to access AbstractEntityPersister#getSubclassPropertyTableNumber [" + persister.toString() + "]",
					e
			);
		}
	}

	public static List<Column> makeValues(
			SessionFactoryImplementor factory,
			Type type,
			String[] columns,
			String[] formulas,
			Table table) {
		assert formulas == null || columns.length == formulas.length;

		final List<Column> values = new ArrayList<>();

		for ( int i = 0; i < columns.length; i++ ) {
			final int jdbcType = type.sqlTypes()[i];

			if ( columns[i] != null ) {
				values.add( table.makeColumn( columns[i], jdbcType ) );
			}
			else {
				if ( formulas == null ) {
					throw new IllegalStateException( "Column name was null and no formula information was supplied" );
				}
				values.add( table.makeFormula( formulas[i], jdbcType ) );
			}
		}

		return values;
	}

	public PersistentAttribute buildAttribute(
			RuntimeModelCreationContext creationContext,
			ManagedTypeImplementor source,
			PersistentAttributeMapping attributeMapping,
			List<Column> columns) {
		if ( attributeMapping.getValueMapping() instanceof Collection ) {
			assert columns == null || columns.isEmpty();

			return buildPluralAttribute(
					creationContext,
					source,
					attributeMapping
			);
		}
		else {
			return buildSingularAttribute(
					creationContext,
					source,
					attributeMapping,
					columns
			);
		}
	}

	public AbstractPersistentAttribute buildSingularAttribute(
			RuntimeModelCreationContext creationContext,
			ManagedTypeImplementor source,
			PersistentAttributeMapping attributeMapping,
			List<Column> columns) {
		if ( attributeMapping.getValueMapping() instanceof Any ) {
			throw new NotYetImplementedException();
		}
		else if ( attributeMapping.getValueMapping() instanceof Component ) {
			return new SingularPersistentAttributeEmbedded(
					source,
					attributeMapping.getName(),
					resolvePropertyAccess( source, attributeMapping, creationContext ),
					Disposition.NORMAL,
					creationContext.getPersisterFactory().createEmbeddablePersister(
							(Component) attributeMapping.getValueMapping(),
							source,
							attributeMapping.getName(),
							creationContext
					)
			);
		}
		else if ( attributeMapping.getValueMapping() instanceof ToOne ) {
			final ToOne toOne = (ToOne) attributeMapping.getValueMapping();

			if ( attributeMapping.getValueMapping() instanceof OneToOne ) {
				// the Classification here should be ONE_TO_ONE which could represent either a real PK one-to-one
				//		or a unique-FK one-to-one (logical).  If this is a real one-to-one then we should have
				//		no columns passed here and should instead use the LHS (source) PK column(s)
				assert columns == null || columns.size() == 0;
				columns = ( (EntityTypeImplementor) source ).getHierarchy().getIdentifierDescriptor().getColumns();
			}
			assert columns != null && columns.size() > 0;

			return new SingularPersistentAttributeEntity(
					source,
					attributeMapping.getName(),
					resolvePropertyAccess( creationContext, attributeMapping ),
					attributeMapping.getValueMapping() instanceof OneToOne || ( (ManyToOne) attributeMapping.getValueMapping() ).isLogicalOneToOne()
							? SingularAttributeClassification.ONE_TO_ONE
							: SingularAttributeClassification.MANY_TO_ONE,
					makeEntityType( creationContext, toOne ),
					Disposition.NORMAL,
					creationContext.getTypeConfiguration().findEntityPersister( toOne.getReferencedEntityName() ),
					columns
			);
		}
		else {
			assert attributeMapping.getValueMapping() instanceof SimpleValue;

			final SimpleValue simpleValue = (SimpleValue) attributeMapping.getValueMapping();

			final AttributeConverterDefinition attributeConverterInfo = simpleValue.getAttributeConverterDescriptor();

			return new SingularPersistentAttributeBasic<>(
					source,
					attributeMapping.getName(),
					resolvePropertyAccess( creationContext, attributeMapping ),
					resolveBasicType( creationContext, simpleValue ),
					Disposition.NORMAL,
					attributeConverterInfo,
					columns
			);

		}
	}

	@SuppressWarnings("unchecked")
	private <J> BasicType<J> resolveBasicType(RuntimeModelCreationContext creationContext, SimpleValue simpleValue) {
		if ( simpleValue.getCurrentType() != null ) {
			return (BasicType<J>) simpleValue.getCurrentType();
		}

		return creationContext.getTypeConfiguration().getBasicTypeRegistry().resolveBasicType(
				simpleValue.getBasicTypeParameters(),
				simpleValue.makeJdbcRecommendedSqlTypeMappingContext( creationContext.getTypeConfiguration() )
		);
	}

	private EntityType makeEntityType(RuntimeModelCreationContext creationContext, ToOne toOne) {
		final String referencedEntityName = toOne.getReferencedEntityName();
		final EntityTypeImplementor<?> entityPersister = creationContext.getTypeConfiguration().findEntityPersister( referencedEntityName );

		return new EntityTypeImpl(
				null,
				entityPersister.getJavaTypeDescriptor(),
				creationContext.getTypeConfiguration()
		);
	}

	private PropertyAccess resolvePropertyAccess(RuntimeModelCreationContext persisterCreationContext, PersistentAttributeMapping attributeMapping) {
		final PropertyAccessStrategyResolver accessStrategyResolver = persisterCreationContext.getSessionFactory()
				.getServiceRegistry()
				.getService( PropertyAccessStrategyResolver.class );

		String accessName = attributeMapping.getPropertyAccessorName();
		if ( accessName == null ) {
			if ( clazz == null || java.util.Map.class.equals( clazz ) ) {
				accessName = "map";
			}
			else {
				accessName = "property";
			}
		}

		final EntityMode entityMode = clazz == null || java.util.Map.class.equals( clazz )
				? EntityMode.MAP
				: EntityMode.POJO;

		return resolveServiceRegistry().getService( PropertyAccessStrategyResolver.class ).resolvePropertyAccessStrategy(
				clazz,
				accessName,
				entityMode
		);

//		final PropertyAccessStrategy accessStrategy = accessStrategyResolver.resolvePropertyAccessStrategy(
//
//		);
		final PropertyAccessStrategy strategy = attributeMapping.getPropertyAccessStrategy( attributeMapping.getPersistentClass().getMappedClass() );
		return strategy.buildPropertyAccess( attributeMapping.getPersistentClass().getMappedClass(), attributeMapping.getName() );
	}

	private static String extractEmbeddableName(Type attributeType) {
		// todo : fixme
		return attributeType.getName();
	}

	public PersistentAttribute buildPluralAttribute(
			RuntimeModelCreationContext creationContext,
			ManagedTypeImplementor source,
			PersistentAttributeMapping attributeMapping) {
		final RuntimeModelNodeFactory persisterFactory = creationContext.getSessionFactory().getServiceRegistry().getService( RuntimeModelNodeFactory.class );

			// todo : resolve cache access
		final CollectionRegionAccessStrategy cachingAccess = null;

		// need PersisterCreationContext - we should always have access to that when building persisters, through finalized initialization
		final PersistentCollectionMetadata collectionPersister = persisterFactory.createCollectionPersister(
				(Collection) attributeMapping.getValueMapping(),
				source,
				attributeMapping.getName(),
				cachingAccess,
				creationContext
		);
		creationContext.registerCollectionPersister( collectionPersister );
		return collectionPersister;
	}

	public static PropertyAccess resolvePropertyAccess(
			ManagedTypeImplementor declarer,
			PersistentAttributeMapping attributeMapping,
			RuntimeModelCreationContext persisterCreationContext) {
		final PropertyAccessStrategyResolver accessStrategyResolver = persisterCreationContext.getSessionFactory()
				.getServiceRegistry()
				.getService( PropertyAccessStrategyResolver.class );

		String accessName = attributeMapping.getPropertyAccessorName();
		if ( accessName == null ) {
			if ( clazz == null || java.util.Map.class.equals( clazz ) ) {
				accessName = "map";
			}
			else {
				accessName = "property";
			}
		}

		final EntityMode entityMode = clazz == null || java.util.Map.class.equals( clazz )
				? EntityMode.MAP
				: EntityMode.POJO;

		return resolveServiceRegistry().getService( PropertyAccessStrategyResolver.class ).resolvePropertyAccessStrategy(
				clazz,
				accessName,
				entityMode
		);

		final PropertyAccessStrategy accessStrategy = accessStrategyResolver.resolvePropertyAccessStrategy(

		);
		final PropertyAccessStrategy strategy = attributeMapping.getPropertyAccessStrategy( attributeMapping.getPersistentClass().getMappedClass() );
		return strategy.buildPropertyAccess( attributeMapping.getPersistentClass().getMappedClass(), attributeMapping.getName() );
	}

	public static List<Column> makeValues(
			SessionFactoryImplementor sessionFactory,
			Type type,
			Iterator<Selectable> columnIterator,
			Table separateCollectionTable) {
		return null;
	}

	public interface CollectionMetadata {
		CollectionClassification getCollectionClassification();
		ElementClassification getElementClassification();

		Type getForeignKeyType();
		BasicType getCollectionIdType();
		Type getElementType();
		Type getIndexType();
	}

	public static class CollectionMetadataImpl implements CollectionMetadata {
		private final CollectionClassification collectionClassification;
		private final ElementClassification elementClassification;
		private final Type foreignKeyType;
		private final BasicType collectionIdType;
		private final Type elementType;
		private final Type indexType;

		public CollectionMetadataImpl(
				CollectionClassification collectionClassification,
				ElementClassification elementClassification,
				Type foreignKeyType,
				BasicType collectionIdType,
				Type elementType,
				Type indexType) {
			this.collectionClassification = collectionClassification;
			this.elementClassification = elementClassification;
			this.foreignKeyType = foreignKeyType;
			this.collectionIdType = collectionIdType;
			this.elementType = elementType;
			this.indexType = indexType;
		}

		@Override
		public CollectionClassification getCollectionClassification() {
			return collectionClassification;
		}

		@Override
		public ElementClassification getElementClassification() {
			return elementClassification;
		}

		@Override
		public Type getForeignKeyType() {
			return foreignKeyType;
		}

		@Override
		public BasicType getCollectionIdType() {
			return collectionIdType;
		}

		@Override
		public Type getElementType() {
			return elementType;
		}

		@Override
		public Type getIndexType() {
			return indexType;
		}
	}

	public static CollectionMetadata interpretCollectionMetadata(SessionFactoryImplementor factory, Property property) {
		final Collection collectionBinding = (Collection) property.getValue();

		return new CollectionMetadataImpl(
				interpretCollectionClassification( collectionBinding ),
				interpretElementClassification( collectionBinding ),
				collectionBinding.getKey().getType(),
				collectionBinding instanceof IdentifierCollection
						? (BasicType) ( (IdentifierCollection) collectionBinding ).getIdentifier().getType()
						: null,
				collectionBinding.getElement().getType(),
				( (IndexedCollection) collectionBinding ).getIndex().getType()
		);
	}

	public static CollectionClassification interpretCollectionClassification(Collection collectionBinding) {
		if ( collectionBinding instanceof Bag || collectionBinding instanceof IdentifierBag ) {
			return CollectionClassification.BAG;
		}
		else if ( collectionBinding instanceof org.hibernate.mapping.List ) {
			return CollectionClassification.LIST;
		}
		else if ( collectionBinding instanceof org.hibernate.mapping.Set ) {
			return CollectionClassification.SET;
		}
		else if ( collectionBinding instanceof org.hibernate.mapping.Map ) {
			return CollectionClassification.MAP;
		}
		else {
			final Class javaType = collectionBinding.getElement().getType().getJavaType();
			if ( Set.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.SET;
			}
			else if ( Map.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.MAP;
			}
			else if ( List.class.isAssignableFrom( javaType ) ) {
				return CollectionClassification.LIST;
			}

			return CollectionClassification.BAG;
		}
	}

	private static ElementClassification interpretElementClassification(Collection collectionBinding) {
		if ( collectionBinding.getElement() instanceof Any ) {
			return ElementClassification.ANY;
		}
		else if ( collectionBinding.getElement() instanceof Component ) {
			return ElementClassification.EMBEDDABLE;
		}
		else if ( collectionBinding.getElement() instanceof OneToMany ) {
			return ElementClassification.ONE_TO_MANY;
		}
		else if ( collectionBinding.getElement() instanceof ManyToOne ) {
			return ElementClassification.MANY_TO_MANY;
		}
		else {
			return ElementClassification.BASIC;
		}
	}


	public static SingularAttributeClassification interpretIdentifierClassification(Type ormIdType) {
		return ormIdType instanceof EmbeddedType
				? SingularAttributeClassification.EMBEDDED
				: SingularAttributeClassification.BASIC;
	}
}