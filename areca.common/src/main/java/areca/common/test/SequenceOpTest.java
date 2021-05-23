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

import java.util.logging.Logger;

import areca.common.Assert;
import areca.common.base.SequenceOpImpl;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class SequenceOpTest {

    private static final Logger LOG = Logger.getLogger( SequenceOpTest.class.getName() );

    public static final ClassInfo<SequenceOpTest> info = SequenceOpTestClassInfo.instance();


//    @Test
//    public void forEachTest() {
//        MutableInt result = new MutableInt();
//        Sequence.of( Arrays.asList( 1, 2, 3 ) ).forEach( elm -> result.add( elm ) );
//        Assert.isEqual( 6, result.intValue() );
//
//        MutableInt indices = new MutableInt();
//        Sequence.of( "1", "2", "3" ).forEach( (elm,i) -> indices.add( i ) );
//        Assert.isEqual( 3, indices.intValue() );
//    }


    @Test
    public void sequenceSeriesPerformanceTest() {
        for (int i=0; i<100; i++) {
            var result = SequenceOpImpl.ofInts( 0, 99 )
                .filter( elm -> elm < 10 )
                .map( elm -> String.valueOf( elm ) )
                .reduce( "", String::concat ) ;
            Assert.isEqual( "0123456789", result );
        }
    }

}
