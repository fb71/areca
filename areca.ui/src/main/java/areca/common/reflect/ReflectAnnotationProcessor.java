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
package areca.common.reflect;

import java.util.Set;

import java.io.IOException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

/**
 *
 * @author Falko Bräutigam
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
public class ReflectAnnotationProcessor
        extends AbstractProcessor {

    private static int      round = 0;

    @Override
    public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
        System.out.println( "Processor: " + round + " -- " + roundEnv.toString() );
        if (round++ > 0) {
            return false;
        }

        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith( annotation );
            System.out.println( "Annotated: " + annotated );
        }

        MethodSpec main = MethodSpec.methodBuilder( "main" )
                .addModifiers( Modifier.PUBLIC, Modifier.STATIC )
                .returns( void.class )
                .addParameter( String[].class, "args" )
                .addStatement( "$T.out.println($S)", System.class, "Hello, JavaPoet!" )
                .build();

        TypeSpec helloWorld = TypeSpec.classBuilder( "HelloWorld" )
                .addModifiers( Modifier.PUBLIC, Modifier.FINAL )
                .addMethod( main )
                .build();

        JavaFile javaFile = JavaFile.builder( "com.example.helloworld", helloWorld )
                .build();

        try {
            javaFile.writeTo( processingEnv.getFiler() );
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
        return false;
    }

}
