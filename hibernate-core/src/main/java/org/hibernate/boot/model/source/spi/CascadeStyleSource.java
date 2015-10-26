/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;

/**
 * Describes sources which define cascading.
 *
 * @author Steve Ebersole
 */
public interface CascadeStyleSource {
	/**
	 * Obtain the cascade styles to be applied to this association.
	 *
	 * @return The cascade styles.
	 */
	Set<CascadeStyle> getCascadeStyles();
}
