/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.graph;

import org.gensokyo.data.generator.domain.FieldPO;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图形排序测试
 *
 * @author Gensokyo V.L.
 * @version 1.0.0
 * @since 2023/1/29 , Version 1.0.0
 */
class GraphTests {

    @Test
    void case1() {
        Graph<FieldPO, DefaultEdge> dag = new DirectedAcyclicGraph<>(DefaultEdge.class);
        List<FieldPO> fields = new ArrayList<>();
        fields.add(new FieldPO("E", List.of("B")));
        fields.add(new FieldPO("D", List.of("A", "B")));
        fields.add(new FieldPO("A", List.of()));
        fields.add(new FieldPO("B", List.of("A")));
        fields.add(new FieldPO("C", List.of("D", "B")));
        fields.add(new FieldPO("F", List.of("E")));
        Map<String, FieldPO> fieldMap = fields.stream()
                .collect(Collectors.toMap(FieldPO::getName, field -> field));
        for (FieldPO field : fields) {
            dag.addVertex(field);
            for (String fn : field.getDependsOn()) {
                FieldPO df = fieldMap.get(fn);
                dag.addVertex(df);
                dag.addEdge(df, field);
            }
        }

        dag.vertexSet().forEach(f -> System.out.println(f.getName()));
        System.out.println("--------------------------------------");

        TopologicalOrderIterator<FieldPO, DefaultEdge> it = new TopologicalOrderIterator<>(dag);
        it.forEachRemaining(f -> {
            System.out.println(f.getName());
            dag.edgesOf(f).forEach(e -> {
                System.out.println(e.toString());
            });
        });
        Assertions.assertTrue(true);
    }
}
