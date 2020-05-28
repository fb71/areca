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
package areca.systemservice.client;

/**
 *
 * @author Falko Bräutigam
 */
public abstract class HierarchyVisitor {

    public boolean acceptsFolder( Path path ) {
        return true;
    }


    public boolean acceptsFile( Path path ) {
        return true;
    }


    public void visitFile( Path path, Object content ) {
    }


    public void onError( Exception e ) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException)e;
        }
        throw new RuntimeException( e );
    }

}
