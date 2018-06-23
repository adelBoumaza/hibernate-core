/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.sql.AbstractJdbcValueBinder;
import org.hibernate.sql.AbstractJdbcValueExtractor;
import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.internal.JdbcLiteralFormatterBoolean;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#BOOLEAN BOOLEAN} handling.
 *
 * @author Steve Ebersole
 */
public class BooleanSqlDescriptor extends AbstractTemplateSqlTypeDescriptor {
	public static final BooleanSqlDescriptor INSTANCE = new BooleanSqlDescriptor();

	public BooleanSqlDescriptor() {
	}

	public int getJdbcTypeCode() {
		return Types.BOOLEAN;
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Boolean.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterBoolean( javaTypeDescriptor );
	}

	@Override
	protected <X> JdbcValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor) {
		return new AbstractJdbcValueBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, ExecutionContext executionContext) throws SQLException {
				st.setBoolean( index, javaTypeDescriptor.unwrap( value, Boolean.class, executionContext.getSession() ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, ExecutionContext executionContext)
					throws SQLException {
				st.setBoolean( name, javaTypeDescriptor.unwrap( value, Boolean.class, executionContext.getSession() ) );
			}
		};
	}

	@Override
	protected <X> JdbcValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor) {
		return new AbstractJdbcValueExtractor<X>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, SqlSelection sqlSelection, JdbcValuesSourceProcessingState processingState) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getBoolean( sqlSelection.getJdbcResultSetIndex() ), processingState.getSession() );
			}

			@Override
			protected X doExtract(CallableStatement statement, SqlSelection sqlSelection, JdbcValuesSourceProcessingState processingState) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBoolean( sqlSelection.getJdbcResultSetIndex() ), processingState.getSession() );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, JdbcValuesSourceProcessingState processingState) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getBoolean( name ), processingState.getSession() );
			}
		};
	}
}
