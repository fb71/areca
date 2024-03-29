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
package areca.common.testrunner;

import static areca.common.log.ConsoleColors.GREEN;
import static areca.common.log.ConsoleColors.RED_BRIGHT;
import static areca.common.log.ConsoleColors.RESET;
import static areca.common.log.ConsoleColors.YELLOW;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.util.logging.Logger;

import areca.common.Platform;
import areca.common.log.ConsoleColors;
import areca.common.reflect.ClassInfo;
import areca.common.reflect.RuntimeInfo;
import areca.common.testrunner.TestRunner.TestMethod;
import areca.common.testrunner.TestRunner.TestResult;
import areca.common.testrunner.TestRunner.TestStatus;

/**
 *
 * @author Falko Bräutigam
 */
@RuntimeInfo
public class LogDecorator
        extends TestRunnerDecorator {

    private static final Logger LOG = Logger.getLogger( LogDecorator.class.getSimpleName() );

    public static final int LINE_LENGHT = 80;

    public static final ClassInfo<LogDecorator> info = LogDecoratorClassInfo.instance();

    protected void println( String s ) {
        System.out.println( ConsoleColors.BLUE_BRIGHT + s + RESET );
    }

    @Override
    public void preRun( TestRunner runner ) {
        //println( "Running tests..." );
    }

    @Override
    public void postRun( TestRunner runner ) {
        //println( "done." );
    }

    @Override
    public void preTest( ClassInfo<?> test ) {
        println( rightPad( "===[" + test.name() + "]", LINE_LENGHT, '=' ) );
    }

    @Override
    public void preTestMethod( TestMethod m ) {
        println( rightPad( "---[" + m.name() + "]", LINE_LENGHT, '-' ) );
    }

    @Override
    public void postTestMethod( TestMethod m, TestResult testResult ) {
        if (testResult.getStatus() == TestStatus.PASSED) {
            println( GREEN + leftPad( "--| ok (" + testResult.elapsedTime() + ")", LINE_LENGHT, ' ' ) );
        }
        else if (testResult.getStatus() == TestStatus.SKIPPED) {
            println( YELLOW + leftPad( "--| skipped (" + testResult.elapsedTime() + ")", LINE_LENGHT, ' ' ) );
        }
        else {
            Throwable e = testResult.getException();
            if (e != null) {
                println( RED_BRIGHT + leftPad( "--| failed", LINE_LENGHT, ' ' ) );
                Throwable cause = Platform.rootCause( e );
                println( "Root cause: " + cause );
                if (Platform.isJVM()) {
                    //cause.printStackTrace( System.err );
                    var stack = cause.getStackTrace();
                    for (int i=0; i < stack.length && i < 25; i++) {
                        System.err.println( String.format( "    at %s", stack[i] ) );
                    }
                }
                throw (RuntimeException)cause;
            } else {
                println( RED_BRIGHT + leftPad( "--| failed (no exception)", LINE_LENGHT, ' ' ) );
            }
        }
    }

}
