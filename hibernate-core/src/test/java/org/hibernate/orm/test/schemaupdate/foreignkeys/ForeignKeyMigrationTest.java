/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.orm.test.schemaupdate.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature(value = { DialectChecks.SupportCatalogCreation.class })
public class ForeignKeyMigrationTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Box.class, Thing.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9716")
	public void testMigrationOfForeignKeys() {
		// first create the schema...
		createSchemaExport().create( EnumSet.of( TargetType.DATABASE ) );

		// try to update the just created schema
		createSchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ) );
	}

	@Entity(name = "Box")
	@Table(name = "Box", schema = "PUBLIC", catalog = "DB1")
	public static class Box {
		@Id
		public Integer id;
		@ManyToOne
		@JoinColumn
		public Thing thing1;
	}

	@Entity(name = "Thing")
	@Table(name = "Thing", schema = "PUBLIC", catalog = "DB1")
	public static class Thing {
		@Id
		public Integer id;
	}
}