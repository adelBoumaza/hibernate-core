/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.model.relational.spi;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.naming.Identifier;

/**
 * Pluggable strategy contract for applying physical naming rules for database object names.
 *
 * NOTE: Ideally we'd pass "extra" things in here like Dialect, etc to better handle identifier
 * length constraints or auto quoting of identifiers.  However, the pre-metamodel model does not
 * necessarily know this information at the time the strategy is called.
 *
 * @author Steve Ebersole
 */
public interface PhysicalNamingStrategy {
	Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment);

	Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment);

	Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment);

	Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment);

	Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment);
}
