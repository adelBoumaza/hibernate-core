/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.sql;

import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * @author Steve Ebersole
 */
public class BaseSqmSqlTest extends BaseSqmUnitTest {
	protected JdbcSelect buildJdbcSelect(
			String hql,
			ExecutionContext executionContext) {

		final SqmSelectStatement sqm = interpretSelect( hql );

		final SqmSelectToSqlAstConverter sqmConveter = new SqmSelectToSqlAstConverter(
				executionContext.getQueryOptions(),
				executionContext.getSession().getLoadQueryInfluencers(),
				executionContext.getCallback(),
				this
		);

		final SqlAstSelectDescriptor interpretation = sqmConveter.interpret( sqm );

		return SqlAstSelectToJdbcSelectConverter.interpret(
				interpretation,
				executionContext.getSession().getSessionFactory()
		);
	}

	@Override
	public MetamodelImplementor getDomainModel() {
		return sessionFactory().getMetamodel();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return sessionFactory().getServiceRegistry();
	}

	@Override
	public Integer getMaximumFetchDepth() {
		return sessionFactory().getMaximumFetchDepth();
	}
}
