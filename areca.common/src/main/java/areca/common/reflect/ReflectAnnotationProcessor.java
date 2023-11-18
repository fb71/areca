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
import java.util.Collections;
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
import javax.lang.model.type.DeclaredType;
import org.apache.commons.lang3.StringUtils;

import com.squareup.javapoet.AnnotationSpec;
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

import areca.common.base.Opt;

/**
 *
 * @author Falko Bräutigam
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
public class ReflectAnnotationProcessor
        extends AbstractProcessor {


    @Override
    public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
        return new Run().process( annotations, roundEnv );
    }


    /**
     * Allow variables shared by all methods per run.
     */
    protected class Run {

        protected Set<Element> annotatedElements = new HashSet<>( 128 );

        protected Set<TypeElement> processedAnnotations = new HashSet<>( 128 );


        public boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv ) {
            //        log( "processingOver(): ", roundEnv.processingOver() );
            //        log( "errorRaised(): ", roundEnv.errorRaised() );
            //        log( "annotations:" + annotations.size() );

            try {
                // create AnnotationInfos
                for (TypeElement annotation : annotations) {
                    if (isInteresting( annotation )) {
                        checkCreateAnnotationInfo( annotation );
                        processedAnnotations.add( annotation );
                        annotatedElements.addAll( roundEnv.getElementsAnnotatedWith( annotation ) );
                    }
                }

                // create ClassInfos
                for (Element annotated : annotatedElements) {
                    if (annotated instanceof TypeElement) {
                        createClassInfos( (TypeElement)annotated );
                    }
                }
            }
            catch (IOException e) {
                throw new RuntimeException( e );
            }
            return true;
        }


        /**
         * Recursive for all super types.
         */
        protected void createClassInfos( TypeElement type ) throws IOException {
            DeclaredType superClass = (DeclaredType)type.getSuperclass();
            TypeElement superType = (TypeElement)superClass.asElement();

            // doing it recursively was the idea in the first place; but it generates to much and
            // I don't see a way to figure what is "interesting"; so I stopped this and have infos
            // for superclasses only if they are annotated by themselves; and those infos are
            // handled by the processor anyway

//            var interesting = Sequence.of( superClass.getAnnotationMirrors() )
//                    .filter( a -> isInteresting( (TypeElement)a.getAnnotationType().asElement() ) )
//                    .first();
//            log( "SUPER: ", type, " -> ", superType, " ANNOTATED: ", interesting, " -- ", superClass.getAnnotationMirrors() );
//            if (interesting.isPresent()) {
//                createClassInfos( superType );
//            }

            createClassInfo( type, superType );
        }


        protected void createClassInfo( TypeElement type, TypeElement superType ) throws IOException {
            log( ("=== " + type + " ==============================================").substring( 0, 68 ) );

            String packageName = StringUtils.substringBeforeLast( type.getQualifiedName().toString(), "." );
            String infoTypeName = type.getSimpleName() + "ClassInfo";
            ClassName rawTypeName = ClassName.get( type );

            // class
            Builder classBuilder = TypeSpec.classBuilder( infoTypeName )
                    .addModifiers( Modifier.PUBLIC, Modifier.FINAL )
                    .addAnnotation( AnnotationSpec.builder( SuppressWarnings.class ).addMember( "value", "\"all\"" ).build() )
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

            // superclassInfo()
            var classInfoTypeName = ParameterizedTypeName.get( ClassName.get( ClassInfo.class ), WildcardTypeName.subtypeOf( Object.class ) );
            String superPackageName = StringUtils.substringBeforeLast( superType.getQualifiedName().toString(), "." );
            String superInfoTypeName = superType.getSimpleName() + "ClassInfo";
            var superclassExists = processingEnv.getElementUtils().getTypeElement( superPackageName + "." + superInfoTypeName ) != null;
//            try {
//                Class.forName( superPackageName + "." + superInfoTypeName );
//                superclassExists = true;
//            }
//            catch (ClassNotFoundException e) {
//                log( "SUPER: " + e );
//            }
            classBuilder.addMethod( MethodSpec.methodBuilder( "superclassInfo" )
                    .addModifiers( Modifier.PUBLIC )
                    .returns( ParameterizedTypeName.get( ClassName.get( Opt.class ), classInfoTypeName ) )
                    // check if the class is already there or is generated by the current run
                    .addStatement( !superclassExists && !annotatedElements.contains( superType )
                            ? "return Opt.absent()"
                            : "return Opt.of( $L.instance() )", ClassName.get( superPackageName, superInfoTypeName ) )
                    .build() );

            // newInstance()
            var isAbstract = type.getModifiers().contains( Modifier.ABSTRACT );
            classBuilder.addMethod( MethodSpec.methodBuilder( "newInstance" )
                    .addModifiers( Modifier.PUBLIC )
                    .returns( ClassName.get( type ) )
                    .addException( InstantiationException.class )
                    .addException( IllegalAccessException.class )
                    .addStatement( isAbstract
                            ? "throw new InstantiationException( \"Class is abstract.\" )"
                            : "return new $T()", rawTypeName )
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

            outer:
            for (Element element : type.getEnclosedElements()) {
                if (element instanceof VariableElement) {
                    var modifiers = element.getModifiers();
                    VariableElement varElm = (VariableElement)element;
                    log( "    Field: ", varElm.getSimpleName(), " -> ", varElm.asType() );

                    if (varElm.getAnnotation( NoRuntimeInfo.class ) != null) {
                        log( "        skipped. (@NoRuntimeInfo) " );
                        continue outer;
                    }
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
                    if (modifiers.contains( Modifier.PRIVATE )) {
                        m.addCode( "    throw new RuntimeException( \"$T.$L is private!\" );\n", rawTypeName, varElm.getSimpleName() );
                    }
                    else if (modifiers.contains( Modifier.STATIC )) {
                        m.addCode( "    return $T.$L;\n", rawTypeName, varElm.getSimpleName() );
                    }
                    else {
                        m.addCode( "    return (($T)obj).$L;\n", rawTypeName, varElm.getSimpleName() );
                    }
                    m.addCode( "  }\n" );

                    // set
                    m.addCode( "  public void set( Object obj, Object value ) throws $T{\n", IllegalArgumentException.class );
                    if (modifiers.contains( Modifier.FINAL )) {
                        m.addCode( "    throw new RuntimeException( \"$T.$L is final!\" );\n", rawTypeName, varElm.getSimpleName() );
                    }
                    else if (modifiers.contains( Modifier.PRIVATE )) {
                        m.addCode( "    throw new RuntimeException( \"$T.$L is private!\" );\n", rawTypeName, varElm.getSimpleName() );
                    }
                    else if (modifiers.contains( Modifier.STATIC )) {
                        m.addCode( "    $T.$L = ($T)value;\n", rawTypeName, varElm.getSimpleName(), varElm.asType() );
                    }
                    else {
                        m.addCode( "    (($T)obj).$L = ($T)value;\n", rawTypeName, varElm.getSimpleName(), varElm.asType() );
                    }
                    m.addCode( "  }\n" );

                    m.addCode( "};\n" );
                    classBuilder.addMethod( m.build() );

                    createFields.addStatement( "result.add( $L() )", methodName );
                }
            }
            createFields.addStatement( "return result" );
            classBuilder.addMethod( createFields.build() );

            // methods
            MethodSpec.Builder createMethods = MethodSpec.methodBuilder( "createDeclaredMethods" )
                    .addModifiers( Modifier.PROTECTED )
                    .returns( ParameterizedTypeName.get( ClassName.get( List.class ), ClassName.get( MethodInfo.class ) ) )
                    .addStatement( "List<MethodInfo> result = new ArrayList<>()" );

            outer:
            for (Element element : type.getEnclosedElements()) {
                if (element instanceof ExecutableElement) {
                    ExecutableElement methodElm = (ExecutableElement)element;
                    log( "    Method: ", methodElm.getSimpleName(), "() -> ", methodElm.getReturnType() );

                    if (methodElm.getAnnotation( NoRuntimeInfo.class ) != null) {
                        log( "        skipped. (@NoRuntimeInfo) " );
                        continue outer;
                    }
                    if (methodElm.getSimpleName().toString().equals( "<init>" )) {
                        continue outer;
                    }
                    //                if (methodElm.getAnnotationMirrors().isEmpty()) {
                    //                    log( "        no annotation.");
                    //                    continue;
                    //                }

                    String methodName = methodElm.getSimpleName().toString() + "MethodInfo";
                    MethodSpec.Builder m = MethodSpec.methodBuilder( methodName )
                            .addModifiers( Modifier.PUBLIC )
                            .returns( MethodInfo.class );
                    m.addCode( "return new MethodInfo() {{\n" );
                    m.addStatement( "  name = $S", methodElm.getSimpleName() );

                    // annotations
                    var ams = processingEnv.getElementUtils().getAllAnnotationMirrors( methodElm );
                    if (ams.isEmpty()) {
                        m.addCode( "  annotations = $T.emptyList();\n", Collections.class );
                    }
                    else {
                        m.addCode( "  annotations = $T.asList(\n", Arrays.class );
                        int c1 = 0;
                        for (AnnotationMirror am : ams) {
                            if (processedAnnotations.contains( am.getAnnotationType().asElement() )) {
                                m.addCode( c1++ > 0 ? ",\n" : "" ).addCode( "    " + createAnnotation( am ) );
                            }
                        }
                        m.addCode( "  );\n" );
                    }
                    m.addCode( "  }\n" );

                    // invoke
                    var returnTypeName = TypeName.get( methodElm.getReturnType() );
                    m.addCode( "  public Object invoke( Object obj, Object... params ) throws $T{\n", InvocationTargetException.class );
                    m.addCode( "    try {\n" );
                    if (!returnTypeName.equals( TypeName.VOID )) {
                        m.addCode( "      return " );
                    }
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
                    m.addCode( ");\n" );
                    if (returnTypeName.equals( TypeName.VOID )) {
                        m.addCode( "      return null;\n" );
                    }
                    m.addCode( "    } catch (Throwable e) {\n" );
                    //m.addCode( "      throw (InvocationTargetException)e;\n" );
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


        private boolean isInteresting( TypeElement annotation ) {
            // XXX
            String qualifiedName = annotation.getQualifiedName().toString();
            return !qualifiedName.startsWith( "java." )
                    && !qualifiedName.startsWith( "javax." )
                    && !qualifiedName.startsWith( "org.teavm." );
        }


        private CodeBlock createAnnotation( AnnotationMirror am ) {
            CodeBlock.Builder codeBlock = CodeBlock.builder();
            codeBlock.add( "new $L()", infoElementClassName( (TypeElement)am.getAnnotationType().asElement(), true ) + "AnnotationInfo" );
            //codeBlock.add( "new $T()", ClassName.bestGuess( am.getAnnotationType().toString() + "AnnotationInfo" ) );
            Map<? extends ExecutableElement,? extends AnnotationValue> values = am.getElementValues();
            if (!values.isEmpty()) {
                codeBlock.add( " {{" );
                for (Entry<? extends ExecutableElement,? extends AnnotationValue> entry : values.entrySet()) {
                    String valueCode = entry.getValue().toString();
                    codeBlock.add( "this._$L = ", entry.getKey().getSimpleName() );
                    // XXX array type: check and handling are probable not meant to do this way
                    if (valueCode.startsWith( "{" )) {
                        String typeName = entry.getKey().asType().toString();
                        // in Maven there are those "()"; missing in Eclipse!?
                        if (typeName.startsWith( "()" )) {
                            typeName = typeName.substring( 2 );
                        }
                        codeBlock.add( "($L) new Object[] $L;", typeName, valueCode );
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

        /**
         * Creates the base classname for an info element for the given type.
         *
         * @param elm The element to create an info class for.
         * @return
         */
        private String infoElementClassName( TypeElement elm, boolean withPackage ) {
            ClassName className = ClassName.get( elm );
            StringBuilder result = new StringBuilder( 128 );
            if (withPackage) {
                result.append( className.packageName() ).append( "." );
            }
            for (var enclosing : className.simpleNames()) { // !!!
                result.append( enclosing );
            }
            return result.toString();

        }

        protected void checkCreateAnnotationInfo( TypeElement annotation ) throws IOException {
            log( ("=== " + annotation + " ===============================================================").substring( 0, 68 ) );
            //log( "Enclosing: " + annotation.getEnclosedElements() );

            var className = ClassName.get( annotation );
            var typeName = className.simpleNames().stream().reduce( (r,elm) -> r + "." + elm ).get();
            var infoTypeName = typeName.replace( ".", "" ) + "AnnotationInfo";
            var infoClassName = ClassName.get( className.packageName(), infoTypeName );

//            PackageElement pkg = processingEnv.getElementUtils().getPackageOf( annotation );
//            String packageName = pkg.getQualifiedName().toString();
//            String aName = annotation.getQualifiedName().toString().substring( packageName.length()+1 ); // maybe Page.Init
//            String typeName = aName.replace( ".", "" ) + "AnnotationInfo";
            log( "    -> ", className.packageName(), "..", infoTypeName );
            try {
                Class.forName( className.packageName() + "." + infoTypeName );
                log( "    already exists!" );  // imported from other jar
                return;
            }
            catch (ClassNotFoundException e) {
                // not found -> generating...
            }

            // class
            Builder classBuilder = TypeSpec.classBuilder( infoTypeName )
                    .addAnnotation( AnnotationSpec.builder( SuppressWarnings.class ).addMember( "value", "\"all\"" ).build() )
                    .addModifiers( Modifier.PUBLIC )
                    .addSuperinterface( annotation.asType() )
                    .superclass( AnnotationInfo.class );

            // INFO field
            classBuilder.addField( FieldSpec.builder( infoClassName, "INFO", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL )
                    .initializer( "new $L()", infoClassName )
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
                    .addStatement( "return " + typeName + ".class" )
                    .build() );

            // file
            JavaFile javaFile = JavaFile.builder( className.packageName(), classBuilder.build() ).build();
            //javaFile.writeTo( System.out );
            javaFile.writeTo( processingEnv.getFiler() );
        }
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
