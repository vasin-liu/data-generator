package org.gensokyo.data.calcite;

@FunctionalInterface
public interface TemplateV2SqlFunctionEvaluator {
    Object evaluate(TemplateV2SqlFunctionContext context);
}
