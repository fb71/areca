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
package areca.common;

/**
 *
 * @author Falko Bräutigam
 */
public class NullProgressMonitor
        extends ProgressMonitor {

    public static final NullProgressMonitor INSTANCE = new NullProgressMonitor();

    @Override
    public ProgressMonitor beginTask( String name, int totalWork ) {
        return this;
    }

    @Override
    public ProgressMonitor subTask( String name ) {
        return this;
    }

    @Override
    public ProgressMonitor worked( int work ) {
        return this;
    }

    @Override
    public ProgressMonitor done() {
        return this;
    }

    @Override
    public void setTotalWork( int toAdd ) {
    }

}
