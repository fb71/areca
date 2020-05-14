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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.io.IOException;

import org.apache.commons.lang3.mutable.MutableInt;
import areca.common.Assert;
import areca.common.base.Sequence;
import areca.common.reflect.ClassInfo;
import areca.common.testrunner.Test;

/**
 *
 * @author Falko Bräutigam
 */
@Test
public class SequenceTest {

    private static final Logger LOG = Logger.getLogger( SequenceTest.class.getName() );

    public static final ClassInfo<SequenceTest> info = SequenceTestClassInfo.instance();


    @Test
    public void forEachTest() {
        MutableInt result = new MutableInt();
        Sequence.of( Arrays.asList( 1, 2, 3 ) ).forEach( elm -> result.add( elm ) );
        Assert.isEqual( 6, result.intValue() );
    }


    @Test
    public void concatTest() {
        Sequence<String,RuntimeException> s = Sequence.of( "1", "2" ).concat( Sequence.of( "4", "5" ) );
        Assert.isEqual( 4, s.count() );
        Assert.isEqual( "1245", s.reduce( String::concat ) );
        Assert.isEqual( 2, Sequence.of().concat( Sequence.of( "4", "5" ) ).count() );
        Assert.isEqual( 0, Sequence.of().concat( Sequence.of() ).count() );
    }


    @Test
    public void reduceTest() {
        Assert.isEqual( 6, Sequence.of( 1, 2, 3 ).reduce( (r,elm) -> r + elm ) );
        Assert.isEqual( 16, Sequence.of( 1, 2, 3 ).reduce( 10, SequenceTest::sum ) );

        Assert.isEqual( "123", Sequence.of( 1, 2, 3 ).reduce( "", (r,elm) -> r + elm ) );
        Assert.isEqual( "123", Sequence.of( "1", "2", "3" ).reduce( String::concat ) );
        Assert.isEqual( "123", Sequence.of( 1, 2, 3 ).reduce( new StringBuilder(), StringBuilder::append ).toString() );
    }


    @Test
    public void transformTest() {
        Sequence<Integer,RuntimeException> sequence = Sequence.of( 1, 2, 3 );
        Assert.isEqual( "234", sequence
                .transform( elm -> {LOG.info( ":"+elm ); return elm + 1; } )
                .transform( elm -> {LOG.info( "::"+elm ); return String.valueOf( elm ); } )
                .reduce( String::concat ) );

        Assert.that( sequence != sequence.transform( elm -> 0 ) );

        Assert.isEqual( "123", sequence.transform( String::valueOf ).reduce( String::concat ) );
    }


    @Test(expected = IOException.class)
    public void exceptionChainTest() throws IOException {
        Sequence.of( IOException.class, Arrays.asList( 1, 2, 3 ) )
                .transform( elm -> throwException() )
                .forEach( elm -> LOG.info( ":: " + elm ) );
    }

    @Test
    public void collectTest() {
        List<Integer> l1 = Sequence.of( 1, 2, 3 ).collect( Collectors.toList() );
        Assert.that( l1 instanceof ArrayList );
        Assert.isEqual( l1.size(), 3 );

        Set<Integer> s1 = Sequence.of( 1, 2, 3 ).collect( Collectors.toSet() );
        Assert.that( s1 instanceof HashSet );
        Assert.isEqual( s1.size(), 3 );

        // not supported by TeaVM
//        Map<Integer,String> m1 = Sequence.withoutExceptions( 1, 2, 3 ).collect( Collectors.toMap( elm -> elm, elm -> "elm:"+elm) );
//        Assert.that( m1 instanceof HashMap );
//        Assert.isEqual( m1.size(), 3 );
//        Assert.isEqual( m1.get( 1 ), "elm:1" );
    }


    @Test
    public void filterTest() {
        Sequence<Integer,RuntimeException> sequence = Sequence.of( 1, 2, 3 );
        Assert.that( sequence != sequence.filter( elm -> true ) );
        Assert.isEqual( 2, sequence.filter( elm -> elm != 3 ).count() );
        Assert.isEqual( 2, sequence.filter( elm -> elm != 2 ).count() );
        Assert.isEqual( 2, sequence.filter( elm -> elm != 1 ).count() );
        Assert.isEqual( 3, sequence.filter( elm -> elm > 0 ).count() );
        Assert.isEqual( 0, sequence.filter( elm -> elm < 0 ).count() );
    }


    protected String throwException() throws IOException {
        throw new IOException( "!!!" );
    }


    protected String transform( Integer i ) throws IOException {
        return String.valueOf( i );
    }

    protected static int sum( int a, int b ) {
        return a + b;
    }
}
