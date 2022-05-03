package ru.sequoio.library.domain.graph;

import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Cluster<T extends Node> implements Comparable<Cluster<T>> {

    private final LinkedList<Node> nodes;
    private final Integer order;

    public Cluster(T node) {
        nodes = new LinkedList<>();
        var roots = getRoots(node).collect(Collectors.toList());
        order = roots.stream().map(Node::getNaturalOrder).min(Integer::compareTo).orElse(-1);

        var queue = new LinkedList<>(roots);
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            if (n.isInCluster()) {
                //throw new IllegalStateException("Cycle found in migration graph!");
            }
            addToCluster(n);
            queue.addAll(n.getNextNodes());
        }
    }

    private Stream<Node> getRoots(Node node) {
        if (node.getPreviousNodes().isEmpty()) {
            return Stream.of(node);
        } else {
            node.markAsNotRoot();
            return node.getPreviousNodes().stream()
                    .peek(n -> {
                        if (n.isNotRoot()) {
                            throw new IllegalStateException("Cycle found in migration graph!");
                        }
                    })
                    .flatMap(this::getRoots);
        }
    }

    private void addToCluster(Node node) {
        nodes.add(node);
        node.markAsInCluster();
    }

    public Integer getOrder() {
        return order;
    }

    public LinkedList<Node> getNodes() {
        return nodes;
    }

    @Override
    public int compareTo(Cluster<T> o) {
        return this.getOrder().compareTo(o.getOrder());
    }
}
