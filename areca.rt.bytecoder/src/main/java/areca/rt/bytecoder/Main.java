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
package areca.rt.bytecoder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author falko
 */
public class Main {

    //private static final Logger LOG = Logger.getLogger( Main.class.getSimpleName() );

    public static void main( String[] args ) throws Exception {
        System.out.println( "Bytecoder! -- " + new Main() );

        log( "Hallo!", " (", 42, ")" );

//        for (Method m : Main.class.getMethods()) {
//            System.out.println( "" + m );
//        }

          for (int i=0; i<100; i++) {
              log( "[" + i + "] ..." );
              List<Flyweight> l = new LinkedList<>();
              for (int c=0; c<1000; c++) {
                  l.add( new Flyweight(c) );
              }
              System.gc();
              Thread.sleep( 1000 );
          }
    }


    /** */
    static class Flyweight {
        int n;

        public Flyweight( int n ) {
            this.n = n;
        }

        @Override
        protected void finalize() throws Throwable {
            log("finalize(): " + n);
        }
    }


    @Override
    public String toString() {
        return "Main []";
    }

    public static void log( Object... parts ) {
        Arrays.stream( parts ).forEach( part -> System.out.print( part.toString() ) );
        System.out.println();
    }
}
