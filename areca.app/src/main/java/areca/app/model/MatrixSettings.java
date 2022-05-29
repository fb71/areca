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

import org.polymap.model2.Entity;
import org.polymap.model2.Nullable;
import org.polymap.model2.Property;

import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class MatrixSettings
        extends Entity {

    public static final ClassInfo<MatrixSettings> info = MatrixSettingsClassInfo.instance();

    public static MatrixSettings    TYPE;

    /** Without proxy prefix */
    public Property<String>         baseUrl;

    /**  Something like @bolo:fulda.social */
    public Property<String>         username;

    @Nullable
    public Property<String>         accessToken;

    @Nullable
    public Property<String>         pwd;

}
