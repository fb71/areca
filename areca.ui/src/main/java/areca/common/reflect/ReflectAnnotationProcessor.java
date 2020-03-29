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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import org.apache.commons.lang3.StringUtils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.squareup.javapoet.WildcardTypeName;

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
        log( round, " -- ", roundEnv.toString() );
        if (round++ > 0) {
            return false;
        }

        try {
            // create Annotations
            for (TypeElement annotation : annotations) {
                if (!annotation.getQualifiedName().toString().startsWith( "java." )) {
                    createAnnotationInfo( annotation );

                    for (Element annotated : roundEnv.getElementsAnnotatedWith( annotation )) {
                        if (annotated instanceof TypeElement) {
                            createClassInfo( (TypeElement)annotated );
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
        return false;
    }


    protected void createClassInfo( TypeElement type ) {

    }


    protected void createAnnotationInfo( TypeElement annotation ) throws IOException {
        log( "Annotation: ", annotation, " enclosing:" + annotation.getEnclosedElements() );

        String packageName = StringUtils.substringBeforeLast( annotation.getQualifiedName().toString(), "." );
        String typeName = annotation.getSimpleName() + "AnnotationInfo";

        // class
        Builder classBuilder = TypeSpec.classBuilder( typeName )
                .addModifiers( Modifier.PUBLIC, Modifier.FINAL )
                .addSuperinterface( annotation.asType() )
                .superclass( AnnotationInfo.class );

        //public static final TestAnnotationInfo INFO = new TestAnnotationInfo();
        ClassName className = ClassName.get( packageName, typeName );
        classBuilder.addField( FieldSpec.builder( className, "INFO", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL )
                .initializer( "new $L()", className )
                .build() );

        // methods
        for (Element element : annotation.getEnclosedElements()) {
            if (element instanceof ExecutableElement) {
                ExecutableElement methodElm = (ExecutableElement)element;
                log( "    ", methodElm.getSimpleName(), " -> ", methodElm.getReturnType(), " : ", methodElm.getDefaultValue() );

                Type fieldType = String.class;
                String fieldName = "_"+methodElm.getSimpleName().toString();
                classBuilder.addField( FieldSpec.builder(fieldType, fieldName, Modifier.PUBLIC )
                        .initializer( "$L", methodElm.getDefaultValue() )
                        .build() );
                classBuilder.addMethod( MethodSpec.methodBuilder( methodElm.getSimpleName().toString() )
                        .addModifiers( Modifier.PUBLIC )
                        .returns( fieldType )  //methodElm.getReturnType(). )
                        .addStatement( "return $L", fieldName )
                        .build() );
            }
        }

        // annotationType() method
        classBuilder.addMethod( MethodSpec.methodBuilder( "annotationType" )
                .addModifiers( Modifier.PUBLIC )
                .returns( ParameterizedTypeName.get( ClassName.get( Class.class ), WildcardTypeName.subtypeOf( Annotation.class ) ) )
                .addStatement( "return " + annotation.getSimpleName() + ".class" )
                .build() );

        // file
        log( "package: " + packageName );
        JavaFile javaFile = JavaFile.builder( packageName, classBuilder.build() ).build();
        javaFile.writeTo( System.out );
        javaFile.writeTo( processingEnv.getFiler() );
    }


    protected static void log( Object... parts ) {
        System.out.print( "REFLECTION: " );
        for (Object part : parts) {
            System.out.print( part.toString() );
        }
        //Arrays.stream( parts ).forEach( part -> System.out.print( part.toString() ) );
        System.out.println();
    }

}
