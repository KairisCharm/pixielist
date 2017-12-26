package kairischarm.pixielist_processor;


//import android.util.Log;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;


import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;


import kairischarm.pixielist_annotation.BindMethod;
import kairischarm.pixielist_annotation.RecyclerView;
import kairischarm.pixielist_annotation.ViewHolderMethod;


public class RecyclerViewProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> set,
                           RoundEnvironment roundEnvironment) {
        // TODO processes the annotations


        Set<? extends Element> elementsToBind = roundEnvironment.getElementsAnnotatedWith(RecyclerView.class);
        Set<? extends Element> bindMethodElements = roundEnvironment.getElementsAnnotatedWith(BindMethod.class);
        Set<? extends Element> viewHolderMethodElements = roundEnvironment.getElementsAnnotatedWith(ViewHolderMethod.class);

        for (Element element : elementsToBind) {
            if(!(element instanceof TypeElement)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@RecyclerView can only be applied to a class");
                return true;
            }

            List<Element> bindingMethods = new ArrayList<>();
            List<Element> viewHolderMethods = new ArrayList<>();

            for(Element enclosedElement : element.getEnclosedElements())
            {
                if(bindMethodElements.contains(enclosedElement))
                    bindingMethods.add(enclosedElement);

                if(viewHolderMethodElements.contains(enclosedElement))
                    viewHolderMethods.add(enclosedElement);
            }

            RecyclerView annotation = element.getAnnotation(RecyclerView.class);
            String viewHolderLayoutId = annotation.viewHolderLayout();
            String dataClassName = null;

            List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
            for (AnnotationMirror mirror: mirrors) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> values = mirror.getElementValues();
                Set<? extends ExecutableElement> name2 = values.keySet();
                Iterator<? extends ExecutableElement> iterator = name2.iterator();
                iterator.next();
                while (iterator.hasNext()) {
                    ExecutableElement element2 = iterator.next();
                    AnnotationValue value = values.get(element2);
                    dataClassName = value.getValue().toString();
                }
            }

            String name = element.getSimpleName().toString();

            ClassName className = ClassName.get((TypeElement)element);

            String packageName = className.packageName();

            StringBuilder bindingPackageName = new StringBuilder();

            boolean resourcesFound;
            StringBuilder basePackage = new StringBuilder();
            basePackage.append(packageName);
            basePackage.append(".");
            do
            {
                basePackage = basePackage.delete(basePackage.lastIndexOf("."), basePackage.length());
                String checking = basePackage.toString() + ".R";
                resourcesFound = processingEnv.getElementUtils().
                        getTypeElement(checking) != null;


            } while(!resourcesFound && basePackage.toString().contains("."));

            if(!resourcesFound)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Cannot find layout resource " + viewHolderLayoutId);

            bindingPackageName.append(basePackage);

            Filer filer = processingEnv.getFiler();
            try
            {
                JavaFile.builder(packageName, generateRecyclerView(name, viewHolderLayoutId, bindingPackageName.toString(), dataClassName, bindingMethods, viewHolderMethods)).build().writeTo(filer);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(
                RecyclerView.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    private TypeSpec generateRecyclerView(String inName, String inViewHolderId, String inPackage, String inDataType, List<Element> inBindMethods, List<Element> inViewHolderMethods) {
        List<ParameterSpec> paramSpec = new ArrayList<>();
        ParameterSpec parmSpec = ParameterSpec.builder(Context.class, "inContext").build();
        paramSpec.add(parmSpec);
        parmSpec = ParameterSpec.builder(AttributeSet.class, "inAttr").build();
        paramSpec.add(parmSpec);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameters(paramSpec)
                .addModifiers(Modifier.PUBLIC)
                .addCode(CodeBlock.of("super(inContext, inAttr);\n\n" +
                        "mViewHolders = new java.util.ArrayList<>();\n\n" +
                        "setHasFixedSize(true);\n" +
                        "setLayoutManager(new android.support.v7.widget.LinearLayoutManager(inContext, VERTICAL, false));\n\n" +
                        "setAdapter(new Adapter());\n"))
                .build();

        List<MethodSpec> viewHolderMethods = new ArrayList<>();
        for(Element viewHolderMethod : inViewHolderMethods)
        {
            ExecutableElement method = (ExecutableElement)viewHolderMethod;
            TypeKind kind = method.getReturnType().getKind();

            TypeName type = TypeName.VOID;
            String returnStatment = "";

            switch (kind)
            {
                case BOOLEAN:
                    type = TypeName.BOOLEAN;
                    returnStatment += "return ";
                    break;
            }

            MethodSpec methodSpec = MethodSpec.methodBuilder(method.getSimpleName().toString())
                    .addParameter(int.class, "inIndex")
                    .addCode(CodeBlock.of(returnStatment + inName + "." + method.getSimpleName() + "(mViewHolders.get(inIndex));\n"))
                    .returns(type)
                    .addModifiers(Modifier.PUBLIC).build();

            viewHolderMethods.add(methodSpec);
        }

        ParameterizedTypeName dataItemsType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.bestGuess(inDataType).withoutAnnotations());
        ParameterizedTypeName viewHoldersType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.bestGuess(GetViewHolderClassName(inViewHolderId)));

        MethodSpec setItems = MethodSpec.methodBuilder("SetItems")
                .addParameter(dataItemsType, "inDataSet")
                .addModifiers(Modifier.PUBLIC)
                .addCode("mDataItems = inDataSet;\n\n" +
                        "getRecycledViewPool().clear();\n" +
                        "getAdapter().notifyDataSetChanged();\n").build();

        MethodSpec getCount = MethodSpec.methodBuilder("GetCount")
                .addModifiers(Modifier.PUBLIC)
                .addCode("return getAdapter().getItemCount();\n")
                .returns(int.class).build();

        FieldSpec data = FieldSpec.builder(dataItemsType, "mDataItems", Modifier.PRIVATE).build();
        FieldSpec viewHolders = FieldSpec.builder(viewHoldersType, "mViewHolders", Modifier.PRIVATE).build();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(inName + "RecyclerView")
                .superclass(android.support.v7.widget.RecyclerView.class)
                .addMethod(constructor)
                .addMethod(setItems)
                .addMethod(getCount)
                .addMethods(viewHolderMethods)
                .addType(generateViewHolder(inViewHolderId, inPackage))
                .addType(generateAdapter(inViewHolderId, inName, inBindMethods, inViewHolderMethods))
                .addModifiers(Modifier.PUBLIC)
                .addField(data)
                .addField(viewHolders);

        return classBuilder.build();
    }


    private TypeSpec generateViewHolder(String inViewHolderId, String inPackage)
    {
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameter(ParameterSpec
                        .builder(ClassName.bestGuess(GetBindingName(inViewHolderId, inPackage)), "inBinding")
                        .build())
                .addCode(CodeBlock.of("super(inBinding.getRoot());\n\n" +
                        "mBinding = inBinding;\n"))
                .addModifiers(Modifier.PRIVATE)
                .build();

        MethodSpec newInstance = MethodSpec.methodBuilder("NewInstance")
                .addParameter(ViewGroup.class, "inParent")
                .returns(ClassName.bestGuess(GetViewHolderClassName(inViewHolderId)))
                .addCode(GetBindingName(inViewHolderId, inPackage) + " binding = android.databinding.DataBindingUtil.inflate(android.view.LayoutInflater.from(inParent.getContext()), " + inPackage + "." + inViewHolderId + ", inParent, false);\n" +
                        "return new " + GetViewHolderClassName(inViewHolderId) + "(binding);\n")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC).build();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(GetViewHolderClassName(inViewHolderId))
                .superclass(android.support.v7.widget.RecyclerView.ViewHolder.class)
                .addField(FieldSpec.builder(ClassName.bestGuess(GetBindingName(inViewHolderId, inPackage)), "mBinding").build())
                .addMethod(constructor)
                .addMethod(newInstance)
                .addModifiers(Modifier.STATIC);

        return classBuilder.build();
    }


    private TypeSpec generateAdapter(String inViewHolderId, String inCallingClassName, List<Element> inBindMethods, List<Element> inViewHolderMethods)
    {
        final String dataItemsName = "mDataItems";
//        final String viewHolderName = GetViewHolderClassName(inViewHolderId);
        ClassName classViewHolder = ClassName.bestGuess("android.support.v7.widget.RecyclerView.ViewHolder");

        MethodSpec getItemCount = MethodSpec.methodBuilder("getItemCount")
                .addAnnotation(AnnotationSpec.builder(Override.class)
                        .build())
                .returns(int.class)
                .addCode("if(" + dataItemsName + " == null)\n    return 0;\n\nreturn " + dataItemsName + ".size();\n")
                .addModifiers(Modifier.PUBLIC)
                .build();

        StringBuilder bindViewHolderCode = new StringBuilder();

        for(Element bindingMethod : inBindMethods)
        {
            ExecutableElement method = (ExecutableElement)bindingMethod;

            bindViewHolderCode.append(inCallingClassName);
            bindViewHolderCode.append(".");
            bindViewHolderCode.append(method.getSimpleName());
            bindViewHolderCode.append("((")
                    .append(GetViewHolderClassName(inViewHolderId))
                    .append(")inViewHolder, mDataItems.get(inPosition));\n");
        }

        MethodSpec onBindViewHolder = MethodSpec.methodBuilder("onBindViewHolder")
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(classViewHolder, "inViewHolder").build())
                .addParameter(ParameterSpec.builder(int.class, "inPosition").build())
                .addCode(bindViewHolderCode.toString())
                .build();

        MethodSpec onCreateViewHolder = MethodSpec.methodBuilder("onCreateViewHolder")
                .addAnnotation(AnnotationSpec.builder(Override.class).build())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(ViewGroup.class, "inParent").build())
                .addParameter(ParameterSpec.builder(int.class, "inViewType").build())
                .returns(classViewHolder)
                .addCode(GetViewHolderClassName(inViewHolderId) + " viewHolder = " + GetViewHolderClassName(inViewHolderId) + ".NewInstance(inParent);\n" +
                        "mViewHolders.add(viewHolder);\n" +
                        "return viewHolder;\n").build();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("Adapter")
                .superclass(ParameterizedTypeName.get(ClassName.bestGuess("android.support.v7.widget.RecyclerView.Adapter"),
                        classViewHolder.withoutAnnotations()))
                .addMethod(getItemCount)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(onBindViewHolder)
                .addMethod(onCreateViewHolder);

        return classBuilder.build();
    }


    private String GetViewHolderClassName(String inViewHolderId)
    {
        StringBuilder result = new StringBuilder();

        String[] viewHolderIdSplit = inViewHolderId.split("\\.");

        String[] viewHolderNameSplit = viewHolderIdSplit[viewHolderIdSplit.length - 1].split("_");
        for(int i = 0; i < viewHolderNameSplit.length; i++)
        {
            String char1 = viewHolderNameSplit[i].substring(0, 1).toUpperCase();

            result.append(char1);
            result.append(viewHolderNameSplit[i].substring(1, viewHolderNameSplit[i].length()));
        }

        return result.toString();
    }


    private String GetBindingName(String inViewHolderId, String inPackage)
    {
        StringBuilder viewHolderBindingNameBuilder = new StringBuilder();

        viewHolderBindingNameBuilder.append(inPackage);
        viewHolderBindingNameBuilder.append(".databinding.");

        viewHolderBindingNameBuilder.append(GetViewHolderClassName(inViewHolderId));

        viewHolderBindingNameBuilder.append("Binding");

        return viewHolderBindingNameBuilder.toString();
    }
}