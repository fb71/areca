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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import org.apache.commons.lang3.StringUtils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
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
            Set<Element> annotatedElements = new HashSet<>();
            Set<TypeElement> processedAnnotations = new HashSet<>();

            // create AnnotationInfos
            for (TypeElement annotation : annotations) {
                if (!annotation.getQualifiedName().toString().startsWith( "java." )) {
                    createAnnotationInfo( annotation );
                    processedAnnotations.add( annotation );
                    annotatedElements.addAll( roundEnv.getElementsAnnotatedWith( annotation ) );
                }
            }

            // create ClassInfos
            for (Element annotated : annotatedElements) {
                if (annotated instanceof TypeElement) {
                    createClassInfo( (TypeElement)annotated, processedAnnotations );
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
        return false;
    }


    protected void createClassInfo( TypeElement type, Set<TypeElement> processedAnnotations ) throws IOException {
        log( "Annotated class: ", type );

        String packageName = StringUtils.substringBeforeLast( type.getQualifiedName().toString(), "." );
        String typeName = type.getSimpleName() + "ClassInfo";

        // class
        Builder classBuilder = TypeSpec.classBuilder( typeName )
                .addModifiers( Modifier.PUBLIC, Modifier.FINAL )
                .superclass( ParameterizedTypeName.get( ClassName.get( ClassInfo.class ), ClassName.get( type ) ) );

        // INFO field
        ClassName className = ClassName.get( packageName, typeName );
        classBuilder.addField( FieldSpec.builder( className, "INFO", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL )
                .initializer( "new $L()", className )
                .build() );

        // name()
        classBuilder.addMethod( MethodSpec.methodBuilder( "name" )
                .addModifiers( Modifier.PUBLIC )
                .returns( String.class )
                .addStatement( "return $S", type.getSimpleName() )
                .build() );

        // type()
        classBuilder.addMethod( MethodSpec.methodBuilder( "type" )
                .addModifiers( Modifier.PUBLIC )
                .returns( ParameterizedTypeName.get( ClassName.get( Class.class ), ClassName.get( type ) ) )
                .addStatement( "return $L.class", type.getSimpleName() )
                .build() );

        // newInstance()
        classBuilder.addMethod( MethodSpec.methodBuilder( "newInstance" )
                .addModifiers( Modifier.PUBLIC )
                .returns( ClassName.get( type ) )
                .addException( InstantiationException.class )
                .addException( IllegalAccessException.class )
                .addStatement( "return new $T()", type )
                .build() );

        // createAnnotations()
        MethodSpec.Builder createAnnotations = MethodSpec.methodBuilder( "createAnnotations" )
                .addModifiers( Modifier.PROTECTED )
                .returns( ParameterizedTypeName.get( ClassName.get( List.class ), ClassName.get( AnnotationInfo.class ) ) )
                .addStatement( "List<AnnotationInfo> result = new $T<>()", ArrayList.class );
        for (AnnotationMirror am : processingEnv.getElementUtils().getAllAnnotationMirrors( type )) {
            if (processedAnnotations.contains( am.getAnnotationType().asElement() )) {
                createAnnotations.addCode( "result.add( " ).addCode( createAnnotation( am ) ).addCode( " );\n" );
            }
        }
        createAnnotations.addStatement( "return result" );
        classBuilder.addMethod( createAnnotations.build() );


        // methods
        MethodSpec.Builder createMethods = MethodSpec.methodBuilder( "createMethods" )
                .addModifiers( Modifier.PROTECTED )
                .returns( ParameterizedTypeName.get( ClassName.get( List.class ), ClassName.get( MethodInfo.class ) ) )
                .addStatement( "List<MethodInfo> result = new ArrayList<>()" );

        for (Element element : type.getEnclosedElements()) {
            if (element instanceof ExecutableElement) {
                ExecutableElement methodElm = (ExecutableElement)element;
                log( "    Method: ", methodElm.getSimpleName(), "() -> ", methodElm.getReturnType() );

                if (methodElm.getAnnotationMirrors().isEmpty()) {
                    log( "        no annotation.");
                    continue;
                }

                String methodName = methodElm.getSimpleName().toString() + "MethodInfo";
                MethodSpec.Builder m = MethodSpec.methodBuilder( methodName )
                        .addModifiers( Modifier.PUBLIC )
                        .returns( MethodInfo.class );
                m.addCode( "return new MethodInfo() {{\n" );
                m.addStatement( "  name = $S", methodElm.getSimpleName() );

                // annotations
                m.addCode( "  annotations = $T.asList(\n", Arrays.class );
                int c1 = 0;
                for (AnnotationMirror am : processingEnv.getElementUtils().getAllAnnotationMirrors( methodElm )) {
                    if (processedAnnotations.contains( am.getAnnotationType().asElement() )) {
                        m.addCode( c1++ > 0 ? ",\n" :  "" ).addCode( "    " + createAnnotation( am ) );
                    }
                }
                // invoke
                m.addCode( "  );\n  }\n" );
                m.addCode( "  public void invoke( Object obj, Object... params ) throws $T{\n", InvocationTargetException.class );
                m.addCode( "    try {\n" );
                m.addCode( "      (($T)obj).$L(", type, methodElm.getSimpleName() );
                int c2 = 0;
                for (VariableElement paramElement : methodElm.getParameters()) {
                    m.addCode( "($T)params[$L]", paramElement.asType(), c2++ );
                }
                m.addCode( ");\n", type, methodElm.getSimpleName() );
                m.addCode( "    } catch (Throwable e) {\n" );
                m.addCode( "      throw new $T( e );\n", InvocationTargetException.class );
                m.addCode( "    }\n" );
                m.addCode( "  }\n" );
                m.addCode( "};\n" );
                classBuilder.addMethod( m.build() );

                createMethods.addStatement( "result.add( $L() )", methodName );
            }
        }
        createMethods.addStatement( "return result" );
        classBuilder.addMethod( createMethods.build() );

        // file
        log( "package: " + packageName );
        JavaFile javaFile = JavaFile.builder( packageName, classBuilder.build() ).build();
        javaFile.writeTo( System.out );
        javaFile.writeTo( processingEnv.getFiler() );
    }


    protected CodeBlock createAnnotation( AnnotationMirror am ) {
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        codeBlock.add( "new $T() {{", ClassName.bestGuess( am.getAnnotationType().toString() + "AnnotationInfo" ) );
        for (Entry<? extends ExecutableElement,? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
            codeBlock.add( "this._$L = $L;", entry.getKey().getSimpleName(), entry.getValue() );
        }
        return codeBlock.add( "}}" ).build();
    }


    protected void createAnnotationInfo( TypeElement annotation ) throws IOException {
        log( "Annotation: ", annotation, " enclosing:" + annotation.getEnclosedElements() );

        String packageName = StringUtils.substringBeforeLast( annotation.getQualifiedName().toString(), "." );
        String typeName = annotation.getSimpleName() + "AnnotationInfo";

        // class
        Builder classBuilder = TypeSpec.classBuilder( typeName )
                .addModifiers( Modifier.PUBLIC )
                .addSuperinterface( annotation.asType() )
                .superclass( AnnotationInfo.class );

        // INFO field
        ClassName className = ClassName.get( packageName, typeName );
        classBuilder.addField( FieldSpec.builder( className, "INFO", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL )
                .initializer( "new $L()", className )
                .build() );

        // methods
        for (Element element : annotation.getEnclosedElements()) {
            if (element instanceof ExecutableElement) {
                ExecutableElement methodElm = (ExecutableElement)element;
                log( "    Method: ", methodElm.getSimpleName(), "() -> ", methodElm.getReturnType(), " : ", methodElm.getDefaultValue() );

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
            System.out.print( part != null ? part.toString() : "[null]" );
        }
        //Arrays.stream( parts ).forEach( part -> System.out.print( part.toString() ) );
        System.out.println();
    }

}
