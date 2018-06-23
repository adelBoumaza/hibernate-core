/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.sql.ast.tree.spi.expression.ParameterSpec;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;

/**
 * @author Steve Ebersole
 */
public class JdbcSelectImpl implements JdbcSelect {
	private final String sql;
	private final List<ParameterSpec> parameterSpecs;
	private final ResultSetMappingDescriptor resultSetMapping;
	private final Set<String> affectedTableNames;

	public JdbcSelectImpl(
			String sql,
			List<ParameterSpec> parameterSpecs,
			ResultSetMappingDescriptor resultSetMapping,
			Set<String> affectedTableNames) {
		this.sql = sql;
		this.parameterSpecs = parameterSpecs;
		this.resultSetMapping = resultSetMapping;
		this.affectedTableNames = affectedTableNames;
	}

	@Override
	public String getSql() {
		return sql;
	}

	@Override
	public List<ParameterSpec> getJdbcParameters() {
		return parameterSpecs;
	}

	@Override
	public ResultSetMappingDescriptor getResultSetMapping() {
		return resultSetMapping;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}
}
