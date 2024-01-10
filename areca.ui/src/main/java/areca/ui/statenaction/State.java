/*
 * Copyright (C) 2024, the @authors. All rights reserved.
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
package areca.ui.statenaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Falko Bräutigam
 */
public interface State {

    /**
     * Denotes an action of a {@link State}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD,ElementType.FIELD})
    public @interface Action {}

    /**
     * Denotes one or more methods of a {@link State} which are called after all
     * {@link Context} variables are injected and before the State is activated.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Init {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface Dispose {}

    /**
     * Denotes a context variable to be injected into a page. Context variables are
     * passed/shared between {@link State}s.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Context {
        public static String DEFAULT_SCOPE = "_default_";

        String scope() default DEFAULT_SCOPE;

        /** This context variable is required to be not null. */
        boolean required() default true;
    }

    /**
     * Creates a new stack of {@link State}s. This method is mainly used
     * to start a new application or session.
     */
    public static StateBuilder start( Object startState ) {
        return new StateHolder().createState( startState );
    }

}
