package ru.sequoio.library.domain.graph;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Graph<T extends Node> {

    private final static Logger LOGGER = LoggerFactory.getLogger(Graph.class);

    private final Map<String, T> nameToNodeMap;
    private final TreeSet<Cluster<T>> orderedClusters;
    private final LinkedList<T> orderedNodes;

    public Graph(List<T> nodes) {
        this.nameToNodeMap = nodes.stream().collect(Collectors.toMap(Node::getName, Function.identity()));
        orderedClusters = new TreeSet<>();
        orderedNodes = buildGraphAndGetOrderedNodes();
    }

    private LinkedList<T> buildGraphAndGetOrderedNodes() {
        LOGGER.debug("Building migration graph");

        resolveNextNodesByRunAfterAndRunBeforeParamValues();
        makeClustersByExplicitNextNodes();
        var nodes = getOrderedNodesFromClusters();

        LOGGER.debug("Migration graph was built");
        return nodes;
    }

    private void resolveNextNodesByRunAfterAndRunBeforeParamValues() {
        LOGGER.debug("Resolving next nodes by RunAfter and RunBefore parameters");

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
        LOGGER.debug("Making clusters");

        getNodes().forEach(node -> {
           if (!node.isInCluster()) {
             orderedClusters.add(new Cluster<>(node));
           }
        });
    }

    private LinkedList<T> getOrderedNodesFromClusters() {
        LOGGER.debug("Traversing graph (breadth first)");

        LinkedList<T> resultingList = new LinkedList<>();
        for (var cluster : orderedClusters) {
            resultingList.addAll(cluster.getNodes());
        }
        return resultingList;
    }

    public Collection<T> getNodes() {
        return nameToNodeMap.values();
    }

    public LinkedList<T> getOrderedNodes() {
        return orderedNodes;
    }

}
