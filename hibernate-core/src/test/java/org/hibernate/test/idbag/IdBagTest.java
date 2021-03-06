/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idbag;

import java.sql.SQLException;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class IdBagTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "idbag/UserGroup.hbm.xml" };
	}

	@Test
	public void testUpdateIdBag() throws HibernateException {
		inTransaction(
				s -> {
					User gavin = new User( "gavin" );
					Group admins = new Group( "admins" );
					Group plebs = new Group( "plebs" );
					Group moderators = new Group( "moderators" );
					Group banned = new Group( "banned" );
					gavin.getGroups().add( plebs );
					//gavin.getGroups().add(moderators);
					s.persist( gavin );
					s.persist( plebs );
					s.persist( admins );
					s.persist( moderators );
					s.persist( banned );
				}
		);

		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
					criteria.from( User.class );
					User gavin = s.createQuery( criteria ).uniqueResult();
//					User gavin = (User) s.createCriteria( User.class ).uniqueResult();
					Group admins = s.load( Group.class, "admins" );
					Group plebs = s.load( Group.class, "plebs" );
					Group banned = s.load( Group.class, "banned" );
					gavin.getGroups().add( admins );
					gavin.getGroups().remove( plebs );
					//gavin.getGroups().add(banned);

					s.delete( plebs );
					s.delete( banned );
					s.delete( s.load( Group.class, "moderators" ) );
					s.delete( admins );
					s.delete( gavin );
				}
		);
	}

	@Test
	public void testJoin() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User( "gavin" );
		Group admins = new Group( "admins" );
		Group plebs = new Group( "plebs" );
		gavin.getGroups().add( plebs );
		gavin.getGroups().add( admins );
		s.persist( gavin );
		s.persist( plebs );
		s.persist( admins );

		List l = s.createQuery( "from User u join u.groups g" ).list();
		assertEquals( l.size(), 2 );
		s.clear();

		gavin = (User) s.createQuery( "from User u join fetch u.groups" ).uniqueResult();
		assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
		assertEquals( gavin.getGroups().size(), 2 );
		assertEquals( ( (Group) gavin.getGroups().get( 0 ) ).getName(), "admins" );

		s.delete( gavin.getGroups().get( 0 ) );
		s.delete( gavin.getGroups().get( 1 ) );
		s.delete( gavin );

		t.commit();
		s.close();
	}

}

