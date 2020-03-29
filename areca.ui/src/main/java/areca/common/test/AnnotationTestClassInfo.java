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
package areca.common.test;

import java.util.Arrays;
import java.util.logging.Logger;

import areca.common.reflect.ClassInfo;
import areca.common.reflect.MethodInfo;

/**
 *
 * @author Falko Bräutigam
 */
public class AnnotationTestClassInfo
        extends ClassInfo<AnnotationTest> {

    private static final Logger LOG = Logger.getLogger( AnnotationTestClassInfo.class.getName() );

    public static final AnnotationTestClassInfo INFO = new AnnotationTestClassInfo();


    private AnnotationTestClassInfo() {
        cl = AnnotationTest.class;

        methods = Arrays.asList(
                testInfo() );
    }


    public MethodInfo testInfo() {
        throw new RuntimeException( "not yet implemented." );
//        List<ParameterInfo> params = new ArrayList<>();
//        MethodInfo info = new MethodInfo( "test", Void.class, params, annotations );
//        return info;
    }

}
