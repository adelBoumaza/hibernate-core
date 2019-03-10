/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.lang.reflect.Field;

import org.hibernate.query.criteria.sqm.JpaParameterSqmWrapper;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.AbstractSpecificSqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmDiscriminatorReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMapEntryBinding;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceAny;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.expression.function.SqmAbsFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmBitLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentDateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimeFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimestampFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLocateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmModFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSqrtFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmStrFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BooleanExpressionSqmPredicate;
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
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmGroupByClause;
import org.hibernate.query.sqm.tree.select.SqmHavingClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.produce.ordering.internal.SqmColumnReference;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;

/**
 * @author Steve Ebersole
 */
public interface SemanticQueryWalker<T> {
	T visitUpdateStatement(SqmUpdateStatement statement);

	T visitSetClause(SqmSetClause setClause);

	T visitAssignment(SqmAssignment assignment);

	T visitInsertSelectStatement(SqmInsertSelectStatement statement);

	T visitDeleteStatement(SqmDeleteStatement statement);

	T visitSelectStatement(SqmSelectStatement statement);

	T visitQuerySpec(SqmQuerySpec querySpec);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// from-clause / domain paths

	T visitFromClause(SqmFromClause fromClause);

	T visitRootEntityFromElement(SqmRoot rootEntityFromElement);

	T visitRootEntityReference(SqmEntityReference sqmEntityReference);

	T visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement);

	T visitQualifiedEntityJoinFromElement(SqmEntityJoin joinedFromElement);

	T visitQualifiedAttributeJoinFromElement(SqmNavigableJoin joinedFromElement);


	T visitBasicValuedPath(SqmBasicValuedSimplePath path);

	T visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path);

	T visitEntityValuedPath(SqmEntityValuedSimplePath path);

	T visitPluralValuedPath(SqmPluralValuedSimplePath path);




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// selections

	T visitSelectClause(SqmSelectClause selectClause);

	T visitSelection(SqmSelection selection);

	T visitGroupByClause(SqmGroupByClause clause);

	T visitGrouping(SqmGroupByClause.SqmGrouping grouping);

	T visitHavingClause(SqmHavingClause clause);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - navigable

	T visitEntityIdentifierReference(SqmEntityIdentifierReference sqmEntityIdentifierBinding);

	T visitBasicValuedSingularAttribute(SqmSingularAttributeReferenceBasic sqmAttributeReference);

	T visitEntityValuedSingularAttribute(SqmSingularAttributeReferenceEntity sqmAttributeReference);

	T visitEmbeddableValuedSingularAttribute(SqmSingularAttributeReferenceEmbedded sqmAttributeReference);

	T visitAnyValuedSingularAttribute(SqmSingularAttributeReferenceAny sqmAttributeReference);

	T visitPluralAttribute(SqmPluralAttributeReference reference);

	// todo (6.0) : split this based on the element type like we did for singular attributes
	//		aka:
	//			#visit

	T visitPluralAttributeElementBinding(SqmCollectionElementReference binding);

	T visitTreatedPath(SqmTreatedPath sqmTreatedPath);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - general

	T visitPositionalParameterExpression(SqmPositionalParameter expression);

	T visitNamedParameterExpression(SqmNamedParameter expression);

	T visitJpaParameterWrapper(JpaParameterSqmWrapper expression);

	T visitEntityTypeLiteralExpression(SqmLiteralEntityType expression);

	T visitDiscriminatorReference(SqmDiscriminatorReference expression);

	T visitParameterizedEntityTypeExpression(SqmParameterizedEntityType expression);

	T visitUnaryOperationExpression(SqmUnaryOperation expression);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - non-standard functions

	T visitGenericFunction(SqmGenericFunction expression);

	T visitSqlAstFunctionProducer(SqlAstFunctionProducer sqlAstFunctionProducer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - standard functions

	T visitAbsFunction(SqmAbsFunction function);

	T visitAvgFunction(SqmAvgFunction expression);

	T visitBitLengthFunction(SqmBitLengthFunction sqmBitLengthFunction);

	T visitCastFunction(SqmCastFunction expression);

	T visitCoalesceFunction(SqmCoalesceFunction expression);

	T visitCountFunction(SqmCountFunction expression);

	T visitCountStarFunction(SqmCountStarFunction expression);

	T visitCurrentDateFunction(SqmCurrentDateFunction sqmCurrentDate);

	T visitCurrentTimeFunction(SqmCurrentTimeFunction sqmCurrentTimeFunction);

	T visitCurrentTimestampFunction(SqmCurrentTimestampFunction sqmCurrentTimestampFunction);

	T visitExtractFunction(SqmExtractFunction function);

	T visitLengthFunction(SqmLengthFunction sqmLengthFunction);

	T visitLocateFunction(SqmLocateFunction function);

	T visitLowerFunction(SqmLowerFunction expression);

	T visitMaxFunction(SqmMaxFunction expression);

	T visitMinFunction(SqmMinFunction expression);

	T visitModFunction(SqmModFunction sqmModFunction);

	T visitNullifFunction(SqmNullifFunction expression);

	T visitSqrtFunction(SqmSqrtFunction sqmSqrtFunction);

	T visitStrFunction(SqmStrFunction sqmStrFunction);

	T visitSubstringFunction(SqmSubstringFunction expression);

	T visitSumFunction(SqmSumFunction expression);

	T visitTrimFunction(SqmTrimFunction expression);

	T visitUpperFunction(SqmUpperFunction expression);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// predicates

	T visitWhereClause(SqmWhereClause whereClause);

	T visitGroupedPredicate(GroupedSqmPredicate predicate);

	T visitAndPredicate(AndSqmPredicate predicate);

	T visitOrPredicate(OrSqmPredicate predicate);

	T visitComparisonPredicate(SqmComparisonPredicate predicate);

	T visitIsEmptyPredicate(EmptinessSqmPredicate predicate);

	T visitIsNullPredicate(NullnessSqmPredicate predicate);

	T visitBetweenPredicate(BetweenSqmPredicate predicate);

	T visitLikePredicate(LikeSqmPredicate predicate);

	T visitMemberOfPredicate(MemberOfSqmPredicate predicate);

	T visitNegatedPredicate(NegatedSqmPredicate predicate);

	T visitInListPredicate(InListSqmPredicate predicate);

	T visitInSubQueryPredicate(InSubQuerySqmPredicate predicate);

	T visitBooleanExpressionPredicate(BooleanExpressionSqmPredicate predicate);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sorting

	T visitOrderByClause(SqmOrderByClause orderByClause);

	T visitSortSpecification(SqmSortSpecification sortSpecification);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// paging

	T visitOffsetExpression(SqmExpression expression);
	T visitLimitExpression(SqmExpression expression);




	T visitPluralAttributeSizeFunction(SqmCollectionSize function);


	T visitPluralAttributeIndexFunction(SqmCollectionIndexReference function);

	T visitMapKeyBinding(SqmCollectionIndexReference binding);

	T visitMapEntryFunction(SqmMapEntryBinding function);

	T visitMaxElementBinding(SqmMaxElementReference binding);

	T visitMinElementBinding(SqmMinElementReference binding);

	T visitMaxIndexFunction(AbstractSpecificSqmCollectionIndexReference function);

	T visitMinIndexFunction(SqmMinIndexReferenceBasic function);

	T visitLiteral(SqmLiteral literal);

	T visitConcatExpression(SqmConcat expression);

	T visitConcatFunction(SqmConcatFunction expression);

	T visitBinaryArithmeticExpression(SqmBinaryArithmetic expression);

	T visitSubQueryExpression(SqmSubQuery expression);

	T visitSimpleCaseExpression(SqmCaseSimple expression);

	T visitSearchedCaseExpression(SqmCaseSearched expression);

	T visitExplicitColumnReference(SqmColumnReference sqmColumnReference);



	T visitDynamicInstantiation(SqmDynamicInstantiation sqmDynamicInstantiation);


	T visitFullyQualifiedClass(Class<?> namedClass);

	T visitFullyQualifiedEnum(Enum<?> value);

	T visitFullyQualifiedField(Field field);
}
