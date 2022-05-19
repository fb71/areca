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
package areca.ui.form;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import areca.common.base.Sequence;

/**
 *
 * @author Falko Bräutigam
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Context {

    public static final String DEFAULT_SCOPE = "";

    /**
     *
     */
    public enum Mode {
        IN, OUT, IN_OUT, LOCAL
    };

    /**
     *
     */
    public static class Scope {
        private String[]    parts;

        public static Scope of( Class<?> cl ) {
            return new Scope( cl.getClass().getName().split( "\\." ) );
        }

        protected Scope( String[] parts ) {
            this.parts = parts;
        }

        public String toString() {
            return String.join( ".", parts );
        }

        public Scope append( String part ) {
            return new Scope( Sequence.of( parts ).concat( Sequence.of( part ) ).toArray( String[]::new ) );
        }
    }


    // instance *******************************************

    public Class<?> type();

    public String scope() default DEFAULT_SCOPE;

    public Mode mode() default Mode.IN_OUT;
}
