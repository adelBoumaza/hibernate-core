/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.type.StandardBasicTypes.StandardBasicType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.StandardBasicValueMapper;

import org.jboss.logging.Logger;

/**
 * Registry for BasicType instances.  Lookup is primarily done by Java type
 * (Class), but can be adjusted by JDBC type-code and/or MutabilityPlan.
 * <p/>
 * It is important to understand that all basic types have a Java type.  We
 * do not support alternate EntityModes for basic-types.
 * <p/>
 * The ability
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
@Incubating
public class BasicTypeRegistry {
	private static final Logger log = Logger.getLogger( BasicTypeRegistry.class );

	private final TypeConfiguration typeConfiguration;

	// HQL
	// ... where cast(c.name as uuid) ...

	private final Map<String,BasicType> typesByNameRegistry = new ConcurrentHashMap<>();

	private Map<SqlTypeDescriptor,Map<BasicJavaDescriptor,BasicType>> registryValues = new ConcurrentHashMap<>();

	private final SqlTypeDescriptorIndicators baseSqlTypeDescriptorIndicators;

	public BasicTypeRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		this.baseSqlTypeDescriptorIndicators = new SqlTypeDescriptorIndicators() {
			@Override
			public boolean isNationalized() {
				return false;
			}

			@Override
			public boolean isLob() {
				return false;
			}

			@Override
			public TypeConfiguration getTypeConfiguration() {
				return typeConfiguration;
			}
		};
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public SqlTypeDescriptorIndicators getBaseSqlTypeDescriptorIndicators() {
		return baseSqlTypeDescriptorIndicators;
	}


	/**
	 * Find an existing BasicType registration for the given JavaTypeDescriptor and
	 * SqlTypeDescriptor combo or create (and register) one.
	 */
	public BasicType<?> resolve(BasicJavaDescriptor<?> jtdToUse, SqlTypeDescriptor stdToUse) {
		final Map<BasicJavaDescriptor, BasicType> mappingsForStdToUse = registryValues.computeIfAbsent(
				stdToUse,
				sqlTypeDescriptor -> new ConcurrentHashMap<>()
		);

		return mappingsForStdToUse.computeIfAbsent(
				jtdToUse,
				javaDescriptor -> new BasicTypeImpl( javaDescriptor, stdToUse )
		);
	}

	// todo (6.0) : we want to consolidate many of these methods below based on typesByNameRegistry vs. registryValues lookups


	public BasicType getBasicTypeByName(String key) {
		return typesByNameRegistry.get( key );
	}

	/**
	 * Returns the default BasicType for the given Java type
	 *
	 * @param javaType The Java type (Class) for which we want the BasicType.
	 *
	 * @return The linked BasicType.  May return {@code null}
	 */
	@SuppressWarnings({"unchecked", "RedundantClassCall"})
	public <T> BasicType<T> getBasicType(Class<T> javaType) {
		final JavaTypeDescriptor<T> jtd = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( javaType );

		if ( !BasicJavaDescriptor.class.isInstance( jtd ) ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Previously registered non-basic JavaTypeDescriptor [%s] found for class [%s]; cannot create BasicType",
							jtd,
							javaType.getName()
					)
			);
		}

		final SqlTypeDescriptor recommendedStd = jtd.getJdbcRecommendedSqlType( getBaseSqlTypeDescriptorIndicators() );

		return (BasicType) resolve( (BasicJavaDescriptor) jtd, recommendedStd );
	}



	// BasicValueBinder really ought to just track the explicit selections
//
//	@SuppressWarnings("unchecked")
//	public <T> BasicType<T> resolveBasicType(
//			BasicTypeParameters<T> parameters,
//			SqlTypeDescriptorIndicators jdbcTypeResolutionContext) {
//		if ( parameters == null ) {
//			throw new IllegalArgumentException( "BasicTypeParameters must not be null" );
//		}
//
//		// IMPL NOTE : resolving a BasicType follows very different algorithms based on what
//		// specific information is available (non-null) from the BasicTypeParameters.  To help
//		// facilitate that, we try to break this down into a number of sub-methods for some
//		// high-level differences
//
//		// todo implement this.  But the intention has changed.  Here we simply have another potential
//		//		hint as to the SqlTypeDescriptor to use.
//
////		if ( parameters.getAttributeConverterDefinition() != null ) {
////			return resolveConvertedBasicType( parameters, jdbcTypeResolutionContext );
////		}
//
//
//		BasicJavaDescriptor<T> javaTypeDescriptor = parameters.getJavaTypeDescriptor();
//		SqlTypeDescriptor sqlTypeDescriptor = parameters.getSqlTypeDescriptor();
//
//		// todo (6.0) - should no longer handle "attribute converter here.
//		// 		see note on `BasicTypeParameters#getAttributeConverterDescriptor`
//		if ( parameters.getAttributeConverterDescriptor() != null ) {
//			// we have an attribute converter, use that to either:
//			//		1) validate the BasicJavaDescriptor/SqlTypeDescriptor defined on parameters
//			//		2) use the converter param types as hints to the missing BasicJavaDescriptor/SqlTypeDescriptor
//
//			final Class<?> converterDomainJavaType = parameters.getAttributeConverterDescriptor()
//					.getDomainValueResolvedType()
//					.getErasedType();
//
//			final JavaTypeDescriptor<?> converterDomainJavaDescriptor = typeConfiguration.getJavaTypeDescriptorRegistry()
//					.getDescriptor( converterDomainJavaType );
//
//			if ( javaTypeDescriptor == null ) {
//				javaTypeDescriptor = (BasicJavaDescriptor<T> ) converterDomainJavaDescriptor;
//			}
//			else {
//				if ( !javaTypeDescriptor.equals( converterDomainJavaDescriptor ) ) {
//					throw new HibernateException(
//							"JavaTypeDescriptors did not match between BasicTypeParameters#getJavaTypeDescriptor and " +
//									"BasicTypeParameters#getAttributeConverterDefinition#getDomainType"
//					);
//				}
//			}
//
//			final Class<?> converterRelationalJavaType = parameters.getAttributeConverterDescriptor()
//					.getRelationalValueResolvedType()
//					.getErasedType();
//			final JavaTypeDescriptor<?> converterRelationalJavaDescriptor = typeConfiguration.getJavaTypeDescriptorRegistry()
//					.getDescriptor( converterRelationalJavaType );
//
//			final SqlTypeDescriptor resolvedConverterHintedSqlTypeDescriptor = converterRelationalJavaDescriptor
//					.getJdbcRecommendedSqlType( jdbcTypeResolutionContext );
//
//			if ( sqlTypeDescriptor == null ) {
//				sqlTypeDescriptor = resolvedConverterHintedSqlTypeDescriptor;
//			}
//			else {
//				if ( !sqlTypeDescriptor.equals( resolvedConverterHintedSqlTypeDescriptor ) ) {
//					throw new HibernateException(
//							"SqlTypeDescriptors did not match between BasicTypeParameters#getSqlTypeDescriptor and " +
//									"BasicTypeParameters#getAttributeConverterDefinition#getJdbcType"
//					);
//				}
//			}
//		}
//		else if ( parameters.getTemporalPrecision() != null ) {
//			// we have a specified temporal precision, which is another hint as to types...
//			if ( javaTypeDescriptor == null ) {
//				javaTypeDescriptor = determineJavaDescriptorForTemporalPrecision( parameters.getTemporalPrecision() );
//			}
//			// else verify that javaTypeDescriptor is "compatible" with parameters.getTemporalPrecision() ?
//
//			if ( sqlTypeDescriptor == null ) {
//				sqlTypeDescriptor = javaTypeDescriptor.getJdbcRecommendedSqlType( jdbcTypeResolutionContext );
//			}
//		}
//
//		if ( javaTypeDescriptor == null ) {
//			if ( sqlTypeDescriptor == null ) {
//				throw new IllegalArgumentException(
//						"BasicTypeParameters must define either a JavaTypeDescriptor or a SqlTypeDescriptor, " +
//								"or provide AttributeConverter or JPA temporal precision (javax.persistence.TemporalType)"
//				);
//			}
//			javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( jdbcTypeResolutionContext.getTypeConfiguration() );
//		}
//
//		if ( sqlTypeDescriptor == null ) {
//			sqlTypeDescriptor = javaTypeDescriptor.getJdbcRecommendedSqlType( jdbcTypeResolutionContext );
//		}
//
//		return createBasicType( javaTypeDescriptor, sqlTypeDescriptor );
//	}

//	private <T> BasicType<T> createBasicType(
//			BasicJavaDescriptor<T> javaTypeDescriptor,
//			SqlTypeDescriptor sqlTypeDescriptor) {
//		final BasicTypeImpl<T> basicType = new BasicTypeImpl<>( javaTypeDescriptor, sqlTypeDescriptor );
//		registry.put( javaTypeDescriptor.getJavaType().getName(), basicType );
//		return basicType;
//	}
//
//	private <T> TemporalJavaDescriptor<T> determineJavaDescriptorForTemporalPrecision(javax.persistence.TemporalType temporalPrecision) {
//		switch ( temporalPrecision ) {
//			case TIMESTAMP: {
//				return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( java.sql.Timestamp.class );
//			}
//			case DATE: {
//				return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( java.sql.Date.class );
//			}
//			case TIME: {
//				return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( java.sql.Time.class );
//			}
//			default: {
//				throw new HibernateException( "Unrecognized JPA temporal precision : " + temporalPrecision );
//			}
//		}
//	}

	public void register(BasicType type) {
		typesByNameRegistry.put( type.getJavaTypeDescriptor().getJavaType().getName(), type );
	}

	public void register(BasicType type, String key) {
		typesByNameRegistry.put( key, type );
	}

	public void register(BasicType type, String... keys) {
		for ( String key : keys ) {
			typesByNameRegistry.put( key, type );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BasicTypeRegistry as a `ValueMapperRegistry`

	private final Map<String,BasicValueMapper> mapperMap = new ConcurrentHashMap<>();

	public void register(BasicValueMapper valueMapper, String... keys) {
		for ( String key : keys ) {
			final BasicValueMapper old = mapperMap.put( key, valueMapper );
			if ( old != null && old != valueMapper ) {
				log.debugf(
						"BasicValueMapper registration [%s] overrode previous : %s -> %s",
						key,
						old,
						valueMapper
				);
			}
		}
	}
}
