/*
 * Copyright © 2021 PCI Technology Group Co.,Ltd. All Rights Reserved.
 * Site: http://www.pcitech.com/
 * Address：PCI Intelligent Building, No.2 Xincen Fourth Road, Tianhe District, Guangzhou，China（Zip code：510653）
 */
package org.gensokyo.data.generator.graph;

import org.gensokyo.data.model.vo.FieldVO;
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
        Graph<FieldVO, DefaultEdge> dag = new DirectedAcyclicGraph<>(DefaultEdge.class);
        List<FieldVO> fields = new ArrayList<>();
        fields.add(new FieldVO("E", List.of("B")));
        fields.add(new FieldVO("D", List.of("A", "B")));
        fields.add(new FieldVO("A", List.of()));
        fields.add(new FieldVO("B", List.of("A")));
        fields.add(new FieldVO("C", List.of("D", "B")));
        fields.add(new FieldVO("F", List.of("E")));
        Map<String, FieldVO> fieldMap = fields.stream()
                .collect(Collectors.toMap(FieldVO::getName, field -> field));
        for (FieldVO field : fields) {
            dag.addVertex(field);
            for (String fn : field.getDependsOn()) {
                FieldVO df = fieldMap.get(fn);
                dag.addVertex(df);
                dag.addEdge(df, field);
            }
        }

        dag.vertexSet().forEach(f -> System.out.println(f.getName()));
        System.out.println("--------------------------------------");

        TopologicalOrderIterator<FieldVO, DefaultEdge> it = new TopologicalOrderIterator<>(dag);
        it.forEachRemaining(f -> {
            System.out.println(f.getName());
            dag.edgesOf(f).forEach(e -> {
                System.out.println(e.toString());
            });
        });
        Assertions.assertTrue(true);
    }
}
