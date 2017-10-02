/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.query.ScrollableResultsDeprecations;

/**
 * @author Steve Ebersole
 *
 * @deprecated See deprecation notice on {@link #get(int)}
 */
@Deprecated
public interface ScrollableResultsDeprecationsImplementor extends ScrollableResultsDeprecations {
	int getNumberOfTypes();
}