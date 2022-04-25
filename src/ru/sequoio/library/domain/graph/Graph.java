package ru.sequoio.library.domain.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Graph<T extends Node> {

    private final Map<String, T> nameToNodeMap;
    private final List<Cluster<T>> clusters;
    private final TreeSet<Cluster<T>> orderedClusters;

    public Graph(List<T> nodes) {
        this.nameToNodeMap = nodes.stream().collect(Collectors.toMap(Node::getName, Function.identity()));
        clusters = new ArrayList<>();
        orderedClusters = new TreeSet<>();

        resolveNextNodesByRunAfterAndRunBeforeParamValues();
        makeClustersByExplicitNextNodes();
    }

    private void resolveNextNodesByRunAfterAndRunBeforeParamValues() {
        nameToNodeMap.values().forEach(migration -> {
            migration.getExplicitPreviousNodeNames().forEach(previousNodeName -> {
                nameToNodeMap.get(previousNodeName).addNextNodeName(migration.getName());
            });
            var nextNodeNames = migration.getExplicitNextNodeNames();
            migration.addNextNodeNames(nextNodeNames);
        });

        nameToNodeMap.values().forEach(migration -> migration.getAllNextNodeNames().forEach(
                        nextNodeName -> {
                            migration.addNextNode(nameToNodeMap.get(nextNodeName));
                            nameToNodeMap.get(nextNodeName).addPreviousNode(migration);
                        }
                )
        );
    }

    private void makeClustersByExplicitNextNodes() {
        getNodes().forEach(node -> {
           if (!node.isInCluster()) {
             orderedClusters.add(new Cluster<>(node));
           }
        });
    }

    public Map<String, T> getNameToNodeMap() {
        return nameToNodeMap;
    }

    public Collection<T> getNodes() {
        return nameToNodeMap.values();
    }

    public Set<String> getNodeNames() {
        return nameToNodeMap.keySet();
    }

    public LinkedList<Node> getOrderedNodes() {
        LinkedList<Node> resultingList = new LinkedList<>();
        for (var cluster : orderedClusters) {
            resultingList.addAll(cluster.getNodes());
        }
        return resultingList;
    }


}
