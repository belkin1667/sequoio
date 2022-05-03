package ru.sequoio.library.domain.graph;

import java.util.ArrayList;
import java.util.List;

public abstract class Node implements Comparable<Node> {

    private final List<Node> nextNodes;
    private final List<Node> prevNodes;
    private final List<String> allNextNodeNames;

    private Integer naturalOrder;
    private String name;
    private boolean inCluster;
    private boolean root;
    private boolean notRoot;

    public Node(Integer naturalOrder, String name) {
        this.naturalOrder = naturalOrder;
        this.name = name;
        nextNodes = new ArrayList<>();
        prevNodes = new ArrayList<>();
        allNextNodeNames = new ArrayList<>();
        inCluster = false;
        root = false;
        notRoot = false;
    }

    public abstract List<String> getExplicitNextNodeNames();
    public abstract List<String> getExplicitPreviousNodeNames();

    public void addNextNode(Node node) {
        nextNodes.add(node);
    }

    public void addPreviousNode(Node node) {
        prevNodes.add(node);
    }

    public List<Node> getNextNodes() {
        return nextNodes;
    }

    public List<Node> getPreviousNodes() {
        return prevNodes;
    }

    public void addNextNodeName(String nodeName) {
        allNextNodeNames.add(nodeName);
    }

    public void addNextNodeNames(List<String> nodeNames) {
        allNextNodeNames.addAll(nodeNames);
    }

    public List<String> getAllNextNodeNames() {
        return allNextNodeNames;
    }

    public Integer getNaturalOrder() {
        return naturalOrder;
    }

    public void setNaturalOrder(int naturalOrder) {
        this.naturalOrder = naturalOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Node [naturalOrder=" + naturalOrder + ", name=" + name + "]";
    }

    public boolean isInCluster() {
        return inCluster;
    }

    public boolean isRoot() {
        return root;
    }

    public void markAsInCluster() {
        inCluster = true;
    }

    public void markAsRoot() {
        root = true;
    }

    public int compareTo(Node o) {
        return naturalOrder.compareTo(o.naturalOrder);
    }

    public void markAsNotRoot() {
        notRoot = true;
    }

    public boolean isNotRoot() {
        return notRoot;
    }
}
