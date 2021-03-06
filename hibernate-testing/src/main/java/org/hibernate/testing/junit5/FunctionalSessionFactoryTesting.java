/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.testing.orm.junit.FailureExpectedExtension;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Composite annotation for functional tests that
 * require a functioning SessionFactory.
 *
 * @apiNote Logically this should also include
 * `@TestInstance( TestInstance.Lifecycle.PER_CLASS )`
 * but that annotation is not conveyed (is that the
 * right word?  its not applied to the thing using this annotation).
 * Test classes should apply that themselves.
 *
 * @see SessionFactoryScopeExtension
 * @see DialectFilterExtension
 * @see FailureExpectedExtension
 *
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME )
@Target(ElementType.TYPE)
@Inherited
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@ExtendWith( SessionFactoryScopeExtension.class )
@ExtendWith( DialectFilterExtension.class )
@ExtendWith( FailureExpectedExtension.class )
@SuppressWarnings("WeakerAccess")
public @interface FunctionalSessionFactoryTesting {
}
