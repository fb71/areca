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
import java.util.Map;
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
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.squareup.javapoet.TypeVariableName;
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
                // XXX
                if (!annotation.getQualifiedName().toString().startsWith( "java." )
                        && !annotation.getQualifiedName().toString().startsWith( "org.teavm." )) {
                    checkCreateAnnotationInfo( annotation );
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
        log( ("=== " + type + " ==============================================").substring( 0, 68 ) );

        String packageName = StringUtils.substringBeforeLast( type.getQualifiedName().toString(), "." );
        String infoTypeName = type.getSimpleName() + "ClassInfo";
        ClassName rawTypeName = ClassName.get( type );

        // class
        Builder classBuilder = TypeSpec.classBuilder( infoTypeName )
                .addModifiers( Modifier.PUBLIC, Modifier.FINAL )
                //.addAnnotation( AnnotationSpec.builder( SuppressWarnings.class ).addMember( "value", "\"unchecked\"" ).build() )
                .superclass( ParameterizedTypeName.get( ClassName.get( ClassInfo.class ), rawTypeName ) );

        // INFO/instance field
        ClassName className = ClassName.get( packageName, infoTypeName );
        classBuilder.addField( FieldSpec.builder( className, "instance", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL )
                .initializer( "new $L()", className )
                .build() );

        // instance()
        classBuilder.addMethod( MethodSpec.methodBuilder( "instance" )
                .addModifiers( Modifier.PUBLIC, Modifier.STATIC )
                .returns( className )
                .addStatement( "return instance", type.getSimpleName() )
                .build() );

        // name()
        classBuilder.addMethod( MethodSpec.methodBuilder( "name" )
                .addModifiers( Modifier.PUBLIC )
                .returns( String.class )
                .addStatement( "return $S", type.getQualifiedName() )
                .build() );

        // simpleName()
        classBuilder.addMethod( MethodSpec.methodBuilder( "simpleName" )
                .addModifiers( Modifier.PUBLIC )
                .returns( String.class )
                .addStatement( "return $S", type.getSimpleName() )
                .build() );

        // type()
        classBuilder.addMethod( MethodSpec.methodBuilder( "type" )
                .addModifiers( Modifier.PUBLIC )
                .returns( ParameterizedTypeName.get( ClassName.get( Class.class ), rawTypeName ) )
                .addStatement( "return $L.class", type.getSimpleName() )
                .build() );

        // newInstance()
        classBuilder.addMethod( MethodSpec.methodBuilder( "newInstance" )
                .addModifiers( Modifier.PUBLIC )
                .returns( ClassName.get( type ) )
                .addException( InstantiationException.class )
                .addException( IllegalAccessException.class )
                .addStatement( "return new $T()", rawTypeName ) //.getSimpleName()
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

        // fields
        MethodSpec.Builder createFields = MethodSpec.methodBuilder( "createFields" )
                .addModifiers( Modifier.PROTECTED )
                .returns( ParameterizedTypeName.get( ClassName.get( List.class ), ClassName.get( FieldInfo.class ) ) )
                .addStatement( "List<FieldInfo> result = new ArrayList<>()" );

        for (Element element : type.getEnclosedElements()) { // XXX all vs. declared
            if (element instanceof VariableElement
                    && !element.getModifiers().contains( Modifier.STATIC )) {  // XXX
                VariableElement varElm = (VariableElement)element;
                log( "    Field: ", varElm.getSimpleName(), " -> ", varElm.asType() );

                String methodName = varElm.getSimpleName().toString() + "FieldInfo";
                MethodSpec.Builder m = MethodSpec.methodBuilder( methodName )
                        .addModifiers( Modifier.PUBLIC )
                        .returns( FieldInfo.class );
                m.addCode( "return new FieldInfo() {{\n" );
                m.addStatement( "  type = $T.class", rawTypeName( TypeName.get( varElm.asType() ) ) );
                m.addStatement( "  genericType = $L", createGenericType( TypeName.get( varElm.asType() ) ) );
                m.addStatement( "  name = $S", varElm.getSimpleName() );
                m.addStatement( "  declaringClassInfo = $L", ClassName.get( type ) + "ClassInfo.instance()" );

                TypeName genericTypeName = TypeName.get( varElm.asType() );
                if (genericTypeName instanceof ParameterizedTypeName) {
                    ParameterizedTypeName parameterized = (ParameterizedTypeName)genericTypeName;
                    log( "        raw: " +  parameterized.rawType );
                    log( "        param: " + genericTypeName + " -> " + parameterized.typeArguments );
                }

                // annotations
                m.addCode( "  annotations = $T.asList(\n", Arrays.class );
                int c1 = 0;
                for (AnnotationMirror am : processingEnv.getElementUtils().getAllAnnotationMirrors( varElm )) {
                    if (processedAnnotations.contains( am.getAnnotationType().asElement() )) {
                        m.addCode( c1++ > 0 ? ",\n" : "" ).addCode( "    " + createAnnotation( am ) );
                    }
                }
                m.addCode( "  );\n  }\n" );

                // get
                m.addCode( "  public Object get( Object obj ) throws $T{\n", IllegalArgumentException.class );
                m.addCode( "    return (($T)obj).$L;\n", rawTypeName, varElm.getSimpleName() );
                m.addCode( "  }\n" );
                // set
                m.addCode( "  public void set( Object obj, Object value ) throws $T{\n", IllegalArgumentException.class );
                m.addCode( "    (($T)obj).$L = ($T)value;\n", rawTypeName, varElm.getSimpleName(), varElm.asType() );
                m.addCode( "  }\n" );

                m.addCode( "};\n" );
                classBuilder.addMethod( m.build() );

                createFields.addStatement( "result.add( $L() )", methodName );
            }
        }
        createFields.addStatement( "return result" );
        classBuilder.addMethod( createFields.build() );

        // methods
        MethodSpec.Builder createMethods = MethodSpec.methodBuilder( "createMethods" )
                .addModifiers( Modifier.PROTECTED )
                .returns( ParameterizedTypeName.get( ClassName.get( List.class ), ClassName.get( MethodInfo.class ) ) )
                .addStatement( "List<MethodInfo> result = new ArrayList<>()" );

        for (Element element : type.getEnclosedElements()) { // XXX all vs. declared
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
                        m.addCode( c1++ > 0 ? ",\n" : "" ).addCode( "    " + createAnnotation( am ) );
                    }
                }
                m.addCode( "  );\n  }\n" );

                // invoke
                m.addCode( "  public void invoke( Object obj, Object... params ) throws $T{\n", InvocationTargetException.class );
                m.addCode( "    try {\n" );
                m.addCode( "      (($T)obj).$L(", rawTypeName, methodElm.getSimpleName() );
                int c2 = 0;
                for (VariableElement paramElement : methodElm.getParameters()) {
                    m.addCode( c2 > 0 ? "," : "" );
                    TypeName paramTypeName = TypeName.get( paramElement.asType() );
                    if (paramTypeName instanceof TypeVariableName) {
                        // XXX use Object
                    }
                    else {
                        m.addCode( "($T)", paramTypeName );
                    }
                    m.addCode( "params[$L]", c2++ );
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
        log( "    package: " + packageName );
        JavaFile javaFile = JavaFile.builder( packageName, classBuilder.build() ).build();
        //javaFile.writeTo( System.out );
        javaFile.writeTo( processingEnv.getFiler() );
    }


    private CodeBlock createAnnotation( AnnotationMirror am ) {
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        codeBlock.add( "new $T()", ClassName.bestGuess( am.getAnnotationType().toString() + "AnnotationInfo" ) );
        Map<? extends ExecutableElement,? extends AnnotationValue> values = am.getElementValues();
        if (!values.isEmpty()) {
            codeBlock.add( " {{" );
            for (Entry<? extends ExecutableElement,? extends AnnotationValue> entry : values.entrySet()) {
                String valueCode = entry.getValue().toString();
                codeBlock.add( "this._$L = ", entry.getKey().getSimpleName() );
                if (valueCode.startsWith( "{" )) {
                    // XXX array type: check and handling are probable not meant to do this way
                    codeBlock.add( "($L) new Object[] $L;", entry.getKey().asType().toString().substring( 2 ), valueCode );
                }
                else {
                    codeBlock.add( "$L;", valueCode );
                }
            }
            codeBlock.add( "}}" );
        }
        return codeBlock.build();
    }


    private CodeBlock createGenericType( TypeName typeName ) {
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        createGenericType( typeName, codeBlock );
        return codeBlock.build();
    }


    private void createGenericType( TypeName typeName, CodeBlock.Builder codeBlock ) {
        codeBlock = codeBlock != null ? codeBlock : CodeBlock.builder();
        if (typeName instanceof ClassName || typeName.isPrimitive()) {
            codeBlock.add( "new $T($T.class)", GenericType.ClassType.class, typeName );
        }
        else if (typeName instanceof ParameterizedTypeName) {
            createParameterizedType( (ParameterizedTypeName)typeName, codeBlock );
        }
        else {
            log( "createGenericType(): unhandled type: " + typeName.getClass().getName() );
            //throw new RuntimeException( "createGenericType(): unhandled type: " + typeName.getClass().getName() );
            codeBlock.add( "null" );
        }
    }


    private void createParameterizedType( ParameterizedTypeName typeName, CodeBlock.Builder codeBlock ) {
        codeBlock.add( "new $T($T.class,$L", GenericType.ParameterizedType.class, rawTypeName( typeName), "null" );
        codeBlock.add( ",new $T[] {", GenericType.class );
        int c = 0;
        for (TypeName argTypeName : typeName.typeArguments) {
            codeBlock.add( c++ == 0 ? "" : "," );
            createGenericType( argTypeName, codeBlock );
        }
        codeBlock.add( "})", Type.class );
    }


    private TypeName rawTypeName( TypeElement type ) {
        return rawTypeName( TypeName.get( type.asType() ) );
    }

    private TypeName rawTypeName( TypeName typeName ) {
        return (typeName instanceof ParameterizedTypeName)
                ? ((ParameterizedTypeName)typeName).rawType
                : typeName;
    }


    protected void checkCreateAnnotationInfo( TypeElement annotation ) throws IOException {
        log( ("=== " + annotation + " ==============================================").substring( 0, 68 ) );
        //log( "Enclosing: " + annotation.getEnclosedElements() );

        String packageName = StringUtils.substringBeforeLast( annotation.getQualifiedName().toString(), "." );
        String typeName = annotation.getSimpleName() + "AnnotationInfo";
        try {
            Class.forName( packageName + "." + typeName );
            log( "    already exists!" );  // imported from other jar
            return;
        }
        catch (ClassNotFoundException e) {
            // not found -> generating...
        }

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

                TypeName fieldTypeName = TypeName.get( methodElm.getReturnType() );
                String fieldName = "_"+methodElm.getSimpleName().toString();
                classBuilder.addField( FieldSpec.builder( fieldTypeName, fieldName, Modifier.PUBLIC )
                        .initializer( "$L", methodElm.getDefaultValue() )
                        .build() );
                classBuilder.addMethod( MethodSpec.methodBuilder( methodElm.getSimpleName().toString() )
                        .addModifiers( Modifier.PUBLIC )
                        .returns( fieldTypeName )
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
        log( "    package: " + packageName );
        JavaFile javaFile = JavaFile.builder( packageName, classBuilder.build() ).build();
        //javaFile.writeTo( System.out );
        javaFile.writeTo( processingEnv.getFiler() );
    }


    private static void log( Object... parts ) {
        System.out.print( "REFLECT: " );
        for (Object part : parts) {
            System.out.print( part != null ? part.toString() : "[null]" );
        }
        //Arrays.stream( parts ).forEach( part -> System.out.print( part.toString() ) );
        System.out.println();
    }

}
