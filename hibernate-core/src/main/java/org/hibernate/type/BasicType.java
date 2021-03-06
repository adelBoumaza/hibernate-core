/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Marker interface for basic types.
 *
 * @author Steve Ebersole
 */
public interface BasicType<T> extends Type, BasicDomainType<T>, MappingType, BasicValuedMapping, JdbcMapping {
	/**
	 * Get the names under which this type should be registered in the type registry.
	 *
	 * @return The keys under which to register this type.
	 */
	String[] getRegistrationKeys();

	@Override
	default MappingType getMappedTypeDescriptor() {
		return this;
	}

	@Override
	default JavaTypeDescriptor getJavaTypeDescriptor() {
		return getMappedJavaTypeDescriptor();
	}

	@Override
	default JdbcMapping getJdbcMapping() {
		return this;
	}

	@Override
	default JavaTypeDescriptor getMappedJavaTypeDescriptor() {
		return getJavaTypeDescriptor();
	}

	@Override
	default ValueExtractor getJdbcValueExtractor() {
		//noinspection unchecked
		return getSqlTypeDescriptor().getExtractor( getMappedJavaTypeDescriptor() );
	}

	@Override
	default ValueBinder getJdbcValueBinder() {
		//noinspection unchecked
		return getSqlTypeDescriptor().getBinder( getMappedJavaTypeDescriptor() );
	}

	@Override
	default void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( getJdbcMapping() );
	}

	@Override
	default Object disassemble(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	default void visitDisassembledJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( value, getJdbcMapping() );
	}

	@Override
	default void visitJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( value, getJdbcMapping() );
	}
}
