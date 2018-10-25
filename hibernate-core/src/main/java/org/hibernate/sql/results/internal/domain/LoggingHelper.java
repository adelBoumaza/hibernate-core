/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;

/**
 * Helper for logging specific to results processing.  Uses path collapsing
 * for readability
 *
 * @author Steve Ebersole
 */
public class LoggingHelper {
	public static String toLoggableString(NavigableRole role) {
		if ( role == null ) {
			return "<unreferenced>";
		}

		if ( role.isRoot() ) {
			return StringHelper.collapse( role.getFullPath() );
		}
		else {
			assert role.getParent() != null;
			return StringHelper.collapse( role.getParent().getFullPath() ) + '.' + role.getNavigableName();
		}
	}

	public static String toLoggableString(NavigablePath path) {
		assert path != null;

		if ( path.isRoot() ) {
			return StringHelper.collapse( path.getFullPath() );
		}
		else {
			assert path.getParent() != null;
			return StringHelper.collapse( path.getParent().getFullPath() ) + '.' + path.getLocalName();
		}
	}

	public static String toLoggableString(NavigableRole role, Object key) {
		if ( role == null ) {
			return "<unreferenced>";
		}

		return toLoggableString( toLoggableString( role ), key );
	}

	public static String toLoggableString(NavigablePath path, Object key) {
		assert path != null;
		return toLoggableString( toLoggableString( path ), key );
	}

	public static String toLoggableString(CollectionKey collectionKey) {
		return toLoggableString( toLoggableString( collectionKey.getNavigableRole() ), collectionKey.getKey() );
	}

	public static String toLoggableString(EntityKey entityKey) {
		return toLoggableString( StringHelper.collapse( entityKey.getEntityName() ), entityKey.getKeyValue() );
	}

	private static String toLoggableString(String roleOrPath, Object key) {
		assert roleOrPath != null;

		StringBuilder buffer = new StringBuilder();

		buffer.append( roleOrPath );
		buffer.append( '#' );

		if ( key == null ) {
			buffer.append( "<null>" );
		}
		else {
			buffer.append( key );
		}

		return buffer.toString();
	}
}
