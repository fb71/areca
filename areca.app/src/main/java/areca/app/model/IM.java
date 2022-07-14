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
package areca.app.model;

import org.polymap.model2.Composite;
import org.polymap.model2.Property;

import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;

/**
 * Messenger ID, phone number, user name, ...whatever.
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class IM
        extends Composite {

    public static final ClassInfo<IM> info = IMClassInfo.instance();

    public static IM TYPE;

    public Property<String>     type;

    public Property<String>     name;
}
