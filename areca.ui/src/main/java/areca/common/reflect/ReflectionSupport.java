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

import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class ReflectionSupport {

    private static final Logger LOG = Logger.getLogger( ReflectionSupport.class.getName() );

    protected static ReflectionSupport       instance;

    public static ReflectionSupport instance() {
        return instance;
    }

    // instance *******************************************

    public abstract Map<String,MethodInfo> methodsOf( Class<?> cl );
}
