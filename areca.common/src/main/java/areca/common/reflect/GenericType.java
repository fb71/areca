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
package areca.common.reflect;

/**
 *
 * @author Falko Bräutigam
 */
public interface GenericType {

    /**
     *
     */
    public static class ClassType implements GenericType {

        private Class<?>        rawType;

        public ClassType( Class<?> rawType ) {
            this.rawType = rawType;
        }

        public Class<?> getRawType() {
            return rawType;
        }
    }

    /**
     *
     */
    public static class ParameterizedType implements GenericType {

        protected Class<?>      rawType;

        protected GenericType   ownerType;

        protected GenericType[] actualTypeArguments;


        public ParameterizedType( Class<?> rawType, GenericType ownerType, GenericType[] actualTypeArguments ) {
            this.rawType = rawType;
            this.ownerType = ownerType;
            this.actualTypeArguments = actualTypeArguments;
        }

        public GenericType[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        public Class<?> getRawType() {
            return rawType;
        }

        public GenericType getOwnerType() {
            // XXX Auto-generated method stub
            throw new RuntimeException( "not yet implemented." );
        }
    }

}
