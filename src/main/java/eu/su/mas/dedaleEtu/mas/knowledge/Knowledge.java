package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;

import java.io.Serializable;
import jade.core.Agent;
import eu.su.mas.dedale.env.Location;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import org.graphstream.ui.view.View;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.princ.Utils;

public class Knowledge implements Serializable {
  private static final long serialVersionUID = -1333959882640838272L;

  // agent static parameters
  private static Integer INTROVERT_LOCKDOWN_TIME = 16;

  // agent identity
  private String agent;
  private AgentData agentData;

  // agent desire
  private float desireExplore;
  private float desireCollect;

  // environment representation
  private Graph worldGraph;
  private Integer nbEdges;
  private SerializableSimpleGraph<String, MapAttribute> serializableGraph;

  // environment entities
  private Map<String, TreasureData> treasures;
  private Map<String, AgentData> agents;
  private SiloData silo;
  private GolemData golem;

  // agent state
  private Integer introvertCounter;
  private Integer blockCounter;

  private String goal;
  private Deque<String> goalPath;

  // visualizatoin reference
  private KnowledgeVisualization visualization;

  public Knowledge(String agent) {
    this.agent = agent;
    this.worldGraph = new SingleGraph("My world vision");
    this.nbEdges = 0;
    this.treasures = new HashMap<>();
    this.agents = new HashMap<>();
    this.silo = null;
    this.golem = null;
    this.introvertCounter = 0;
    this.blockCounter = 0;
    this.goal = "";
    this.goalPath = new ArrayDeque<>();
    this.desireExplore = 1;
    this.desireCollect = 0;
  }

  public synchronized void attachVisualization(KnowledgeVisualization visualization) {
    this.visualization = visualization;
    notifyVisualization();
  }

  public synchronized void observe(Agent agent) {
    String myPosition = ((AbstractDedaleAgent) agent).getCurrentPosition().getLocationId();
    updateAgentPosition(myPosition);

    String position = getPosition();

    List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) agent).observe();
    addNode(position, MapAttribute.closed);

    String nextNodeId = null;
    for (Couple<Location, List<Couple<Observation, String>>> entry : observations) {
      String accessibleNode = entry.getLeft().getLocationId();

      // add new node to map representation
      boolean isNewNode = addNewNode(accessibleNode);
      if (!position.equals(accessibleNode)) {
        addEdge(position, accessibleNode);
        if (nextNodeId == null && isNewNode) {
          nextNodeId = accessibleNode;
        }
      }

      // collect agent names
      for (Couple<Observation, String> observation : entry.getRight()) {
        Observation observeKind = observation.getLeft();
        String observed = observation.getRight();

        switch (observeKind) {
          case AGENTNAME:
            if (observed.startsWith("Silo")) {
              setSiloPosition(accessibleNode);
            } else if (observed.startsWith("Golem")) {
              setGolemPosition(accessibleNode);
            } else {
              updateAgentsPosition(observed, accessibleNode);
            }
            break;

          case GOLD:
          case DIAMOND:
            // TODO : update treasure creation to include all information
            System.out.println("Found treasure : " + observed);
            int treasureValue = Integer.parseInt(observed);
            addTreasure(accessibleNode, observeKind, treasureValue, -1, -1);
            break;

          default:
            assert false : "Unhandled observation type: " + observeKind;
        }
      }
    }
  }

  public synchronized float getDesireExplore() {
    return this.desireExplore;
  }

  public synchronized AgentData getAgentData() {
    return this.agentData;
  }

  public synchronized String getPosition() {
    return this.agentData.getPosition();
  }

  public synchronized String getGoal() {
    return this.goal;
  }

  public synchronized void setGoalPath() {
    List<String> path = getShortestPath(getPosition(), getGoal());
    if (path == null) {
      return;
    }
    this.goalPath = new ArrayDeque<>(path);
  }

  public synchronized void updateDesireExplore() {
    float convergence = (float) 0.99;
    float effect = (float) 0.5;
    if (wantsToCollect()) {
      this.desireCollect = Utils.lerp(this.desireCollect, 0, convergence);
      this.desireExplore = Utils.lerp(this.desireCollect, 1, convergence);

      // we have changed our state, we will exagerate this effect
      if (!wantsToCollect()) {
        this.desireCollect = Utils.lerp(this.desireCollect, 0, effect);
        this.desireExplore = Utils.lerp(this.desireCollect, 1, effect);
      }
    } else {
      this.desireExplore = Utils.lerp(this.desireCollect, 0, convergence);
      this.desireCollect = Utils.lerp(this.desireCollect, 1, convergence);

      // we have changed our state, we will exagerate this effect
      if (wantsToCollect()) {
        this.desireExplore = Utils.lerp(this.desireCollect, 0, effect);
        this.desireCollect = Utils.lerp(this.desireCollect, 1, effect);
      }
    }
  }

  public synchronized boolean wantsToCollect() {
    return this.desireExplore < this.desireCollect;
  }

  public synchronized Graph getGraph() {
    return this.worldGraph;
  }

  public synchronized void introvertReset() {
    this.introvertCounter = 0;
    notifyVisualization();
  }

  public synchronized void introvertSoftReset() {
    this.introvertCounter = INTROVERT_LOCKDOWN_TIME;
    notifyVisualization();
  }

  public synchronized void introvertRecovery() {
    this.introvertCounter += 1;
    notifyVisualization();
  }

  public synchronized boolean introvertCanTalk() {
    return this.introvertCounter > INTROVERT_LOCKDOWN_TIME;
  }

  public synchronized Integer getIntrovertCounter() {
    return introvertCounter;
  }

  public synchronized void addNode(String id, MapAttribute mapAttribute) {
    if (this.worldGraph.getNode(id) == null) {
      Node currentNode = this.worldGraph.addNode(id);
      currentNode.setAttribute("ui.class", mapAttribute.toString());
      currentNode.setAttribute("ui.label", id);
    } else {
      Node currentNode = this.worldGraph.getNode(id);
      currentNode.setAttribute("ui.class", mapAttribute.toString());
    }
    notifyVisualization();
  }

  // addNewNode attempts to add a new node to the graph with the open attribute.
  // Returns true if node was newly added, false if it already existed.
  public synchronized boolean addNewNode(String id) {
    if (this.worldGraph.getNode(id) == null) {
      addNode(id, MapAttribute.open);
      return true;
    }
    return false;
  }

  // addEdge creates a connection between two nodes identified by their IDs.
  // This represents a navigable path between locations in the environment.
  public synchronized void addEdge(String idNode1, String idNode2) {
    this.nbEdges++;
    try {
      this.worldGraph.addEdge(this.nbEdges.toString(), idNode1, idNode2);
      notifyVisualization();
    } catch (IdAlreadyInUseException e1) {
      System.exit(1);
    } catch (EdgeRejectedException e2) {
      this.nbEdges--;
    } catch (ElementNotFoundException e3) {
      System.err.println("Node not found: " + e3.getMessage());
    }
  }

  // addTreasure records the discovery of a treasure at a specific node, tracking
  // its type, quantity, lock and pick strength requirements. The node is visually
  // highlighted in yellow with the treasure type displayed.
  public synchronized void addTreasure(String nodeId, Observation type, int quantity, int lockStrength,
      int pickStrength) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData existing = this.treasures.get(nodeId);
      existing.setNodeId(nodeId);
      if (existing.getUpdateCounter() > 0) {
        existing.resetCounter();
        if (quantity > 0) {
          existing.setQuantity(quantity);
        }
      }
    } else {
      TreasureData treasure = new TreasureData(nodeId, type, quantity, quantity > 0, lockStrength, pickStrength);
      this.treasures.put(nodeId, treasure);
    }
    notifyVisualization();
  }

  // updateTreasureState updates whether a treasure at the specified node is open
  // (accessible). Used when agents successfully unlock a treasure chest.
  public synchronized void updateTreasureState(String nodeId, boolean isLocked) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData treasure = this.treasures.get(nodeId);
      treasure.setLocked(isLocked);
      treasure.resetCounter();
      notifyVisualization();
    }
  }

  // updateTreasureQuantity decreases the quantity of treasure at a node by the
  // specified amount.
  // Used when agents collect treasures from a location.
  public synchronized void updateTreasureQuantity(String nodeId, int quantityPicked) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData treasure = this.treasures.get(nodeId);
      treasure.decreaseQuantity(quantityPicked);
      treasure.resetCounter();
      notifyVisualization();
    }
  }

  // ageTreasureData ages all treasure data by incrementing their counters.
  // Should be called regularly to track data freshness.
  public synchronized void ageTreasureData() {
    for (TreasureData treasure : this.treasures.values()) {
      treasure.incrementCounter();
    }
    notifyVisualization();
  }

  // hasTreasure checks if a node contains any remaining treasure.
  // Returns true if treasure exists and quantity is greater than zero.
  public synchronized boolean hasTreasure(String nodeId) {
    return this.treasures.containsKey(nodeId) && this.treasures.get(nodeId).getQuantity() > 0;
  }

  // getTreasureData retrieves all information about a treasure at a specific
  // node.
  public synchronized TreasureData getTreasureData(String nodeId) {
    return this.treasures.get(nodeId);
  }

  // getNodesWithTreasureType finds all nodes containing a specific type of
  // treasure with quantity > 0.
  // Useful for targeting specific treasure types during exploration.
  public synchronized List<String> getNodesWithTreasureType(Observation type) {
    return this.treasures.entrySet().stream()
        .filter(entry -> entry.getValue().getType() == type && entry.getValue().getQuantity() > 0)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  public synchronized List<TreasureData> getFreashestTreasures() {
    return this.treasures.values().stream()
        .sorted(Comparator.comparing(TreasureData::getUpdateCounter))
        .collect(Collectors.toList());
  }

  public synchronized void updateAgentPosition(String nodeId) {
    if (this.agentData == null) {
      this.agentData = new AgentData(nodeId);
    } else {
      agentData.setPosition(nodeId);
    }
  }

  public synchronized void loseAgentPosition(String agentName) {
    if (this.agents.containsKey(agentName)) {
      AgentData agentData = this.agents.get(agentName);
      agentData.setPosition(null);
      notifyVisualization();
    }
  }

  // updateAgentPosition tracks the current position of an agent in the
  // environment.
  // Creates a new agent record if it doesn't exist or updates an existing one.
  public synchronized void updateAgentsPosition(String agentName, String nodeId) {
    if (!this.agents.containsKey(agentName)) {
      this.agents.put(agentName, new AgentData(nodeId));
    } else {
      AgentData agentData = this.agents.get(agentName);
      agentData.setPosition(nodeId);
      agentData.resetCounter();
    }
    notifyVisualization();
  }

  // updateAgentExpertise records the expertise levels of an agent for different
  // treasure types.
  // Expertise determines an agent's efficiency in handling specific treasures.
  public synchronized void updateAgentExpertise(String agentName, Map<Observation, Integer> expertise) {
    if (this.agents.containsKey(agentName)) {
      this.agents.get(agentName).setExpertise(expertise);
      notifyVisualization();
    }
  }

  // updateAgentBackpack updates information about an agent's carrying capacity
  // and available space.
  // Important for planning treasure collection strategies.
  public synchronized void updateAgentBackpack(String agentName, int capacity, int freeSpace) {
    if (this.agents.containsKey(agentName)) {
      AgentData data = this.agents.get(agentName);
      data.setBackpackCapacity(capacity);
      data.setBackpackFreeSpace(freeSpace);
      notifyVisualization();
    }
  }

  public synchronized void updateAgentStatus(String agentName, String status) {
    if (this.agents.containsKey(agentName)) {
      this.agents.get(agentName).setStatus(status);
      notifyVisualization();
    }
  }

  public synchronized void ageAgentData() {
    for (AgentData agent : this.agents.values()) {
      agent.incrementCounter();
    }
    notifyVisualization();
  }

  // getAgentData retrieves all information about a specific agent.
  public synchronized AgentData getAgentData(String agentName) {
    return this.agents.get(agentName);
  }

  // getAgentAtPosition finds all agents currently located at a specific node.
  // Useful for coordinating actions between agents in proximity.
  public synchronized String getAgentAtPosition(String nodeId) {
    return this.agents.entrySet().stream()
        .filter(entry -> nodeId.equals(entry.getValue().getPosition()))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  public synchronized List<String> getAgentsWithLockpickingStrength(int requiredStrength) {
    return this.agents.entrySet().stream()
        .filter(entry -> entry.getValue().canOpenLock(requiredStrength))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  public synchronized List<String> getAgentsWithCarryingStrength(int requiredStrength) {
    return this.agents.entrySet().stream()
        .filter(entry -> entry.getValue().canPickTreasure(requiredStrength))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  // setSiloPosition marks the location of a silo in the environment.
  // Silos are special locations where treasures can be deposited.
  // The node is visually highlighted in orange and labeled as "SILO".
  public synchronized void setSiloPosition(String nodeId) {
    if (silo == null) {
      this.silo = new SiloData(nodeId);
    } else {
      this.silo.setPosition(nodeId);
      this.silo.resetCounter();
    }
    notifyVisualization();
  }

  public synchronized void ageSiloData() {
    if (this.silo != null) {
      this.silo.incrementCounter();
      notifyVisualization();
    }
  }

  // getSiloPosition retrieves the location of the silo.
  public synchronized String getSiloPosition() {
    return (this.silo != null) ? this.silo.getPosition() : null;
  }

  public synchronized int getSiloUpdateCounter() {
    return (this.silo != null) ? this.silo.getUpdateCounter() : -1;
  }

  // getShortestPathToSilo calculates the most efficient path from a given
  // position to the silo. Important for optimizing treasure delivery.
  public synchronized List<String> getShortestPathToSilo(String currentPosition) {
    if (this.silo == null)
      return null;
    return getShortestPath(currentPosition, this.silo.getPosition());
  }

  public synchronized void setGolemPosition(String nodeId) {
    if (this.golem == null) {
      this.golem = new GolemData(nodeId);
    } else {
      this.golem.setPosition(nodeId);
      this.golem.resetCounter();
    }
    notifyVisualization();
  }

  public synchronized void ageGolemData() {
    if (this.golem != null) {
      this.golem.incrementCounter();
      notifyVisualization();
    }
  }

  public synchronized String getGolemPosition() {
    return (this.golem != null) ? this.golem.getPosition() : null;
  }

  public synchronized int getGolemUpdateCounter() {
    return (this.golem != null) ? this.golem.getUpdateCounter() : -1;
  }

  public synchronized boolean isGolemNearby(String nodeId, int maxDistance) {
    int MAX_AGE = 10;
    if (this.golem == null || this.golem.getUpdateCounter() > MAX_AGE) {
      return false;
    }

    List<String> path = getShortestPath(nodeId, this.golem.getPosition());
    if (path == null)
      return false;

    return path.size() <= maxDistance;
  }

  public synchronized Graph createTempGraph() {
    // TODO: we always DO WANT to include our current position in in the graph,
    // regardless of if we think it is occupied or not
    Graph tempGraph = new SingleGraph("Temporary path graph");
    this.worldGraph.nodes().forEach(node -> {
      String id = node.getId();
      boolean isOccupied = false;

      for (AgentData agent : this.agents.values()) {
        String position = agent.getPosition();
        if (position != null && position.equals(id)) {
          isOccupied = true;
          break;
        }
      }

      if (this.silo != null && id.equals(this.silo.getPosition())) {
        isOccupied = true;
      }
      if (this.golem != null && id.equals(this.golem.getPosition())) {
        isOccupied = true;
      }

      if (!isOccupied) {
        tempGraph.addNode(id);
      }
    });

    this.worldGraph.edges().forEach(edge -> {
      String sourceId = edge.getSourceNode().getId();
      String targetId = edge.getTargetNode().getId();

      if (tempGraph.getNode(sourceId) != null && tempGraph.getNode(targetId) != null) {
        try {
          tempGraph.addEdge(edge.getId(), sourceId, targetId);
        } catch (Exception e) {
          // edge already exists or nodes not found
        }
      }
    });

    return tempGraph;
  }

  // getShortestPath calculates the most efficient path between any two nodes.
  // Uses Dijkstra's algorithm to find the optimal route.
  public synchronized List<String> getShortestPath(String idFrom, String idTo) {
    if (idFrom == null || idTo == null) {
      return null;
    }

    Graph tempGraph = createTempGraph();
    Node from = tempGraph.getNode(idFrom);

    System.out.println("from: " + idFrom + ", to: " + idTo);

    if (from == null) {
      return null;
    }

    Dijkstra dijkstra = new Dijkstra();
    dijkstra.init(tempGraph);
    dijkstra.setSource(from);
    dijkstra.compute();

    List<Node> path;
    try {
      path = dijkstra.getPath(tempGraph.getNode(idTo)).getNodePath();
    } catch (Exception e) {
      return null;
    }

    List<String> shortestPath = new ArrayList<String>();
    for (Node entry : path) {
      shortestPath.add(entry.getId());
    }

    dijkstra.clear();

    if (shortestPath.isEmpty()) {
      return null;
    }

    shortestPath.remove(0);
    return shortestPath;
  }

  public synchronized String getClosestOpenNode() {
    System.out.println("Open nodes: " + getOpenNodes());
    return getOpenNodes().stream()
        .map(node -> {
          var path = getShortestPath(getPosition(), node);
          int distance = (path != null) ? path.size() : 99999;
          return new Couple<>(node, distance);
        })
        .min(Comparator.comparing(pair -> pair.getRight()))
        .map(pair -> pair.getLeft())
        .orElse(null);
  }

  // getShortestPathToClosestOpenNode finds the closest unexplored node.
  // Useful for efficient exploration of the environment.
  public List<String> getShortestPathToClosestOpenNode(String myPosition) {
    List<String> openNodes = getOpenNodes();

    return openNodes.stream()
        .map(currentNode -> getShortestPath(myPosition, currentNode))
        .filter(path -> path != null)
        .min(Comparator.comparing(List::size))
        .orElse(null);
  }

  // getOpenNodes retrieves all nodes marked as open (discovered but not fully
  // explored).
  // Helps prioritize exploration efforts.
  public List<String> getOpenNodes() {
    return this.worldGraph
        .nodes()
        .filter(currentNode -> "open".equals(currentNode.getAttribute("ui.class")))
        .map(Node::getId)
        .collect(Collectors.toList());
  }

  private void serializeGraphTopology() {
    this.serializableGraph = new SerializableSimpleGraph<>();

    Iterator<Node> nodeIterator = this.worldGraph.iterator();
    while (nodeIterator.hasNext()) {
      Node currentNode = nodeIterator.next();
      String nodeClass = (String) currentNode.getAttribute("ui.class");
      this.serializableGraph.addNode(
          currentNode.getId(),
          MapAttribute.valueOf(nodeClass));
    }

    Iterator<Edge> edgeIterator = this.worldGraph.edges().iterator();
    while (edgeIterator.hasNext()) {
      Edge currentEdge = edgeIterator.next();
      Node sourceNode = currentEdge.getSourceNode();
      Node targetNode = currentEdge.getTargetNode();
      this.serializableGraph.addEdge(currentEdge.getId(), sourceNode.getId(), targetNode.getId());
    }
  }

  // getSerializableGraph creates a serializable version of the map for
  // communication.
  // Enables sharing of map information between agents.
  public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializableGraph() {
    serializeGraphTopology();
    return this.serializableGraph;
  }

  public synchronized Map<String, TreasureData> getTreasures() {
    return this.treasures;
  }

  public synchronized Map<String, AgentData> getAgents() {
    return this.agents;
  }

  public synchronized SiloData getSilo() {
    return this.silo;
  }

  public synchronized GolemData getGolem() {
    return this.golem;
  }

  public synchronized Integer getBlockCounter() {
    return this.blockCounter;
  }

  public synchronized void bumpBlockCounter() {
    this.blockCounter += 1;
    notifyVisualization();
  }

  public synchronized Deque<String> getGoalPath() {
    return this.goalPath;
  }

  public synchronized void setGoal(String goal) {
    this.goal = goal;
  }

  public synchronized void updateGoal(String myPosition) {
    switch (this.goal) {
      case "SILO":
        this.goalPath = new ArrayDeque<>(getShortestPathToSilo(myPosition));
        break;

      case "TREASURE":
        this.goalPath = new ArrayDeque<>(treasures.entrySet().stream()
            .map(currentTreasure -> getShortestPath(myPosition, currentTreasure.getKey()))
            .filter(path -> path != null)
            .min(Comparator.comparing(List::size))
            .orElse(null));
        break;
      default:
        assert false : "Unhandled goal type";
    }

  }

  public synchronized SerializableKnowledge getSerializableKnowledge() {
    return new SerializableKnowledge(
        this.getSerializableGraph(),
        this.getTreasures(),
        this.getAgents(),
        this.getSilo(),
        this.getGolem());
  }

  // mergeMap integrates map information received from another agent.
  // Combines node and edge information while preserving the most updated state.
  public void mergeMap(SerializableSimpleGraph<String, MapAttribute> sgreceived) {
    for (SerializableNode<String, MapAttribute> currentNode : sgreceived.getAllNodes()) {
      boolean alreadyIn = false;
      Node newnode = null;

      try {
        newnode = this.worldGraph.addNode(currentNode.getNodeId());
      } catch (IdAlreadyInUseException e) {
        alreadyIn = true;
      }

      if (!alreadyIn) {
        newnode.setAttribute("ui.label", newnode.getId());
        newnode.setAttribute("ui.class", currentNode.getNodeContent().toString());
      } else {
        newnode = this.worldGraph.getNode(currentNode.getNodeId());
        if (((String) newnode.getAttribute("ui.class")) == MapAttribute.closed.toString()
            || currentNode.getNodeContent().toString() == MapAttribute.closed.toString()) {
          newnode.setAttribute("ui.class", MapAttribute.closed.toString());
        }
      }
    }

    for (SerializableNode<String, MapAttribute> currentNode : sgreceived.getAllNodes()) {
      for (String s : sgreceived.getEdges(currentNode.getNodeId())) {
        addEdge(currentNode.getNodeId(), s);
      }
    }

    notifyVisualization();
  }

  public void mergeTreasures(Map<String, TreasureData> treasures) {
    for (Map.Entry<String, TreasureData> entry : treasures.entrySet()) {
      String nodeId = entry.getKey();
      TreasureData receivedTreasure = entry.getValue();

      if (!treasures.containsKey(nodeId)
          || treasures.get(nodeId).getUpdateCounter() > receivedTreasure.getUpdateCounter()) {
        this.treasures.put(nodeId, receivedTreasure);
      }
    }

    notifyVisualization();
  }

  public void mergeAgents(Map<String, AgentData> agents) {
    for (Map.Entry<String, AgentData> entry : agents.entrySet()) {
      String agentName = entry.getKey();
      AgentData receivedAgent = entry.getValue();

      if (!agents.containsKey(agentName)
          || agents.get(agentName).getUpdateCounter() > receivedAgent.getUpdateCounter()) {
        this.agents.put(agentName, receivedAgent);
      }
    }

    notifyVisualization();
  }

  public void mergeSilo(SiloData silo) {
    if (silo == null) {
      return;
    }

    if (this.silo == null) {
      this.silo = silo;
    } else if (this.silo.getUpdateCounter() > silo.getUpdateCounter()) {
      this.silo.copy(silo);
    }

    notifyVisualization();
  }

  public void mergeGolem(GolemData golem) {
    if (golem == null) {
      return;
    }

    if (this.golem == null) {
      this.golem = golem;
    } else if (this.golem.getUpdateCounter() > golem.getUpdateCounter()) {
      this.golem.copy(golem);
    }

    notifyVisualization();
  }

  public void mergeKnowledge(SerializableKnowledge knowledge) {
    mergeMap(knowledge.getGraph());
    mergeTreasures(knowledge.getTreasures());
    mergeAgents(knowledge.getAgents());
    mergeSilo(knowledge.getSilo());
    mergeGolem(knowledge.getGolem());
  }

  // hasOpenNode checks if any unexplored nodes remain in the graph.
  // Used to determine if exploration should continue.
  public boolean hasOpenNode() {
    return this.worldGraph
        .nodes()
        .filter(currentNode -> "open".equals(currentNode.getAttribute("ui.class")))
        .findAny()
        .isPresent();
  }

  private void notifyVisualization() {
    if (this.visualization != null) {
      this.visualization.updateFromModel(this);
    }
  }

  public void createVisualization() {
    try {
      this.visualization = new KnowledgeVisualization(this.agent);
      if (this.visualization.initialize()) {
        attachVisualization(this.visualization);
      }
    } catch (Exception e) {
      System.err.println("Failed to create visualization: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public synchronized void beforeMove() {
    serializeGraphTopology();
    if (this.visualization != null) {
      this.visualization.close();
      this.visualization = null;
    }
    this.worldGraph = null;
  }

  public synchronized void afterMove() {
    this.worldGraph = new SingleGraph("My world vision");

    // we want to reconstruct the grap from the serialized graph
    Integer edgeCounter = 0;
    for (SerializableNode<String, MapAttribute> currentNode : this.serializableGraph.getAllNodes()) {
      this.worldGraph
          .addNode(currentNode.getNodeId())
          .setAttribute("ui.class", currentNode.getNodeContent().toString());

      for (String s : this.serializableGraph.getEdges(currentNode.getNodeId())) {
        try {
          this.worldGraph.addEdge(edgeCounter.toString(), currentNode.getNodeId(), s);
          edgeCounter++;
        } catch (Exception e) {
          // edge already exists or nodes not found
        }
      }
    }

    notifyVisualization();

    System.out.println("Loading done");
  }
}

class KnowledgeVisualization {
  private static final String DEFAULT_STYLESHEET = "node {" +
      "  fill-color: black;" +
      "  size-mode: fit;" +
      "  text-alignment: under;" +
      "  text-size: 14;" +
      "  text-color: black;" +
      "  text-background-mode: rounded-box;" +
      "  text-background-color: white;" +
      "}" +
      "node.open { fill-color: blue; }" +
      "node.closed { fill-color: grey; }" +
      "node.me { fill-color: black; size: 20px; }" +
      "node.treasure { fill-color: yellow; }" +
      "node.agent { fill-color: forestgreen; }" +
      "node.silo { fill-color: orange; }" +
      "node.golem { fill-color: red; }" +
      "edge { fill-color: #999; }" +
      "edge.path { fill-color: green; stroke-width: 10px; }";

  private final AtomicBoolean isInitialized = new AtomicBoolean(false);
  private FxViewer viewer;
  private boolean isViewerActive = false;
  private final String agentName;

  // Add fields for the info panel
  private Label infoLabel;
  private Stage stage;
  private SplitPane splitPane;

  public KnowledgeVisualization(String agentName) {
    this.agentName = agentName;
  }

  public boolean initialize() {
    if (isInitialized.get()) {
      return true;
    }

    try {
      System.setProperty("org.graphstream.ui", "javafx");
      // Initialize JavaFX components
      Platform.runLater(() -> {
        // Create the main layout components but don't show yet
        initializeLayout();
      });
      isInitialized.set(true);
      return true;
    } catch (Exception e) {
      System.err.println("Failed to initialize visualization: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  // Content VBox to hold all info sections
  private VBox infoContent;

  // Section labels for each category
  private Label agentHeaderLabel;
  private VBox statusSection;
  private VBox treasuresSection;
  private VBox agentsSection;
  private VBox environmentSection;
  private VBox pathSection;

  private void initializeLayout() {
    // Create the main window
    stage = new Stage();
    stage.setTitle("Knowledge Map - Agent: " + agentName);

    // Create a split pane to hold both the graph and info panel
    splitPane = new SplitPane();
    splitPane.setOrientation(Orientation.HORIZONTAL);

    // Create a VBox to hold styled information content
    infoContent = new VBox(10);
    infoContent.setPadding(new Insets(15));
    infoContent.setStyle("-fx-background-color: #f4f4f4;");

    // Create section for Agent header (centered and bold)
    agentHeaderLabel = new Label(agentName);
    agentHeaderLabel.setStyle("-fx-font-size: 18px; -fx-font-style: italic; -fx-font-weight: bold;");
    agentHeaderLabel.setMaxWidth(Double.MAX_VALUE);
    agentHeaderLabel.setAlignment(javafx.geometry.Pos.CENTER);
    infoContent.getChildren().add(agentHeaderLabel);

    // Create STATUS section
    Label statusHeader = createSectionHeader("Status");
    statusSection = new VBox(5);
    infoContent.getChildren().addAll(statusHeader, statusSection);

    // Create TREASURES section
    Label treasuresHeader = createSectionHeader("Treasures");
    treasuresSection = new VBox(5);
    infoContent.getChildren().addAll(treasuresHeader, treasuresSection);

    // Create AGENTS section
    Label agentsHeader = createSectionHeader("Agents");
    agentsSection = new VBox(5);
    infoContent.getChildren().addAll(agentsHeader, agentsSection);

    // Create ENVIRONMENT section
    Label environmentHeader = createSectionHeader("Environment");
    environmentSection = new VBox(5);
    infoContent.getChildren().addAll(environmentHeader, environmentSection);

    // Create PATH section
    Label pathHeader = createSectionHeader("Path");
    pathSection = new VBox(5);
    infoContent.getChildren().addAll(pathHeader, pathSection);

    // Create a scroll pane for the info panel
    ScrollPane infoScrollPane = new ScrollPane(infoContent);
    infoScrollPane.setFitToWidth(true);
    infoScrollPane.setPrefWidth(300); // Set preferred width for info panel
    infoScrollPane.setStyle("-fx-background: #f4f4f4; -fx-background-color: #f4f4f4;");

    // Add the info panel to the split pane (it will be on the left)
    splitPane.getItems().add(infoScrollPane);

    // Create the scene
    Scene scene = new Scene(splitPane, 1000, 700);
    stage.setScene(scene);
  }

  private Label createSectionHeader(String text) {
    Label header = new Label(text);
    header.setStyle("-fx-font-weight: bold; -fx-font-style: italic;");
    header.setMaxWidth(Double.MAX_VALUE);
    header.setAlignment(javafx.geometry.Pos.CENTER);
    header.setPadding(new Insets(5, 0, 5, 0));
    return header;
  }

  public void updateFromModel(Knowledge knowledge) {
    if (!isInitialized.get()) {
      return;
    }

    if (!isViewerActive || viewer == null) {
      Graph graph = knowledge.getGraph();
      if (graph != null) {
        createOrUpdateViewer(graph, knowledge);
      }
    } else {
      updateExistingVisualization(knowledge);
    }
  }

  private void createOrUpdateViewer(Graph graph, Knowledge knowledge) {
    graph.setAttribute("ui.stylesheet", DEFAULT_STYLESHEET);

    styleNodes(graph, knowledge);
    styleEdgesForPath(graph, knowledge);
    updateInfoPanel(knowledge);

    Platform.runLater(() -> {
      try {
        openGui(graph);
      } catch (Exception e) {
        System.err.println("Error opening GUI: " + e.getMessage());
        e.printStackTrace();
      }
    });
  }

  private void updateExistingVisualization(Knowledge knowledge) {
    Graph graph = knowledge.getGraph();
    if (graph == null) {
      return;
    }

    Platform.runLater(() -> {
      try {
        styleNodes(graph, knowledge);
        styleEdgesForPath(graph, knowledge);
        updateInfoPanel(knowledge);
      } catch (Exception e) {
        System.err.println("Error updating visualization: " + e.getMessage());
        e.printStackTrace();
      }
    });
  }

  private void styleNodes(Graph graph, Knowledge knowledge) {
    graph.nodes().forEach(node -> {
      String nodeId = node.getId();

      // Set default label
      node.setAttribute("ui.label", nodeId);

      // Get node class (default to closed if not open)
      String nodeClass = (String) node.getAttribute("ui.class");
      if (nodeClass == null || (!nodeClass.equals("open") && !nodeClass.equals("closed"))) {
        nodeClass = "closed";
      }

      // Determine what's at this node
      boolean hasTreasure = knowledge.hasTreasure(nodeId);
      String agentHere = knowledge.getAgentAtPosition(nodeId);
      boolean isSilo = nodeId.equals(knowledge.getSiloPosition());
      boolean isGolem = nodeId.equals(knowledge.getGolemPosition());
      boolean isCurrentPosition = nodeId.equals(knowledge.getAgentData().getPosition());

      // Set node class and label based on content
      if (isCurrentPosition) {
        // Current agent position takes precedence
        node.setAttribute("ui.class", "me");

        // Include treasure info in label if present
        if (hasTreasure) {
          TreasureData treasure = knowledge.getTreasureData(nodeId);
          node.setAttribute("ui.label",
              nodeId + "-" + this.agentName + "-" + treasure.getType() + "(" + treasure.getQuantity() + ")");
        } else {
          node.setAttribute("ui.label", nodeId + "-" + this.agentName);
        }
      } else if (agentHere != null) {
        // Other agent is here
        node.setAttribute("ui.class", "agent");

        // Include treasure info in label if present
        if (hasTreasure) {
          TreasureData treasure = knowledge.getTreasureData(nodeId);
          node.setAttribute("ui.label",
              nodeId + "-" + agentHere + "-" + treasure.getType() + "(" + treasure.getQuantity() + ")");
        } else {
          node.setAttribute("ui.label", nodeId + "-" + agentHere);
        }
      } else if (isGolem) {
        // Golem is here
        node.setAttribute("ui.class", "golem");

        // Include treasure info in label if present
        if (hasTreasure) {
          TreasureData treasure = knowledge.getTreasureData(nodeId);
          node.setAttribute("ui.label", nodeId + "-Golem-" + treasure.getType() + "(" + treasure.getQuantity() + ")");
        } else {
          node.setAttribute("ui.label", nodeId + "-Golem");
        }
      } else if (isSilo) {
        // Silo is here
        node.setAttribute("ui.class", "silo");

        // Include treasure info in label if present
        if (hasTreasure) {
          TreasureData treasure = knowledge.getTreasureData(nodeId);
          node.setAttribute("ui.label", nodeId + "-Silo-" + treasure.getType() + "(" + treasure.getQuantity() + ")");
        } else {
          node.setAttribute("ui.label", nodeId + "-Silo");
        }
      } else if (hasTreasure) {
        // Only treasure is here
        TreasureData treasure = knowledge.getTreasureData(nodeId);
        node.setAttribute("ui.class", "treasure");
        node.setAttribute("ui.label", nodeId + "-" + treasure.getType() + "(" + treasure.getQuantity() + ")");
      } else {
        // Just a regular node (open or closed)
        node.setAttribute("ui.class", nodeClass);
      }
    });
  }

  private void styleEdgesForPath(Graph graph, Knowledge knowledge) {
    graph.edges().forEach(edge -> edge.removeAttribute("ui.class"));
    if (!knowledge.getGoalPath().isEmpty()) {
      Deque<String> path = new ArrayDeque<>(knowledge.getGoalPath());
      path.addFirst(knowledge.getAgentData().getPosition());

      String[] pathArray = path.toArray(new String[0]);
      for (int i = 0; i < pathArray.length - 1; i++) {
        String currentNode = pathArray[i];
        String nextNode = pathArray[i + 1];
        graph.edges()
            .filter(edge -> (edge.getSourceNode().getId().equals(currentNode) &&
                edge.getTargetNode().getId().equals(nextNode)) ||
                (edge.getSourceNode().getId().equals(nextNode) &&
                    edge.getTargetNode().getId().equals(currentNode)))
            .forEach(edge -> edge.setAttribute("ui.class", "path"));
      }
    }
  }

  private void updateInfoPanel(Knowledge knowledge) {
    // Update the agent header
    Platform.runLater(() -> {
      // Update agent name
      agentHeaderLabel.setText(agentName);

      // Clear previous content from each section
      statusSection.getChildren().clear();
      treasuresSection.getChildren().clear();
      agentsSection.getChildren().clear();
      environmentSection.getChildren().clear();
      pathSection.getChildren().clear();

      // Update STATUS section
      Label desiresLabel = new Label(String.format("Desires: Explore=%d Collect=%d",
          knowledge.wantsToCollect() ? 0 : 1,
          knowledge.wantsToCollect() ? 1 : 0));

      Label introvertLabel = new Label("Introvert Counter: " + knowledge.getIntrovertCounter());
      Label blockLabel = new Label("Block Counter: " + knowledge.getBlockCounter());

      // Add agent expertise and backpack info if available
      AgentData myAgent = knowledge.getAgentData();
      if (myAgent != null) {
        // Add expertise information
        if (myAgent.getExpertise() != null && !myAgent.getExpertise().isEmpty()) {
          StringBuilder expertise = new StringBuilder("Expertise: ");
          for (Map.Entry<Observation, Integer> exp : myAgent.getExpertise().entrySet()) {
            expertise.append(exp.getKey()).append("=").append(exp.getValue()).append(" ");
          }
          Label expertiseLabel = new Label(expertise.toString().trim());
          statusSection.getChildren().add(expertiseLabel);
        }

        // Add backpack information
        Label backpackLabel = new Label(String.format("Backpack: %d/%d free",
            myAgent.getBackpackFreeSpace(), myAgent.getBackpackCapacity()));

        // Add treasure type if specified
        if (myAgent.getTreasureType() != null) {
          Label treasureTypeLabel = new Label("Collects: " + myAgent.getTreasureType());
          statusSection.getChildren().add(treasureTypeLabel);
        }

        statusSection.getChildren().addAll(desiresLabel, introvertLabel, blockLabel, backpackLabel);
      } else {
        statusSection.getChildren().addAll(desiresLabel, introvertLabel, blockLabel);
      }

      // Update TREASURES section
      for (Map.Entry<String, TreasureData> entry : knowledge.getTreasures().entrySet()) {
        TreasureData treasure = entry.getValue();
        if (treasure.getQuantity() > 0) {
          StringBuilder treasureInfo = new StringBuilder();
          treasureInfo.append(entry.getKey()).append(": ")
              .append(treasure.getType()).append(" (")
              .append(treasure.getQuantity()).append(")");

          // Add lock information if it's locked
          if (treasure.isLocked()) {
            treasureInfo.append(" [Locked: ").append(treasure.getLockStrength()).append("]");
          }

          // Add pickup strength if needed
          if (treasure.getPickStrength() > 0) {
            treasureInfo.append(" [Strength: ").append(treasure.getPickStrength()).append("]");
          }

          Label treasureLabel = new Label(treasureInfo.toString());
          treasuresSection.getChildren().add(treasureLabel);
        }
      }

      // Update AGENTS section
      for (Map.Entry<String, AgentData> entry : knowledge.getAgents().entrySet()) {
        AgentData agent = entry.getValue();
        StringBuilder agentInfo = new StringBuilder();
        agentInfo.append(entry.getKey()).append(": ")
            .append("Pos=").append(agent.getPosition())
            .append(" Status=").append(agent.getStatus());

        // Add expertise if available
        if (agent.getExpertise() != null && !agent.getExpertise().isEmpty()) {
          agentInfo.append(" Exp=[");
          for (Map.Entry<Observation, Integer> exp : agent.getExpertise().entrySet()) {
            agentInfo.append(exp.getKey()).append("=").append(exp.getValue()).append(" ");
          }
          agentInfo.append("]");
        }

        Label agentLabel = new Label(agentInfo.toString());
        agentsSection.getChildren().add(agentLabel);
      }

      // Update ENVIRONMENT section
      if (knowledge.getSilo() != null) {
        Label siloLabel = new Label("Silo: " + knowledge.getSiloPosition() +
            " (Age: " + knowledge.getSiloUpdateCounter() + ")");
        environmentSection.getChildren().add(siloLabel);
      }

      if (knowledge.getGolem() != null) {
        Label golemLabel = new Label("Golem: " + knowledge.getGolemPosition() +
            " (Age: " + knowledge.getGolemUpdateCounter() + ")");
        environmentSection.getChildren().add(golemLabel);
      }

      // Update PATH section
      if (!knowledge.getGoalPath().isEmpty()) {
        StringBuilder pathInfo = new StringBuilder("Path to closest treasure: ");
        for (String nodeId : knowledge.getGoalPath()) {
          pathInfo.append(nodeId).append(" → ");
        }
        if (pathInfo.length() > 2) {
          pathInfo.delete(pathInfo.length() - 3, pathInfo.length());
        }

        Label pathLabel = new Label(pathInfo.toString());
        pathLabel.setWrapText(true);
        pathSection.getChildren().add(pathLabel);
      }
    });
  }

  private void openGui(Graph graph) {
    try {
      if (this.viewer == null) {
        this.viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        this.viewer.enableAutoLayout();
        this.viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);

        // Get the view from the viewer
        View view = this.viewer.addDefaultView(false); // false means don't create its own window

        // Add the view to the right side of the split pane
        Platform.runLater(() -> {
          // Make sure the splitPane has exactly 2 items (info panel and graph view)
          if (splitPane.getItems().size() == 1) {
            // Add the graph view to the second position (right side)
            splitPane.getItems().add((javafx.scene.Node) view);

            // Set the divider position (30% for info, 70% for graph)
            splitPane.setDividerPositions(0.3);

            // Show the stage if not already showing
            if (!stage.isShowing()) {
              stage.show();
            }
          }
        });

        // Add a viewer listener to detect when the window is closed
        stage.setOnCloseRequest(event -> {
          close();
        });

        this.isViewerActive = true;
      }
    } catch (Exception e) {
      System.err.println("Error opening GUI: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void close() {
    if (this.viewer != null) {
      Platform.runLater(() -> {
        try {
          // Close the main stage
          if (stage != null) {
            stage.close();
          }

          // Close the viewer
          this.viewer.close();
        } catch (NullPointerException e) {
          System.err.println(
              "Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
        } finally {
          this.viewer = null;
          this.isViewerActive = false;
        }
      });
    }
  }

  public boolean isActive() {
    return isViewerActive;
  }

  public boolean isInitialized() {
    return isInitialized.get();
  }
}
