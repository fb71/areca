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
package areca.rt.teavm.testapp;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Falko Bräutigam
 */
public class Main2 {

//    @Retention(RetentionPolicy.RUNTIME)
//    @Target({ElementType.METHOD, ElementType.FIELD})
//    public @interface Test {
//    }

    public static class Test {
        public void doSomething() {};
    }

    public static interface MethodInfo {
        public void invoke( Object obj );
    }

    public static interface ClassInfo<T> {
        public List<MethodInfo> methods();
    }

    public static class TestClassInfo implements ClassInfo<Test> {

        public static final TestClassInfo INFO = new TestClassInfo();

        public MethodInfo doSomethingInfo() {
            return obj -> ((Test)obj).doSomething();
        }

        @Override
        public List<MethodInfo> methods() {
            return Arrays.asList( doSomethingInfo() );
        }
    }

    /**
     *
     */
    public static void main( String[] args ) {
        Test test = new Test();
        TestClassInfo.INFO.methods().get( 0 ).invoke( test );
    }

}
