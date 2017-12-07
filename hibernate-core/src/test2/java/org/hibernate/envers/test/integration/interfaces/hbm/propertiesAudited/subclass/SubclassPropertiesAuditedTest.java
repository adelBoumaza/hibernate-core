/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.subclass;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class SubclassPropertiesAuditedTest extends AbstractPropertiesAuditedTest {
	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/envers/test/interfaces/subclassPropertiesAuditedMappings.hbm.xml" };
	}
}