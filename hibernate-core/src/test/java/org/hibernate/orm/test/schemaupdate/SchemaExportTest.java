/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class SchemaExportTest extends BaseSchemaUnitTestCase {
	private boolean doesDialectSupportDropTableIfExist() {
		return Dialect.getDialect().supportsIfExistsAfterTableName() || Dialect.getDialect()
				.supportsIfExistsBeforeTableName();
	}

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "schemaupdate/mapping.hbm.xml" };
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@Test
	public void testCreateAndDropOnlyType() {
		final SchemaExport schemaExport = createSchemaExport();

		// create w/o dropping first; (OK because tables don't exist yet
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.CREATE );
		assertEquals( 0, schemaExport.getExceptions().size() );

		// create w/o dropping again; should cause an exception because the tables exist already
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.CREATE );
		assertEquals( 1, schemaExport.getExceptions().size() );

		// drop tables only
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}

	@Test
	public void testBothType() {
		final SchemaExport schemaExport = createSchemaExport();

		// drop beforeQuery create (nothing to drop yeT)
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP );
		if ( doesDialectSupportDropTableIfExist() ) {
			assertEquals( 0, schemaExport.getExceptions().size() );
		}
		else {
			assertEquals( 1, schemaExport.getExceptions().size() );
		}

		// drop beforeQuery create again (this time drops the tables beforeQuery re-creating)
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH );
		int exceptionCount = schemaExport.getExceptions().size();
		if ( doesDialectSupportDropTableIfExist() ) {
			assertEquals( 0, exceptionCount );
		}

		// drop tables
		schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}

	@Test
	public void testGenerateDdlToFile() {
		final SchemaExport schemaExport = createSchemaExport();

		java.io.File outFile = new java.io.File( "schema.ddl" );
		schemaExport.setOutputFile( outFile.getPath() );

		// do not script to console or export to database
		schemaExport.execute( EnumSet.of( TargetType.SCRIPT ), SchemaExport.Action.DROP );
		if ( doesDialectSupportDropTableIfExist() && schemaExport.getExceptions().size() > 0 ) {
			assertEquals( 2, schemaExport.getExceptions().size() );
		}
		assertTrue( outFile.exists() );

		//check file is not empty
		assertTrue( outFile.length() > 0 );
		outFile.delete();
	}

	@Test
	public void testCreateAndDrop() {
		final SchemaExport schemaExport = createSchemaExport();

		// should drop beforeQuery creating, but tables don't exist yet
		schemaExport.create( EnumSet.of( TargetType.DATABASE ) );
		if ( doesDialectSupportDropTableIfExist() ) {
			assertEquals( 0, schemaExport.getExceptions().size() );
		}
		else {
			assertEquals( 1, schemaExport.getExceptions().size() );
		}

		// call create again; it should drop tables beforeQuery re-creating
		schemaExport.create( EnumSet.of( TargetType.DATABASE ) );
		assertEquals( 0, schemaExport.getExceptions().size() );

		// drop the tables
		schemaExport.drop( EnumSet.of( TargetType.DATABASE ) );
		assertEquals( 0, schemaExport.getExceptions().size() );
	}
}