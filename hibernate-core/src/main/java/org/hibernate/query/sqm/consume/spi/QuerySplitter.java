/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.SqmPathRegistry;
import org.hibernate.query.sqm.produce.SqmQuerySpecCreationProcessingState;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.produce.spi.SqmCreationOptions;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.Distinctable;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.EmptinessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.GroupedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.MemberOfSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmGroupByClause;
import org.hibernate.query.sqm.tree.select.SqmHavingClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;

/**
 * Handles splitting queries containing unmapped polymorphic references.
 *
 * @author Steve Ebersole
 */
public class QuerySplitter {
	public static SqmSelectStatement[] split(
			SqmSelectStatement statement,
			SessionFactoryImplementor sessionFactory) {
		// We only allow unmapped polymorphism in a very restricted way.  Specifically,
		// the unmapped polymorphic reference can only be a root and can be the only
		// root.  Use that restriction to locate the unmapped polymorphic reference
		SqmRoot unmappedPolymorphicReference = null;
		for ( SqmRoot root : statement.getQuerySpec().getFromClause().getRoots() ) {
			if ( root.getReferencedNavigable() instanceof SqmPolymorphicRootDescriptor ) {
				unmappedPolymorphicReference = root;
			}
		}

		if ( unmappedPolymorphicReference == null ) {
			return new SqmSelectStatement[] { statement };
		}

		final SqmPolymorphicRootDescriptor<?> unmappedPolymorphicDescriptor = (SqmPolymorphicRootDescriptor) unmappedPolymorphicReference.getReferencedNavigable();
		final SqmSelectStatement[] expanded = new SqmSelectStatement[ unmappedPolymorphicDescriptor.getImplementors().size() ];

		int i = -1;
		for ( EntityTypeDescriptor<?> mappedDescriptor : unmappedPolymorphicDescriptor.getImplementors() ) {
			i++;
			final UnmappedPolymorphismReplacer replacer = new UnmappedPolymorphismReplacer(
					unmappedPolymorphicReference,
					mappedDescriptor,
					sessionFactory
			);
			expanded[i] = replacer.visitSelectStatement( statement );
		}

		return expanded;
	}

	@SuppressWarnings("unchecked")
	private static class UnmappedPolymorphismReplacer extends BaseSemanticQueryWalker implements SqmCreationState {
		private final SqmRoot unmappedPolymorphicFromElement;
		private final EntityTypeDescriptor mappedDescriptor;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// `sqmPathCopyMap` ought to be able to replace `sqmFromCopyMap` and `navigableBindingCopyMap`
		private Map<NavigablePath, SqmPath> sqmPathCopyMap = new HashMap<>();

		private Map<SqmFrom,SqmFrom> sqmFromCopyMap = new HashMap<>();
		private Map<SqmNavigableReference, SqmNavigableReference> navigableBindingCopyMap = new HashMap<>();
		private Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPathCopy;
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		private UnmappedPolymorphismReplacer(
				SqmRoot unmappedPolymorphicFromElement,
				EntityTypeDescriptor mappedDescriptor,
				SessionFactoryImplementor sessionFactory) {
			super( sessionFactory.getTypeConfiguration(), sessionFactory.getServiceRegistry() );
			this.unmappedPolymorphicFromElement = unmappedPolymorphicFromElement;
			this.mappedDescriptor = mappedDescriptor;
		}

		@Override
		public SqmUpdateStatement visitUpdateStatement(SqmUpdateStatement statement) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public SqmSetClause visitSetClause(SqmSetClause setClause) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public SqmAssignment visitAssignment(SqmAssignment assignment) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public SqmDeleteStatement visitDeleteStatement(SqmDeleteStatement statement) {
			throw new UnsupportedOperationException( "Not valid" );
		}

		@Override
		public SqmSelectStatement visitSelectStatement(SqmSelectStatement statement) {
			final SqmSelectStatement copy = new SqmSelectStatement();
			copy.setQuerySpec( visitQuerySpec( statement.getQuerySpec() ) );
			copy.applyFetchJoinsByParentPath( fetchJoinsByParentPathCopy );
			return copy;
		}

		@Override
		public SqmQuerySpec visitQuerySpec(SqmQuerySpec querySpec) {
			// NOTE : it is important that we visit the SqmFromClause first so that the
			// 		fromElementCopyMap gets built before other parts of the queryspec
			// 		are visited

			final SqmQuerySpec sqmQuerySpec = new SqmQuerySpec();
			sqmQuerySpec.setFromClause( visitFromClause( querySpec.getFromClause() ) );
			sqmQuerySpec.setSelectClause( visitSelectClause( querySpec.getSelectClause() ) );
			sqmQuerySpec.setWhereClause( visitWhereClause( querySpec.getWhereClause() ) );
			sqmQuerySpec.setGroupByClause( visitGroupByClause( querySpec.getGroupByClause() ) );
			sqmQuerySpec.setOrderByClause( visitOrderByClause( querySpec.getOrderByClause() ) );
			sqmQuerySpec.setLimitExpression( (SqmExpression) querySpec.getLimitExpression().accept( this ) );
			sqmQuerySpec.setOffsetExpression( (SqmExpression) querySpec.getOffsetExpression().accept( this ) );

			return querySpec;
		}

		private SqmFromClause currentFromClauseCopy = null;

		@Override
		public SqmFromClause visitFromClause(SqmFromClause fromClause) {
			final SqmFromClause previousCurrent = currentFromClauseCopy;

			try {
				SqmFromClause copy = new SqmFromClause();
				currentFromClauseCopy = copy;
				super.visitFromClause( fromClause );
				return copy;
			}
			finally {
				currentFromClauseCopy = previousCurrent;
			}
		}

		@Override
		public SqmGroupByClause visitGroupByClause(SqmGroupByClause clause) {
			final SqmGroupByClause result = new SqmGroupByClause();
			clause.visitGroupings(
					grouping -> result.addGrouping(
							(SqmExpression) grouping.getExpression().accept( this ),
							grouping.getCollation()
					)
			);
			return result;
		}

		@Override
		public SqmGroupByClause.SqmGrouping visitGrouping(SqmGroupByClause.SqmGrouping grouping) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SqmHavingClause visitHavingClause(SqmHavingClause clause) {
			return new SqmHavingClause(
					(SqmPredicate) clause.getPredicate().accept( this )
			);
		}

		@Override
		public SqmRoot visitRootEntityFromElement(SqmRoot rootEntityFromElement) {
			return (SqmRoot) getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					rootEntityFromElement.getNavigablePath(),
					navigablePath -> {
						if ( rootEntityFromElement == unmappedPolymorphicFromElement ) {
							return new SqmRoot(
									rootEntityFromElement.getUniqueIdentifier(),
									rootEntityFromElement.getIdentificationVariable(),
									mappedDescriptor
							);
						}
						else {
							return new SqmRoot(
									rootEntityFromElement.getUniqueIdentifier(),
									rootEntityFromElement.getIdentificationVariable(),
									rootEntityFromElement.getReferencedNavigable().getEntityDescriptor()
							);
						}
					}
			);
		}

		@Override
		public SqmCrossJoin visitCrossJoinedFromElement(SqmCrossJoin join) {
			return (SqmCrossJoin) getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					join.getNavigablePath(),
					navigablePath -> new SqmCrossJoin(
							join.getUniqueIdentifier(),
							join.getIdentificationVariable(),
							join.getReferencedNavigable().getEntityDescriptor()
					)
			);
		}

		@Override
		public SqmEntityJoin visitQualifiedEntityJoinFromElement(SqmEntityJoin join) {
			return (SqmEntityJoin) getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					join.getNavigablePath(),
					navigablePath -> new SqmEntityJoin(
							join.getUniqueIdentifier(),
							join.getIdentificationVariable(),
							join.getReferencedNavigable().getEntityDescriptor(),
							join.getJoinType()
					)
			);
		}

		@Override
		public SqmNavigableJoin visitQualifiedAttributeJoinFromElement(SqmNavigableJoin join) {
			return (SqmNavigableJoin) getProcessingStateStack().getCurrent().getPathRegistry().resolvePath(
					join.getNavigablePath(),
					navigablePath -> new SqmNavigableJoin(
							join.getUniqueIdentifier(),
							getProcessingStateStack().getCurrent().getPathRegistry().findFromByPath( join.getLhs().getNavigablePath() ),
							join.getReferencedNavigable(),
							join.getIdentificationVariable(),
							join.getJoinType(),
							join.isFetched(),
							this
					)
			);
		}

		@Override
		public SqmBasicValuedSimplePath visitBasicValuedPath(SqmBasicValuedSimplePath path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			return (SqmBasicValuedSimplePath) pathRegistry.resolvePath(
					path.getNavigablePath(),
					navigablePath -> new SqmBasicValuedSimplePath(
							path.getUniqueIdentifier(),
							navigablePath,
							path.getReferencedNavigable(),
							pathRegistry.findFromByPath( path.getLhs().getNavigablePath() )
					)
			);
		}

		@Override
		public SqmEmbeddedValuedSimplePath visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			return (SqmEmbeddedValuedSimplePath) pathRegistry.resolvePath(
					path.getNavigablePath(),
					navigablePath -> new SqmEmbeddedValuedSimplePath(
							path.getUniqueIdentifier(),
							navigablePath,
							path.getReferencedNavigable(),
							pathRegistry.findFromByPath( path.getLhs().getNavigablePath() )
					)
			);
		}

		@Override
		public SqmEntityValuedSimplePath visitEntityValuedPath(SqmEntityValuedSimplePath path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			return (SqmEntityValuedSimplePath) pathRegistry.resolvePath(
					path.getNavigablePath(),
					navigablePath -> new SqmEntityValuedSimplePath(
							path.getUniqueIdentifier(),
							navigablePath,
							path.getReferencedNavigable(),
							pathRegistry.findFromByPath( path.getLhs().getNavigablePath() )
					)
			);
		}

		@Override
		public SqmPluralValuedSimplePath visitPluralValuedPath(SqmPluralValuedSimplePath path) {
			final SqmPathRegistry pathRegistry = getProcessingStateStack().getCurrent().getPathRegistry();

			return (SqmPluralValuedSimplePath) pathRegistry.resolvePath(
					path.getNavigablePath(),
					navigablePath -> new SqmPluralValuedSimplePath(
							path.getUniqueIdentifier(),
							navigablePath,
							path.getReferencedNavigable(),
							pathRegistry.findFromByPath( path.getLhs().getNavigablePath() )
					)
			);
		}

		@Override
		public SqmSelectClause visitSelectClause(SqmSelectClause selectClause) {
			SqmSelectClause copy = new SqmSelectClause( selectClause.isDistinct() );
			for ( SqmSelection selection : selectClause.getSelections() ) {
				copy.addSelection(
						new SqmSelection(
								(SqmExpression) selection.getSelectableNode().accept( this ),
								selection.getAlias()
						)
				);
			}
			return copy;
		}

		@Override
		public SqmDynamicInstantiation visitDynamicInstantiation(SqmDynamicInstantiation original) {
			final SqmDynamicInstantiationTarget instantiationTarget = original.getInstantiationTarget();
			final SqmDynamicInstantiation copy;

			switch ( instantiationTarget.getNature() ) {
				case MAP: {
					copy = SqmDynamicInstantiation.forMapInstantiation( instantiationTarget.getTargetTypeDescriptor() );
					break;
				}
				case LIST: {
					copy = SqmDynamicInstantiation.forListInstantiation( instantiationTarget.getTargetTypeDescriptor() );
					break;
				}
				default: {
					copy = SqmDynamicInstantiation.forClassInstantiation( instantiationTarget.getTargetTypeDescriptor() );
				}
			}

			for ( SqmDynamicInstantiationArgument originalArgument : original.getArguments() ) {
				copy.addArgument(
						new SqmDynamicInstantiationArgument(
								( SqmSelectableNode) originalArgument.getSelectableNode().accept( this ),
								originalArgument.getAlias()
						)
				);
			}

			return copy;
		}

		@Override
		public SqmWhereClause visitWhereClause(SqmWhereClause whereClause) {
			if ( whereClause == null ) {
				return null;
			}
			return new SqmWhereClause( (SqmPredicate) whereClause.getPredicate().accept( this ) );
		}

		@Override
		public GroupedSqmPredicate visitGroupedPredicate(GroupedSqmPredicate predicate) {
			return new GroupedSqmPredicate( (SqmPredicate) predicate.accept( this ) );
		}

		@Override
		public AndSqmPredicate visitAndPredicate(AndSqmPredicate predicate) {
			return new AndSqmPredicate(
					(SqmPredicate) predicate.getLeftHandPredicate().accept( this ),
					(SqmPredicate) predicate.getRightHandPredicate().accept( this )
			);
		}

		@Override
		public OrSqmPredicate visitOrPredicate(OrSqmPredicate predicate) {
			return new OrSqmPredicate(
					(SqmPredicate) predicate.getLeftHandPredicate().accept( this ),
					(SqmPredicate) predicate.getRightHandPredicate().accept( this )
			);
		}

		@Override
		public SqmComparisonPredicate visitComparisonPredicate(SqmComparisonPredicate predicate) {
			return new SqmComparisonPredicate(
					(SqmExpression) predicate.getLeftHandExpression().accept( this ), predicate.getOperator(),
					(SqmExpression) predicate.getRightHandExpression().accept( this )
			);
		}

		@Override
		public EmptinessSqmPredicate visitIsEmptyPredicate(EmptinessSqmPredicate predicate) {
			return new EmptinessSqmPredicate(
					(SqmPluralAttributeReference) predicate.getExpression().accept( this ),
					predicate.isNegated()
			);
		}

		@Override
		public NullnessSqmPredicate visitIsNullPredicate(NullnessSqmPredicate predicate) {
			return new NullnessSqmPredicate(
					(SqmExpression) predicate.getExpression().accept( this ),
					predicate.isNegated()
			);
		}

		@Override
		public BetweenSqmPredicate visitBetweenPredicate(BetweenSqmPredicate predicate) {
			return new BetweenSqmPredicate(
					(SqmExpression) predicate.getExpression().accept( this ),
					(SqmExpression) predicate.getLowerBound().accept( this ),
					(SqmExpression) predicate.getUpperBound().accept( this ),
					predicate.isNegated()
			);
		}

		@Override
		public LikeSqmPredicate visitLikePredicate(LikeSqmPredicate predicate) {
			return new LikeSqmPredicate(
					(SqmExpression) predicate.getMatchExpression().accept( this ),
					(SqmExpression) predicate.getPattern().accept( this ),
					(SqmExpression) predicate.getEscapeCharacter().accept( this )
			);
		}

		@Override
		public MemberOfSqmPredicate visitMemberOfPredicate(MemberOfSqmPredicate predicate) {
			final SqmAttributeReference attributeReferenceCopy = resolveAttributeReference( predicate.getPluralAttributeReference() );
			// NOTE : no type check b4 cast as it is assumed that the initial SQM producer
			//		already verified that the path resolves to a plural attribute
			return new MemberOfSqmPredicate( (SqmPluralAttributeReference) attributeReferenceCopy );
		}

//		private DomainReferenceBinding resolveDomainReferenceBinding(DomainReferenceBinding binding) {
//			DomainReferenceBinding copy = navigableBindingCopyMap.get( binding );
//			if ( copy == null ) {
//				copy = makeDomainReferenceBindingCopy( binding );
//				navigableBindingCopyMap.put( binding, copy );
//			}
//			return copy;
//		}

//		private DomainReferenceBinding makeDomainReferenceBindingCopy(DomainReferenceBinding binding) {
//			if ( binding instanceof AttributeBinding ) {
//				final AttributeBinding attributeBinding = (AttributeBinding) binding;
//				return new AttributeBinding(
//						resolveDomainReferenceBinding( attributeBinding.getLhs() ),
//						attributeBinding.getBoundDomainReference(),
//						attributeBinding.getFromElement()
//				);
//			}
//			else if ( binding instanceof )
//		}


		// todo (6.0) : broker in SqmNavigableReference instead?

		private SqmAttributeReference resolveAttributeReference(SqmAttributeReference attributeBinding) {
			// its an attribute join... there has to be a source
			assert attributeBinding.getSourceReference() != null;

			SqmAttributeReference attributeBindingCopy = (SqmAttributeReference) navigableBindingCopyMap.get( attributeBinding );
			if ( attributeBindingCopy == null ) {
				attributeBindingCopy = makeCopy( attributeBinding );
			}

			return attributeBindingCopy;
		}

		private SqmAttributeReference makeCopy(SqmAttributeReference attributeReference) {
			// its an attribute join... there has to be a source
			assert attributeReference.getSourceReference() != null;

			assert !navigableBindingCopyMap.containsKey( attributeReference );

			final SqmNavigableJoin originalJoin = (SqmNavigableJoin) sqmFromCopyMap.get( attributeReference.getExportedFromElement() );
			final SqmNavigableContainerReference sourceNavRef = (SqmNavigableContainerReference) navigableBindingCopyMap.get(
					attributeReference.getSourceReference()
			);

			if ( sourceNavRef == null ) {
				throw new ParsingException( "Could not resolve NavigableSourceBinding copy during query splitting" );
			}

			final SqmAttributeReference attributeBindingCopy = (SqmAttributeReference) sourceNavRef.getReferencedNavigable().createSqmExpression(
					sourceNavRef.getExportedFromElement(),
					sourceNavRef,
					this
			);
			navigableBindingCopyMap.put( attributeReference, attributeBindingCopy );
			return attributeBindingCopy;
		}

		@Override
		public NegatedSqmPredicate visitNegatedPredicate(NegatedSqmPredicate predicate) {
			return new NegatedSqmPredicate(
					(SqmPredicate) predicate.getWrappedPredicate().accept( this )
			);
		}

		@Override
		public InListSqmPredicate visitInListPredicate(InListSqmPredicate predicate) {
			InListSqmPredicate copy = new InListSqmPredicate(
					(SqmExpression) predicate.getTestExpression().accept( this )
			);
			for ( SqmExpression expression : predicate.getListExpressions() ) {
				copy.addExpression( (SqmExpression) expression.accept( this ) );
			}
			return copy;
		}

		@Override
		public InSubQuerySqmPredicate visitInSubQueryPredicate(InSubQuerySqmPredicate predicate) {
			return new InSubQuerySqmPredicate(
					(SqmExpression) predicate.getTestExpression().accept( this ),
					visitSubQueryExpression( predicate.getSubQueryExpression() )
			);
		}

		@Override
		public SqmOrderByClause visitOrderByClause(SqmOrderByClause orderByClause) {
			if ( orderByClause == null ) {
				return null;
			}

			SqmOrderByClause copy = new SqmOrderByClause();
			for ( SqmSortSpecification sortSpecification : orderByClause.getSortSpecifications() ) {
				copy.addSortSpecification( visitSortSpecification( sortSpecification ) );
			}
			return copy;
		}

		@Override
		public SqmSortSpecification visitSortSpecification(SqmSortSpecification sortSpecification) {
			return new SqmSortSpecification(
					(SqmExpression) sortSpecification.getSortExpression().accept( this ),
					sortSpecification.getCollation(),
					sortSpecification.getSortOrder()
			);
		}



		@Override
		public SqmPositionalParameter visitPositionalParameterExpression(SqmPositionalParameter expression) {
			return new SqmPositionalParameter( expression.getPosition(), expression.allowMultiValuedBinding() );
		}

		@Override
		public SqmNamedParameter visitNamedParameterExpression(SqmNamedParameter expression) {
			return new SqmNamedParameter( expression.getName(), expression.allowMultiValuedBinding() );
		}

		@Override
		public SqmLiteralEntityType visitEntityTypeLiteralExpression(SqmLiteralEntityType expression) {
			return new SqmLiteralEntityType( expression.getExpressableType() );
		}

		@Override
		public SqmUnaryOperation visitUnaryOperationExpression(SqmUnaryOperation expression) {
			return new SqmUnaryOperation(
					expression.getOperation(),
					(SqmExpression) expression.getOperand().accept( this )
			);
		}

		@Override
		public SqmGenericFunction visitGenericFunction(SqmGenericFunction expression) {
			List<SqmExpression> argumentsCopy = new ArrayList<>();
			for ( SqmExpression argument : expression.getArguments() ) {
				argumentsCopy.add( (SqmExpression) argument.accept( this ) );
			}
			return new SqmGenericFunction(
					expression.getFunctionName(),
					expression.getExpressableType(),
					argumentsCopy
			);
		}

		@Override
		public SqlAstFunctionProducer visitSqlAstFunctionProducer(SqlAstFunctionProducer functionProducer) {
			// todo (6.0) : likely this needs a copy too
			//		how to model that?
			//		for now, return the same reference
			return functionProducer;
		}

		@Override
		public SqmAvgFunction visitAvgFunction(SqmAvgFunction expression) {
			return handleDistinct(
					new SqmAvgFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		private <T extends SqmFunction> T handleDistinct(T function, boolean shouldMakeDistinction) {
			if ( function instanceof Distinctable
					&& shouldMakeDistinction ) {
				( (Distinctable) function ).makeDistinct();
			}

			return function;
		}

		@Override
		public SqmCountStarFunction visitCountStarFunction(SqmCountStarFunction expression) {
			return handleDistinct(
					new SqmCountStarFunction( expression.getExpressableType() ),
					expression.isDistinct()
			);

		}

		@Override
		public SqmCountFunction visitCountFunction(SqmCountFunction expression) {
			return handleDistinct(
					new SqmCountFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmMaxFunction visitMaxFunction(SqmMaxFunction expression) {
			return handleDistinct(
					new SqmMaxFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmMinFunction visitMinFunction(SqmMinFunction expression) {
			return handleDistinct(
					new SqmMinFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmSumFunction visitSumFunction(SqmSumFunction expression) {
			return handleDistinct(
					new SqmSumFunction(
							(SqmExpression) expression.getArgument().accept( this ),
							expression.getExpressableType()
					),
					expression.isDistinct()
			);
		}

		@Override
		public SqmLiteral visitLiteral(SqmLiteral literal) {
			return new SqmLiteral( literal.getLiteralValue(), literal.getExpressableType() );
		}

		@Override
		public SqmConcat visitConcatExpression(SqmConcat expression) {
			return new SqmConcat(
					(SqmExpression) expression.getLeftHandOperand().accept( this ),
					(SqmExpression) expression.getRightHandOperand().accept( this )
			);
		}

		@Override
		public SqmConcatFunction visitConcatFunction(SqmConcatFunction expression) {
			final List<SqmExpression> arguments = new ArrayList<>();
			for ( SqmExpression argument : expression.getExpressions() ) {
				arguments.add( (SqmExpression) argument.accept( this ) );
			}

			return new SqmConcatFunction(
					(BasicValuedExpressableType) expression.getExpressableType(),
					arguments
			);
		}

		@Override
		public SqmBinaryArithmetic visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
			return new SqmBinaryArithmetic(
					(SqmExpression) expression.getLeftHandOperand().accept( this ),
					expression.getOperator(),
					(SqmExpression) expression.getRightHandOperand().accept( this ),
					expression.getExpressableType()
			);
		}

		@Override
		public SqmSubQuery visitSubQueryExpression(SqmSubQuery expression) {
			// its not supported for a SubQuery to define a dynamic instantiation, so
			//		any "selectable node" will only ever be an SqmExpression
			return new SqmSubQuery(
					visitQuerySpec( expression.getQuerySpec() ),
					// assume already validated
					( (SqmExpression) expression.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode() ).getExpressableType()
			);
		}

		@Override
		public SqmQuerySpecCreationProcessingState getCurrentQuerySpecProcessingState() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public Stack<SqmCreationProcessingState> getProcessingStateStack() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public SqmCreationContext getCreationContext() {
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public SqmCreationOptions getCreationOptions() {
			return () -> false;
		}

		@Override
		public String generateUniqueIdentifier() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public ImplicitAliasGenerator getImplicitAliasGenerator() {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public void cacheNavigableReference(SqmNavigableReference reference) {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public SqmNavigableReference getCachedNavigableReference(
				SqmNavigableContainerReference source, Navigable navigable) {
			// todo (6.0) : not sure these are needed
			throw new NotYetImplementedFor6Exception(  );
		}

		@Override
		public void registerFetch(SqmNavigableContainerReference sourceReference, SqmNavigableJoin navigableJoin) {
			Set<SqmNavigableJoin> joins = null;
			if ( fetchJoinsByParentPathCopy == null ) {
				fetchJoinsByParentPathCopy = new HashMap<>();
			}
			else {
				joins = fetchJoinsByParentPathCopy.get( sourceReference.getNavigablePath() );
			}

			if ( joins == null ) {
				joins = new HashSet<>();
				fetchJoinsByParentPathCopy.put( sourceReference.getNavigablePath(), joins );
			}

			joins.add( navigableJoin );
		}
	}

}
