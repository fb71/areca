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
package areca.app.model;

import org.polymap.model2.Association;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;
import org.polymap.model2.Queryable;

import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class Contact extends Common {

    public static final ContactClassInfo info = ContactClassInfo.instance();

    public static Contact TYPE;

    @Nullable
    @Queryable
    public Property<String>         firstname;

    @Nullable
    @Queryable
    public Property<String>         lastname;

    @Nullable
    @Queryable
    public Property<String>         email;

    @Nullable
    @Queryable
    public Property<String>         phone;

    @Nullable
    @Queryable
    public Property<String>         storeRef;

    @Nullable
    public Property<String>         photo;

    @Nullable
    public Association<Anchor>      anchor;

    public String label() {
        return String.format( "%s %s", firstname.opt().orElse( "" ), lastname.opt().orElse( "" ) );
    }

}
