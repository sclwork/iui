package com.scliang.annotation.compiler;

import static javax.tools.Diagnostic.Kind;

import com.google.auto.service.AutoService;
import com.scliang.annotations.CreateModel;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@AutoService(Processor.class)
public class IUiAnnotationProcessor extends AbstractProcessor {
    private Filer mFiler;
    private Messager mMessager;
    private Elements mElementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        mMessager = processingEnvironment.getMessager();
        mElementUtils = processingEnvironment.getElementUtils();
        mMessager.printMessage(Kind.NOTE, "IUiAnnotationProcessor init.");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        SourceVersion version = SourceVersion.latestSupported();
        mMessager.printMessage(Kind.NOTE, "IUiAnnotationProcessor getSupportedSourceVersion: " + version.name());
        return version;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(CreateModel.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        CreateModelAnnotation.create(mFiler, mMessager, mElementUtils, roundEnvironment);
        return true;
    }
}