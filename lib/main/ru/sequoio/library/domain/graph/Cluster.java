package ru.sequoio.library.domain.graph;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Cluster<T extends Node> implements Comparable<Cluster<T>> {

    private final LinkedList<T> nodes;
    private final Integer order;

    public Cluster(T node) {
        nodes = new LinkedList<>();
        var roots = getRoots(node).collect(Collectors.toList());
        //order = roots.stream().map(Node::getNaturalOrder).min(Integer::compareTo).orElse(-1);

        LinkedList<T> queue = new LinkedList<>(roots);
        while (!queue.isEmpty()) {
            T n = queue.poll();
            if (n.isInCluster()) {
                throw new IllegalStateException("Cycle found in migration graph!");
            }
            addToCluster(n);
            queue.addAll((Collection<? extends T>) n.getNextNodes());
        }
        order = nodes.stream().map(Node::getNaturalOrder).min(Integer::compareTo).orElse(-1);
    }

    private Stream<T> getRoots(T node) {
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
                    .flatMap(n -> getRoots((T) n));
        }
    }

    private void addToCluster(T node) {
        nodes.add(node);
        node.markAsInCluster();
    }

    public Integer getOrder() {
        return order;
    }

    public LinkedList<T> getNodes() {
        return nodes;
    }

    @Override
    public int compareTo(Cluster<T> o) {
        return this.getOrder().compareTo(o.getOrder());
    }
}
