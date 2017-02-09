/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.access.UpdateTimestampsRegionAccess;

/**
 * @author Steve Ebersole
 */
public class UpdateTimestampsRegionAccessImpl extends BaseRegionAccess implements UpdateTimestampsRegionAccess {
	public UpdateTimestampsRegionAccessImpl(RegionImpl region) {
		super( region );
	}

	@Override
	protected boolean isDefaultMinimalPutOverride() {
		return getInternalRegion().getRegionFactory().isMinimalPutsEnabledByDefault();
	}
}
