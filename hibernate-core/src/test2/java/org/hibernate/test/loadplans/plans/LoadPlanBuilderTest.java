/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.plans;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.loader.plan.build.internal.NavigableVisitationStrategyCascadeStyleLoadPlanBuildingImpl;
import org.hibernate.loader.plan.build.internal.SqlSelectPlanBuilder;
import org.hibernate.loader.plan.build.spi.LoadPlanTreePrinter;
import org.hibernate.loader.plan.build.spi.MetamodelDrivenSqlSelectPlanBuilder;
import org.hibernate.loader.plan.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class LoadPlanBuilderTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Message.class, Poster.class };
	}

	@Test
	public void testSimpleBuild() {
		EntityDescriptor ep = (EntityDescriptor) sessionFactory().getClassMetadata( Message.class);
		SqlSelectPlanBuilder strategy = new SqlSelectPlanBuilder(
				sessionFactory(),
				LoadQueryInfluencers.NONE,
				LockMode.NONE
		);
		LoadPlan plan = MetamodelDrivenSqlSelectPlanBuilder.buildRootEntityLoadPlan( strategy, ep );
		assertFalse( plan.hasAnyScalarReturns() );
		assertEquals( 1, plan.getReturns().size() );
		Return rtn = plan.getReturns().get( 0 );
		EntityReturn entityReturn = ExtraAssertions.assertTyping( EntityReturn.class, rtn );
		assertNotNull( entityReturn.getFetches() );
		assertEquals( 1, entityReturn.getFetches().length );
		Fetch fetch = entityReturn.getFetches()[0];
		EntityFetch entityFetch = ExtraAssertions.assertTyping( EntityFetch.class, fetch );
		assertNotNull( entityFetch.getFetches() );
		assertEquals( 0, entityFetch.getFetches().length );

		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sessionFactory() ) );
	}

	@Test
	public void testCascadeBasedBuild() {
		EntityDescriptor ep = (EntityDescriptor) sessionFactory().getClassMetadata( Message.class);
		NavigableVisitationStrategyCascadeStyleLoadPlanBuildingImpl strategy = new NavigableVisitationStrategyCascadeStyleLoadPlanBuildingImpl(
				CascadingActions.MERGE,
				sessionFactory(),
				LoadQueryInfluencers.NONE,
				LockMode.NONE
		);
		LoadPlan plan = MetamodelDrivenSqlSelectPlanBuilder.buildRootEntityLoadPlan( strategy, ep );
		assertFalse( plan.hasAnyScalarReturns() );
		assertEquals( 1, plan.getReturns().size() );
		Return rtn = plan.getReturns().get( 0 );
		EntityReturn entityReturn = ExtraAssertions.assertTyping( EntityReturn.class, rtn );
		assertNotNull( entityReturn.getFetches() );
		assertEquals( 1, entityReturn.getFetches().length );
		Fetch fetch = entityReturn.getFetches()[0];
		EntityFetch entityFetch = ExtraAssertions.assertTyping( EntityFetch.class, fetch );
		assertNotNull( entityFetch.getFetches() );
		assertEquals( 0, entityFetch.getFetches().length );

		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sessionFactory() ) );
	}

	@Test
	public void testCollectionInitializerCase() {
		PersistentCollectionDescriptor cp = sessionFactory().getCollectionPersister( Poster.class.getName() + ".messages" );
		SqlSelectPlanBuilder strategy = new SqlSelectPlanBuilder(
				sessionFactory(),
				LoadQueryInfluencers.NONE,
				LockMode.NONE
		);
		LoadPlan plan = MetamodelDrivenSqlSelectPlanBuilder.buildRootCollectionLoadPlan( strategy, cp );
		assertFalse( plan.hasAnyScalarReturns() );
		assertEquals( 1, plan.getReturns().size() );
		Return rtn = plan.getReturns().get( 0 );
		CollectionReturn collectionReturn = ExtraAssertions.assertTyping( CollectionReturn.class, rtn );

		assertNotNull( collectionReturn.getElementGraph() );
		assertNotNull( collectionReturn.getElementGraph().getFetches() );
		// the collection Message elements are fetched, but Message.poster is not fetched
		// (because that collection is owned by that Poster)
		assertEquals( 0, collectionReturn.getElementGraph().getFetches().length );
		EntityReference entityReference = ExtraAssertions.assertTyping( EntityReference.class, collectionReturn.getElementGraph() );
		assertNotNull( entityReference.getFetches() );
		assertEquals( 0, entityReference.getFetches().length );

		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sessionFactory() ) );
	}

	@Entity( name = "Message" )
	public static class Message {
		@Id
		private Integer mid;
		private String msgTxt;
		@ManyToOne( cascade = CascadeType.MERGE )
		@JoinColumn
		private Poster poster;
	}

	@Entity( name = "Poster" )
	public static class Poster {
		@Id
		private Integer pid;
		private String name;
		@OneToMany(mappedBy = "poster")
		private List<Message> messages;
	}

}