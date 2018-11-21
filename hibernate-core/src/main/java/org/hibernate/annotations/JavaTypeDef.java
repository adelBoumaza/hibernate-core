/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Inherited
@Retention(RUNTIME)
@Repeatable( JavaTypeDefs.class )
public @interface JavaTypeDef {
	Class javaType();
	Class<? extends JavaTypeDescriptor> descriptorClass();

	// todo (6.0) : ? add some notion of auto-apply / local?
	//		-in other words, is this def local to its scope or is it
	// 		registered with the JavaTypeDescriptorRegistry?
}
