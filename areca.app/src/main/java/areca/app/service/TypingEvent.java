/*
 * Copyright (C) 2022, the @authors. All rights reserved.
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
package areca.app.service;

import java.util.EventObject;

/**
 *
 * @author Falko Bräutigam
 */
public class TypingEvent
        extends EventObject {

    public boolean  typing;

    public TypingEvent( Object source, boolean typing ) {
        super( source );
        this.typing = typing;
    }

    /**
     * User-id or something.
     */
    @Override
    public String getSource() {
        return (String)super.getSource();
    }

}
