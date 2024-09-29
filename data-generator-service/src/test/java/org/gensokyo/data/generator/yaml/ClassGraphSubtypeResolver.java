/*
 * Copyright © 2024 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.yaml;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SubtypeResolver;
import com.github.victools.jsonschema.generator.TypeContext;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2024/7/11 , Version 1.0.0
 */
public class ClassGraphSubtypeResolver implements SubtypeResolver {

    private final ClassGraph classGraphConfig;
    private ScanResult scanResult;

    ClassGraphSubtypeResolver() {
        this.classGraphConfig = new ClassGraph()
                .enableClassInfo()
                .enableInterClassDependencies()
                // in this example, only consider a certain set of potential subtypes
                .acceptPackages("com.github.victools.jsonschema.examples");
    }

    private ScanResult getScanResult() {
        if (this.scanResult == null) {
            this.scanResult = this.classGraphConfig.scan();
        }
        return this.scanResult;
    }

    @Override
    public void resetAfterSchemaGenerationFinished() {
        if (this.scanResult != null) {
            this.scanResult.close();
            this.scanResult = null;
        }
    }

    @Override
    public List<ResolvedType> findSubtypes(ResolvedType declaredType, SchemaGenerationContext context) {
        if (declaredType.getErasedType() == Object.class) {
            return null;
        }
        ClassInfoList subtypes;
        if (declaredType.isInterface()) {
            subtypes = this.getScanResult().getClassesImplementing(declaredType.getErasedType());
        } else {
            subtypes = this.getScanResult().getSubclasses(declaredType.getErasedType());
        }
        if (!subtypes.isEmpty()) {
            TypeContext typeContext = context.getTypeContext();
            return subtypes.loadClasses(true)
                    .stream()
                    .map(subclass -> typeContext.resolveSubtype(declaredType, subclass))
                    .collect(Collectors.toList());
        }
        return null;
    }
}