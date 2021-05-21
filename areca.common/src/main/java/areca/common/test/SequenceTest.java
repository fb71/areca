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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        MutableInt indices = new MutableInt();
        Sequence.of( "1", "2", "3" ).forEach( (elm,i) -> indices.add( i ) );
        Assert.isEqual( 3, indices.intValue() );
    }


    @Test
    public void concatTest() {
        Sequence<String,RuntimeException> s = Sequence.of( "1", "2" ).concat( Sequence.of( "4", "5" ) );
        Assert.isEqual( 4, s.count() );
        Assert.isEqual( "1245", s.reduce( String::concat ).get() );
        Assert.isEqual( 2, Sequence.of().concat( Sequence.of( "4", "5" ) ).count() );
        Assert.isEqual( 0, Sequence.of().concat( Sequence.of() ).count() );
        Assert.isEqual( "1234", Sequence.of( "1", "2" ).concat( "3", "4" ).reduce( String::concat ).get() );
    }


    @Test
    public void reduceTest() {
        Assert.isEqual( 6, Sequence.of( 1, 2, 3 ).reduce( (r,elm) -> r + elm ).get() );
        Assert.isEqual( 16, Sequence.of( 1, 2, 3 ).reduce( 10, SequenceTest::sum ) );

        Assert.isEqual( "123", Sequence.of( 1, 2, 3 ).reduce( "", (r,elm) -> r + elm ) );
        Assert.isEqual( "123", Sequence.of( "1", "2", "3" ).reduce( String::concat ).get() );
        Assert.isEqual( "123", Sequence.of( 1, 2, 3 ).reduce( new StringBuilder(), StringBuilder::append ).toString() );
    }


    @Test
    public void transformTest() {
        Sequence<Integer,RuntimeException> sequence = Sequence.of( 1, 2, 3 );
        Assert.isEqual( "234", sequence
                .transform( elm -> {LOG.info( ":"+elm ); return elm + 1; } )
                .transform( elm -> {LOG.info( "::"+elm ); return String.valueOf( elm ); } )
                .reduce( String::concat ).get() );

        Assert.that( sequence != sequence.transform( elm -> 0 ) );

        Assert.isEqual( "123", sequence.transform( String::valueOf ).reduce( String::concat ).get() );
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
        Assert.isEqual( 3, s1.size() );

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


    @Test
    public void seriesTest() {
        var s = Sequence.series( 0, n -> n + 1, n -> n < 10 );
        Assert.isEqual( 0, s.first().get() );
        Assert.isEqual( 10, s.count() );
        Assert.isEqual( 45, s.reduce( SequenceTest::sum ).get() );

        s = Sequence.series( 0, n -> n + 2, n -> n < 10 );
        Assert.isEqual( 0, s.first().get() );
        Assert.isEqual( 5, s.count() );
        Assert.isEqual( 20, s.reduce( SequenceTest::sum ).get() );

        Sequence.ofInts( 0, -1 ).forEach( n -> Assert.that( false ) );
    }


    @Test
    public void firstLastTest() {
        Assert.isEqual( 1, Sequence.of( 1, 2 ).first().get() );
        Assert.isEqual( 2, Sequence.of( 1, 2 ).last().get() );
        Assert.isEqual( 3, Sequence.of().last().orElse( 3 ) );
        Assert.isEqual( 3, Sequence.of().first().orElse( 3 ) );
        Assert.isEqual( 2, Sequence.of( 1, 2 ).first( n -> n>1).get() );
        Assert.isEqual( 1, Sequence.of( 1, 2 ).last( n -> n<2).get() );
    }

    @Test
    public void toListTest() {
        ArrayList<String> result = Sequence.of( 1, 1 ).map( elm -> elm.toString() ).toList();
        Assert.isEqual( "1", result.get( 0 ) );
        Assert.isEqual( "1", result.get( 1 ) );
    }


    @Test
    public void toMapTest() {
        Map<String,Integer> result = Sequence.of( 1, 2 ).toMap( elm -> elm.toString() );
        Assert.that( result instanceof HashMap );
        Assert.isEqual( 1, result.get( "1" ) );
        Assert.isEqual( 2, result.get( "2" ) );
    }


    @Test(expected = IllegalStateException.class)
    public void toMapFailTest() {
        Sequence.of( 1, 1 ).toMap( elm -> elm.toString() );
    }


    @Test(expected = IOException.class)
    public void asCollectionFailTest() throws IOException {
        Collection<String> result = Sequence.of( IOException.class, 1, 2, 3 )
                .map( elm -> throwException() )
                .asCollection();
        iterateCollection( result );
    }

    protected void iterateCollection( Collection<?> coll ) {
        coll.forEach( elm -> LOG.info( "" + elm ) );
    }


    @Test
    public void sequenceSeriesPerformanceTest() {
        for (int i=0; i<100; i++) {
            var result = Sequence.ofInts( 0, 99 )
                    .filter( elm -> elm < 10 )
                    .map( elm -> String.valueOf( elm ) )
                    .reduce( "", String::concat ) ;
            Assert.isEqual( "0123456789", result );
        }
    }


    @Test
    public void streamSeriesPerformanceTest() {
        for (int i=0; i<100; i++) {
            var result = Stream.iterate( 0, elm -> elm + 1 )
                    .limit( 100 )
                    .filter( elm -> elm < 10 )
                    .map( elm -> String.valueOf( elm ) )
                    .reduce( "", String::concat );
            Assert.isEqual( "0123456789", result );
        }
    }


    @Test
    public void loopSeriesPerformanceTest() {
        for (int i=0; i<100; i++) {
            var result = "";
            for (var elm=0; elm < 100; elm++) {
                if (elm < 10 ) {
                    result = result.concat( String.valueOf( elm ) );
                }
            }
            Assert.isEqual( "0123456789", result );
        }
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
