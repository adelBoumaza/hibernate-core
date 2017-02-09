/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.CollectionCacheDataDescription;

/**
 * @author Strong Liu
 */
class ReadOnlyCollectionRegionAccess extends BaseCollectionRegionAccess {
	ReadOnlyCollectionRegionAccess(
			CollectionCacheDataDescription metadata,
			CacheKeysFactory cacheKeysFactory,
			RegionImpl region) {
		super( cacheKeysFactory, region );
	}
}
