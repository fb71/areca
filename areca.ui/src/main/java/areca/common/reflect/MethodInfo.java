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

import java.util.List;

/**
 *
 * @author Falko Bräutigam
 */
public class MethodInfo
        implements Named, Annotated {

    public String                   name;

    public List<AnnotationInfo>     annotations;


    @Override
    public String toString() {
        return "MethodInfo[name=" + name + "]";
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public List<AnnotationInfo> annotations() {
        return annotations;
    }


    public void invoke( Object obj, Object... params ) {
        throw new RuntimeException( "not yet implemented." );
    }

}
