/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.sql.ast.produce.result.spi.ColumnReferenceResolver;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * @author Steve Ebersole
 */
public interface Selection {
	Expression getSelectedExpression();

	String getResultVariable();

	QueryResult createQueryResult(
			ColumnReferenceResolver columnReferenceResolver,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext);
}
