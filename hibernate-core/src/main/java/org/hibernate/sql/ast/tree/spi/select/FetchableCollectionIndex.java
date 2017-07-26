/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

/**
 * A collection index which is fetchable and is therefore also a {@link FetchParent}.
 *
 * @author Steve Ebersole
 */
public interface FetchableCollectionIndex extends FetchParent {
	/**
	 * Reference back to the collection to which this index belongs
	 *
	 * @return the collection reference.
	 */
	CollectionReference getCollectionReference();
}