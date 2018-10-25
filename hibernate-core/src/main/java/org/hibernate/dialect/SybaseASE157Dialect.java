/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.JDBCException;
import org.hibernate.LockOptions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect targeting Sybase Adaptive Server Enterprise (ASE) 15.7 and higher.
 * <p/>
 *
 * @author Junyan Ren
 */
public class SybaseASE157Dialect extends SybaseASE15Dialect {

	/**
	 * Constructs a SybaseASE157Dialect
	 */
	public SybaseASE157Dialect() {
		super();
	}

	@Override
	public void initializeFunctionRegistry(SqmFunctionRegistry registry) {
		super.initializeFunctionRegistry( registry );

		registry.registerPattern( "create_locator", "create_locator(?1, ?2)", StandardSpiBasicTypes.BINARY );
		registry.registerPattern( "locator_literal", "locator_literal(?1, ?2)", StandardSpiBasicTypes.BINARY );
		registry.registerPattern( "locator_valid", "locator_valid(?1)", StandardSpiBasicTypes.BOOLEAN );
		registry.registerPattern( "return_lob", "return_lob(?1, ?2)", StandardSpiBasicTypes.BINARY );
		registry.registerPattern( "setdata", "setdata(?1, ?2, ?3)", StandardSpiBasicTypes.BOOLEAN );
		registry.registerPattern( "charindex", "charindex(?1, ?2, ?3)", StandardSpiBasicTypes.INTEGER );
	}

	@Override
	public String getTableTypeString() {
		//HHH-7298 I don't know if this would break something or cause some side affects
		//but it is required to use 'select for update'
		return " lock datarows";
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String appendLockHint(LockOptions mode, String tableName) {
		return tableName;
	}

	@Override
	public String applyLocksToSql(String sql, LockOptions aliasedLockOptions, Map<String, String[]> keyColumnNames) {
		return sql + new ForUpdateFragment( this, aliasedLockOptions, keyColumnNames ).toFragmentString();
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				if("JZ0TO".equals( sqlState ) || "JZ006".equals( sqlState )){
					throw new LockTimeoutException( message, sqlException, sql );
				}
				if ( 515 == errorCode && "ZZZZZ".equals( sqlState ) ) {
					// Attempt to insert NULL value into column; column does not allow nulls.
					final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName( sqlException );
					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}
				return null;
			}
		};
	}
}
