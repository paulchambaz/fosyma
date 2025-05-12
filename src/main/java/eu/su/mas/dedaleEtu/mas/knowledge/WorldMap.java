package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.tuple.Couple;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.Set;

import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import dataStructures.serializableGraph.SerializableNode;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class WorldMap implements Serializable {
  private static final long serialVersionUID = -8761824239823746289L;

  private final Brain brain;

  private Graph worldGraph;
  private int edgeCounter;
  private Map<String, MapAttribute> nodeAttributes;

  private SerializableSimpleGraph<String, MapAttribute> serializableGraph;

  public WorldMap(Brain brain) {
    this.brain = brain;
    this.worldGraph = new SingleGraph("world");
    this.edgeCounter = 0;
    this.nodeAttributes = new HashMap<>();
    this.serializableGraph = null;
  }

  public synchronized Graph getGraph() {
    return this.worldGraph;
  }

  public synchronized Map<String, MapAttribute> getNodeAttributes() {
    return this.nodeAttributes;
  }

  public synchronized MapAttribute getNodeAttribute(String nodeId) {
    return nodeAttributes.get(nodeId);
  }

  public synchronized void addNode(String id, MapAttribute mapAttribute) {
    this.nodeAttributes.put(id, mapAttribute);

    if (this.worldGraph.getNode(id) == null) {
      this.worldGraph.addNode(id);
    }

    brain.notifyVisualization();
  }

  public synchronized boolean addNewNode(String id) {
    if (!this.nodeAttributes.containsKey(id)) {
      addNode(id, MapAttribute.OPEN);
      return true;
    }
    return false;
  }

  public synchronized void addEdge(String idNode1, String idNode2) {
    if (!this.nodeAttributes.containsKey(idNode1)) {
      addNode(idNode1, MapAttribute.OPEN);
    }
    if (!this.nodeAttributes.containsKey(idNode2)) {
      addNode(idNode2, MapAttribute.OPEN);
    }
    this.edgeCounter++;

    try {
      this.worldGraph.addEdge(String.valueOf(this.edgeCounter), idNode1, idNode2);
    } catch (EdgeRejectedException e) {
      this.edgeCounter--;
    }
  }

  public synchronized Graph createNavigableGraph(List<String> occupiedPositions) {
    Graph tempGraph = new SingleGraph("Temporary graph");

    this.nodeAttributes.keySet().stream()
        .filter(id -> !occupiedPositions.contains(id))
        .forEach(tempGraph::addNode);

    this.worldGraph.edges().forEach(edge -> {
      String sourceId = edge.getSourceNode().getId();
      String targetId = edge.getTargetNode().getId();

      if (tempGraph.getNode(sourceId) != null && tempGraph.getNode(targetId) != null) {
        try {
          tempGraph.addEdge(edge.getId(), sourceId, targetId);
        } catch (Exception e) {
        }
      }
    });

    return tempGraph;
  }

  public synchronized List<String> getAdjacentNodes(String nodeId) {
    Set<String> neighborhood = getNeighborhood(nodeId);
    neighborhood.remove(nodeId);
    return new ArrayList<>(neighborhood);
  }

  public synchronized Set<String> getNeighborhood(String position) {
    Set<String> neighborhood = new HashSet<>();

    neighborhood.add(position);

    Node node = this.worldGraph.getNode(position);
    if (node != null) {
      node.edges().forEach(edge -> {
        Node oppositeNode = edge.getOpposite(node);
        neighborhood.add(oppositeNode.getId());
      });
    }

    return neighborhood;
  }

  public synchronized List<String> findShortestPath(String idFrom, String idTo, List<String> occupiedPositions) {
    List<String> filteredOccupiedPositions = new ArrayList<>(occupiedPositions);

    filteredOccupiedPositions.remove(idFrom);
    filteredOccupiedPositions.remove(idTo);

    Graph tempGraph = createNavigableGraph(filteredOccupiedPositions);
    Node from = tempGraph.getNode(idFrom);
    if (from == null) {
      return null;
    }

    Dijkstra dijkstra = new Dijkstra();
    dijkstra.init(tempGraph);
    dijkstra.setSource(from);
    dijkstra.compute();

    List<String> shortestPath = new ArrayList<>();
    try {
      for (Node node : dijkstra.getPathNodes(tempGraph.getNode(idTo))) {
        shortestPath.add(node.getId());
      }
    } catch (Exception e) {
      return null;
    }

    dijkstra.clear();
    if (shortestPath.isEmpty()) {
      return null;
    }

    shortestPath = shortestPath.reversed();

    shortestPath.remove(0);
    return shortestPath;
  }

  public synchronized List<String> getOpenNodes() {
    return this.nodeAttributes.entrySet().stream()
        .filter(entry -> entry.getValue() == MapAttribute.OPEN)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  public synchronized List<String> getNodes() {
    return this.nodeAttributes.entrySet().stream()
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  public synchronized boolean hasOpenNode() {
    return this.nodeAttributes.entrySet().stream()
        .anyMatch(entry -> entry.getValue() == MapAttribute.OPEN);
  }

  public synchronized String findRandomNode(List<String> occupiedPositions) {
    Graph navigableGraph = createNavigableGraph(occupiedPositions);

    List<String> availableNodes = new ArrayList<>();
    navigableGraph.nodes().forEach(node -> {
      availableNodes.add(node.getId());
    });

    Random random = new Random();
    int randomIndex = random.nextInt(availableNodes.size());
    return availableNodes.get(randomIndex);
  }

  public synchronized String findClosestOpenNode(String startPosition, List<String> occupiedPositions) {
    return getOpenNodes().stream()
        .map(node -> {
          List<String> path = findShortestPath(startPosition, node, occupiedPositions);
          int distance = (path != null) ? path.size() : Integer.MAX_VALUE;
          return new Couple<>(node, distance);
        })
        .min(Comparator.comparing(pair -> pair.getRight()))
        .map(pair -> pair.getLeft())
        .orElse(null);
  }

  public synchronized String findClosestNode(String startPosition, List<String> nodes, List<String> occupiedPositions) {
    return nodes.stream()
        .map(node -> {
          List<String> path = findShortestPath(startPosition, node, occupiedPositions);
          int distance = (path != null) ? path.size() : Integer.MAX_VALUE;
          return new Couple<>(node, distance);
        })
        .min(Comparator.comparing(pair -> pair.getRight()))
        .map(pair -> pair.getLeft())
        .orElse(null);
  }

  public synchronized List<String> findPathToClosestOpenNode(String startPosition, List<String> occupiedPositions) {
    List<String> openNodes = getOpenNodes();

    return openNodes.stream()
        .map(currentNode -> findShortestPath(startPosition, currentNode, occupiedPositions))
        .filter(path -> path != null)
        .min(Comparator.comparing(List::size))
        .orElse(null);
  }

  public synchronized boolean hasUnexploredNodes() {
    return this.nodeAttributes.values().stream()
        .anyMatch(attr -> attr == MapAttribute.OPEN);
  }

  public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializableGraph() {
    serializeTopology();
    return this.serializableGraph;
  }

  public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializedSubgraphFromNodes(
      List<String> nodesToKeep) {

    Set<String> allNodes = new HashSet<>();
    for (Map.Entry<String, MapAttribute> entry : this.nodeAttributes.entrySet()) {
      allNodes.add(entry.getKey());
    }

    Set<String> initiallyMarkedForRemoval = new HashSet<>(allNodes);
    initiallyMarkedForRemoval.removeAll(new HashSet<>(nodesToKeep));

    Set<String> cannotRemove = new HashSet<>();
    this.worldGraph.edges().forEach(currentEdge -> {
      String sourceId = currentEdge.getSourceNode().getId();
      String targetId = currentEdge.getTargetNode().getId();

      boolean sourceInKeep = nodesToKeep.contains(sourceId);
      boolean targetInKeep = nodesToKeep.contains(targetId);

      if (sourceInKeep && initiallyMarkedForRemoval.contains(targetId)) {
        cannotRemove.add(targetId);
      }
      if (targetInKeep && initiallyMarkedForRemoval.contains(sourceId)) {
        cannotRemove.add(sourceId);
      }
    });

    Set<String> finalNodesToKeep = new HashSet<>(nodesToKeep);
    finalNodesToKeep.addAll(cannotRemove);

    SerializableSimpleGraph<String, MapAttribute> serializableSubgraph = new SerializableSimpleGraph<>();

    for (Map.Entry<String, MapAttribute> entry : this.nodeAttributes.entrySet()) {
      if (finalNodesToKeep.contains(entry.getKey())) {
        serializableSubgraph.addNode(entry.getKey(), entry.getValue());
      }
    }

    this.worldGraph.edges().forEach(currentEdge -> {
      String sourceId = currentEdge.getSourceNode().getId();
      String targetId = currentEdge.getTargetNode().getId();

      if (finalNodesToKeep.contains(sourceId) && finalNodesToKeep.contains(targetId)) {
        serializableSubgraph.addEdge(currentEdge.getId(), sourceId, targetId);
      }
    });

    return serializableSubgraph;
  }

  public synchronized void mergeWithReceivedMap(SerializableSimpleGraph<String, MapAttribute> receivedGraph) {
    for (SerializableNode<String, MapAttribute> receivedNode : receivedGraph.getAllNodes()) {
      String nodeId = receivedNode.getNodeId();
      MapAttribute receivedAttribute = receivedNode.getNodeContent();

      if (this.worldGraph.getNode(nodeId) == null) {
        this.worldGraph.addNode(nodeId);
      }

      if (this.nodeAttributes.containsKey(nodeId)) {
        if (receivedAttribute == MapAttribute.CLOSED || this.nodeAttributes.get(nodeId) == MapAttribute.CLOSED) {
          this.nodeAttributes.put(nodeId, MapAttribute.CLOSED);
        } else {
          this.nodeAttributes.put(nodeId, receivedAttribute);
        }
      }
    }

    for (SerializableNode<String, MapAttribute> currentNode : receivedGraph.getAllNodes()) {
      for (String neighbor : receivedGraph.getEdges(currentNode.getNodeId())) {
        addEdge(currentNode.getNodeId(), neighbor);
      }
    }

    brain.notifyVisualization();
  }

  private void serializeTopology() {
    this.serializableGraph = new SerializableSimpleGraph<>();

    for (Map.Entry<String, MapAttribute> entry : this.nodeAttributes.entrySet()) {
      this.serializableGraph.addNode(entry.getKey(), entry.getValue());
    }

    this.worldGraph.edges().forEach(currentEdge -> {
      Node sourceNode = currentEdge.getSourceNode();
      Node targetNode = currentEdge.getTargetNode();
      this.serializableGraph.addEdge(currentEdge.getId(), sourceNode.getId(), targetNode.getId());
    });
  }

  private void deserializeTopology() {
    this.worldGraph = new SingleGraph("world");
    this.nodeAttributes.clear();

    for (SerializableNode<String, MapAttribute> currentNode : this.serializableGraph.getAllNodes()) {
      String nodeId = currentNode.getNodeId();
      MapAttribute attribute = currentNode.getNodeContent();

      this.worldGraph.addNode(nodeId);
      this.nodeAttributes.put(nodeId, attribute);
    }

    int newEdgeCounter = 0;
    for (SerializableNode<String, MapAttribute> currentNode : this.serializableGraph.getAllNodes()) {
      String nodeId = currentNode.getNodeId();

      for (String neighbor : this.serializableGraph.getEdges(nodeId)) {
        try {
          this.worldGraph.addEdge(String.valueOf(newEdgeCounter), nodeId, neighbor);
          newEdgeCounter++;
        } catch (Exception e) {
        }
      }
    }

    this.edgeCounter = newEdgeCounter;
  }

  public synchronized void beforeMove() {
    serializeTopology();
    this.worldGraph = null;
  }

  public synchronized void afterMove() {
    deserializeTopology();
    brain.notifyVisualization();
  }
}
