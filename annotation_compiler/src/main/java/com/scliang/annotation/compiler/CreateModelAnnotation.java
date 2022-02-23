package com.scliang.annotation.compiler;

import com.scliang.annotations.CreateModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public final class CreateModelAnnotation {
    public static void create(Filer filer, Messager messager, Elements elementUtils, RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(CreateModel.class)) {
            if (element.getKind() == ElementKind.INTERFACE) {
                try {
                    CreateModel createModel = element.getAnnotation(CreateModel.class);
                    processCreateModel(filer, messager, elementUtils, element, createModel.baseUrl());
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void processCreateModel(Filer filer, Messager messager, Elements elementUtils, Element element, String baseUrl) throws ClassNotFoundException {
        if (element.getKind().isInterface()) {
            String packageName = elementUtils.getPackageOf(element).getQualifiedName().toString();
            String name = element.getSimpleName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "IUiAnnotationProcessor process: " + packageName + "/" + name);

            final ClassName iModel = ClassName.get("com.scliang.iui.base", "IModel");
            final ClassName iCallback = ClassName.get("com.scliang.iui.base", "ICallback");
            final ClassName dataUtils = ClassName.get("com.scliang.iui.utils", "DataUtils");

            List<MethodSpec> classMethods = new ArrayList<>();
            List<MethodSpec> interfaceMethods = new ArrayList<>();
            checkMethods(messager, elementUtils, (TypeElement) element, dataUtils, iCallback, classMethods, interfaceMethods);

            String iModelName = name.replaceFirst("Api", "") + "Model";
            createModelInterface(filer, packageName, iModel, interfaceMethods, iModelName);
            createModelClass(filer, baseUrl, packageName, name, dataUtils, classMethods, iModelName);
        }
    }

    private static void checkMethods(Messager messager, Elements elementUtils, TypeElement element, ClassName dataUtils, ClassName iCallback, List<MethodSpec> classMethods, List<MethodSpec> interfaceMethods) {
        List<? extends Element> mses = elementUtils.getAllMembers(element);
        for (Element m : mses) {
            if (m instanceof Symbol.MethodSymbol) {
                Symbol.MethodSymbol ms = (Symbol.MethodSymbol) m;
                ClassName call = ClassName.get("retrofit2", "Call");
                if (ms.getReturnType().toString().startsWith(call.toString())) {
                    messager.printMessage(Diagnostic.Kind.NOTE,
                            "IUiAnnotationProcessor members: " +
                                    ms.getReturnType().toString() + " " + ms.getQualifiedName());
                    Type.ClassType returnType = (Type.ClassType) ms.getReturnType();
                    List<Type> typeArgs = returnType.getTypeArguments();
                    if (typeArgs.size() != 1) continue;
                    ClassName callType = ClassName.bestGuess(typeArgs.get(0).toString());
                    TypeName returnTypeName = ParameterizedTypeName.get(call, callType);
                    messager.printMessage(Diagnostic.Kind.NOTE,
                            "IUiAnnotationProcessor returnType: " +
                                    returnType.getClass().toString());
                    List<ParameterSpec> params = new ArrayList<>();
                    List<Symbol.VarSymbol> ps = ms.getParameters();
                    for (Symbol.VarSymbol p : ps) {
                        messager.printMessage(Diagnostic.Kind.NOTE,
                                "IUiAnnotationProcessor members: " +
                                        p.asType().toString() + ":" + p.toString());
                        ParameterSpec param = ParameterSpec.builder(TypeVariableName.get(p.asType().toString()), p.toString()).build();
                        params.add(param);
                    }
                    Object[] pName = new Object[params.size() + 2];
                    pName[0] = returnTypeName;
                    pName[1] = ms.getQualifiedName();
                    StringBuilder statement = new StringBuilder();
                    statement.append("$T call = api.$N(");
                    for (int i = 2; i < pName.length; i++) {
                        statement.append(", $N");
                        ParameterSpec p = params.get(i - 2);
                        pName[i] = p.name;
                    }
                    statement.append(")");
                    String statementFormat = statement.toString().replaceFirst(", ", "");
                    messager.printMessage(Diagnostic.Kind.NOTE,
                            "IUiAnnotationProcessor statementFormat: " +
                                    statementFormat);
                    TypeName callbackTypeName = ParameterizedTypeName.get(iCallback, callType);
                    ParameterSpec param = ParameterSpec.builder(callbackTypeName, "callback").build();
                    params.add(param);
                    MethodSpec classMethod = MethodSpec.methodBuilder(ms.getQualifiedName().toString())
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .addParameters(params)
                            .addStatement(statementFormat, pName)
                            .addStatement("synchronized (this) { calls.add(call); }")
                            .addStatement("$T.post(call, callback)", dataUtils)
                            .build();
                    classMethods.add(classMethod);
                    MethodSpec interfaceMethod = MethodSpec.methodBuilder(ms.getQualifiedName().toString())
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameters(params)
                            .build();
                    interfaceMethods.add(interfaceMethod);
                }
            }
        }
    }

    private static void createModelInterface(Filer filer, String packageName, ClassName iModel, List<MethodSpec> interfaceMethods, String iModelName) {
        TypeSpec modelInterface = TypeSpec.interfaceBuilder(iModelName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(iModel)
                .addMethods(interfaceMethods)
                .build();
        try {
            JavaFile javaFile = JavaFile.builder(packageName, modelInterface)
                    .addFileComment("这段代码是自动生成的不要修改！")
                    .build();
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createModelClass(Filer filer, String baseUrl, String packageName, String name, ClassName dataUtils, List<MethodSpec> classMethods, String iModelName) {
        List<FieldSpec> fields = new ArrayList<>();

        FieldSpec api = FieldSpec.builder(ClassName.get(packageName, name), "api")
                .addModifiers(Modifier.PROTECTED)
                .build();
        fields.add(api);

        ClassName call = ClassName.get("retrofit2", "Call");
        ClassName list = ClassName.get("java.util", "List");
        ClassName arrayList = ClassName.get("java.util", "ArrayList");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, call);
        FieldSpec calls = FieldSpec.builder(listOfHoverboards, "calls")
                .addModifiers(Modifier.PRIVATE)
                .initializer("new $T<>()", arrayList)
                .build();
        fields.add(calls);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("api = $T.newApi($T.class, $S)", dataUtils, ClassName.get(packageName, name), baseUrl)
                .build();
        classMethods.add(0, constructor);

        MethodSpec cancelRequests = MethodSpec.methodBuilder("cancelRequests")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("synchronized (this) { for ($T call : calls) call.cancel(); calls.clear(); }", call)
                .build();
        classMethods.add(0, cancelRequests);

        String modelName = name.replaceFirst("I", "").replaceFirst("Api", "") + "Model";
        TypeSpec modelClass = TypeSpec.classBuilder(modelName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(packageName, iModelName))
                .addMethods(classMethods)
                .addFields(fields)
                .build();
        try {
            JavaFile javaFile = JavaFile.builder(packageName, modelClass)
                    .addFileComment("这段代码是自动生成的不要修改！")
                    .build();
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
