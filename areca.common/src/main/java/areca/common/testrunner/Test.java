/*
 * Copyright (C) 2020, the @authors. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package areca.common.testrunner;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import areca.common.Promise;

/**
 * Flags a method as a test.
 * <p>
 * A test method can return <code>null</code> or, if the methods runs any
 * asynchronous computations, a {@link Promise}.
 *
 * @author Falko Bräutigam
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface Test {

    public static final TestAnnotationInfo info = TestAnnotationInfo.INFO;

    class NoException
            extends Exception {
    }

    /** Default: {@link NoException} */
    public Class<? extends Throwable> expected() default NoException.class;

}
