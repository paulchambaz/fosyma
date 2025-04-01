package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.serializableGraph.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.stream.Collectors;
import javafx.application.Platform;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import eu.su.mas.dedale.env.Observation;

import eu.su.mas.dedaleEtu.princ.Utils;

public class Knowledge implements Serializable {
  private static final long serialVersionUID = -1333959882640838272L;

  private float desireExplore;
  private float desireCollect;

  private String agent;

  private Graph worldGraph;
  private Integer nbEdges;
  private SerializableSimpleGraph<String, MapAttribute> serializableGraph;

  private Map<String, TreasureData> treasures;
  private Map<String, AgentData> agents;
  private SiloData silo;
  private GolemData golem;

  private Integer introvertCounter;
  private static Integer INTROVERT_LOCKDOWN_TIME = 16;
  private Integer blockCounter;

  private String goal;
  private ArrayDeque<String> goalPath;

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

  public void updateDesireExplore() {
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

  public boolean wantsToCollect() {
    return this.desireExplore < this.desireCollect;
  }

  public void attachVisualization(KnowledgeVisualization visualization) {
    this.visualization = visualization;
    if (this.visualization != null) {
      this.visualization.updateFromModel(this);
    }
  }

  public Graph getGraph() {
    return this.worldGraph;
  }

  public synchronized void introvertReset() {
    this.introvertCounter = 0;
  }

  public synchronized void introvertSoftReset() {
    this.introvertCounter = INTROVERT_LOCKDOWN_TIME;
  }

  public synchronized void introvertRecovery() {
    this.introvertCounter += 1;
  }

  public synchronized boolean introvertCanTalk() {
    return this.introvertCounter > INTROVERT_LOCKDOWN_TIME;
  }

  public synchronized Integer getIntrovertCounter() {
    return introvertCounter;
  }

  public synchronized void addNode(String id, MapAttribute mapAttribute) {
    Node currentNode;
    if (this.worldGraph.getNode(id) == null) {
      currentNode = this.worldGraph.addNode(id);
    } else {
      currentNode = this.worldGraph.getNode(id);
    }
    currentNode.clearAttributes();
    currentNode.setAttribute("ui.class", mapAttribute.toString());
    currentNode.setAttribute("ui.label", id);

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

    if (this.worldGraph != null && this.worldGraph.getNode(nodeId) != null) {
      Node currentNode = this.worldGraph.getNode(nodeId);
      currentNode.setAttribute("ui.style", "fill-color: yellow;");
      currentNode.setAttribute("ui.label", nodeId + "-" + type);
      notifyVisualization();
    }
  }

  // updateTreasureState updates whether a treasure at the specified node is open
  // (accessible). Used when agents successfully unlock a treasure chest.
  public synchronized void updateTreasureState(String nodeId, boolean isLocked) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData treasure = this.treasures.get(nodeId);
      treasure.setLocked(isLocked);
      treasure.resetCounter();
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
    }
  }

  // ageTreasureData ages all treasure data by incrementing their counters.
  // Should be called regularly to track data freshness.
  public synchronized void ageTreasureData() {
    for (TreasureData treasure : this.treasures.values()) {
      treasure.incrementCounter();
    }
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

  // updateAgentPosition tracks the current position of an agent in the
  // environment.
  // Creates a new agent record if it doesn't exist or updates an existing one.
  public synchronized void updateAgentPosition(String agentName, String nodeId) {
    if (!this.agents.containsKey(agentName)) {
      this.agents.put(agentName, new AgentData(nodeId));
    } else {
      AgentData agentData = this.agents.get(agentName);
      agentData.setPosition(nodeId);
      agentData.resetCounter();
    }
  }

  // updateAgentExpertise records the expertise levels of an agent for different
  // treasure types.
  // Expertise determines an agent's efficiency in handling specific treasures.
  public synchronized void updateAgentExpertise(String agentName, Map<Observation, Integer> expertise) {
    if (this.agents.containsKey(agentName)) {
      this.agents.get(agentName).setExpertise(expertise);
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
    }
  }

  public synchronized void updateAgentStatus(String agentName, String status) {
    if (this.agents.containsKey(agentName)) {
      this.agents.get(agentName).setStatus(status);
    }
  }

  public synchronized void ageAgentData() {
    for (AgentData agent : this.agents.values()) {
      agent.incrementCounter();
    }
  }

  // getAgentData retrieves all information about a specific agent.
  public synchronized AgentData getAgentData(String agentName) {
    return this.agents.get(agentName);
  }

  // getAgentAtPosition finds all agents currently located at a specific node.
  // Useful for coordinating actions between agents in proximity.
  public synchronized List<String> getAgentAtPosition(String nodeId) {
    return this.agents.entrySet().stream()
        .filter(entry -> nodeId.equals(entry.getValue().getPosition()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
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

    if (this.worldGraph != null && this.worldGraph.getNode(nodeId) != null) {
      Node currentNode = this.worldGraph.getNode(nodeId);
      currentNode.setAttribute("ui.style", "fill-color: orange; size: 20px;");
      currentNode.setAttribute("ui.label", "SILO");
      notifyVisualization();
    }
  }

  public synchronized void ageSiloData() {
    if (this.silo != null) {
      this.silo.incrementCounter();
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

    if (this.worldGraph != null && this.worldGraph.getNode(nodeId) != null) {
      Node currentNode = this.worldGraph.getNode(nodeId);
      currentNode.setAttribute("ui.style", "fill-color: red; size: 20px;");
      currentNode.setAttribute("ui.label", "GOLEM");
      notifyVisualization();
    }
  }

  public synchronized void ageGolemData() {
    if (this.golem != null) {
      this.golem.incrementCounter();
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
    // TODO: im not sure this function works properly - must test
    Graph tempGraph = new SingleGraph("Temporary path graph");

    this.worldGraph.nodes().forEach(node -> {
      String id = node.getId();

      for (AgentData agent : this.agents.values()) {
        String position = agent.getPosition();
        if (position == id) {
          return;
        }
      }

      if (this.silo != null && this.silo.getPosition() == id) {
        return;
      }

      if (this.golem != null && this.golem.getPosition() == id) {
        return;
      }

      tempGraph.addNode(id);
    });

    this.worldGraph.edges().forEach(edge -> {
      try {
        tempGraph.addEdge(
            edge.getId(),
            edge.getSourceNode().getId(),
            edge.getTargetNode().getId());
      } catch (Exception e) {
      }
    });

    return tempGraph;
  }

  // getShortestPath calculates the most efficient path between any two nodes.
  // Uses Dijkstra's algorithm to find the optimal route.
  public synchronized List<String> getShortestPath(String idFrom, String idTo) {
    Graph tempGraph = createTempGraph();

    Dijkstra dijkstra = new Dijkstra();
    dijkstra.init(tempGraph);
    dijkstra.setSource(tempGraph.getNode(idFrom));
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
    // TODO: we currenly filter through UI attributes, this is horrible and we can
    // and should do better
    return this.worldGraph
        .nodes()
        .filter(currentNode -> currentNode.getAttribute("ui.class") == MapAttribute.open.toString())
        .map(Node::getId)
        .collect(Collectors.toList());
  }

  private void serializeGraphTopology() {
    this.serializableGraph = new SerializableSimpleGraph<String, MapAttribute>();

    Iterator<Node> nodeIterator = this.worldGraph.iterator();
    while (nodeIterator.hasNext()) {
      Node currentNode = nodeIterator.next();
      this.serializableGraph.addNode(
          currentNode.getId(),
          MapAttribute.valueOf((String) currentNode.getAttribute("ui.class")));
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
    // TODO: we are using UI attributes in order to perform a filter : this is
    // horrible
    return (this.worldGraph
        .nodes()
        .filter(currentNode -> currentNode.getAttribute("ui.class") == MapAttribute.open.toString())
        .findAny()).isPresent();
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
      } else {
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
    }
    this.worldGraph = null;
  }

  public synchronized void afterMove() {
    this.worldGraph = new SingleGraph("My world vision");

    Integer edgeCounter = 0;
    for (SerializableNode<String, MapAttribute> currentNode : this.serializableGraph.getAllNodes()) {
      this.worldGraph
          .addNode(currentNode.getNodeId())
          .setAttribute("ui.class", currentNode.getNodeContent().toString());

      for (String s : this.serializableGraph.getEdges(currentNode.getNodeId())) {
        this.worldGraph.addEdge(edgeCounter.toString(), currentNode.getNodeId(), s);
        edgeCounter++;
      }
    }

    notifyVisualization();

    System.out.println("Loading done");
  }
}

class KnowledgeVisualization {
  private static final String DEFAULT_NODE_STYLE = "node {fill-color: black; size-mode:fit; text-alignment:under;"
      + " text-size:14; text-color:white; text-background-mode:rounded-box; text-background-color:black;}";
  private static final String NODE_STYLE_OPEN = "node.open {fill-color: blue;}";
  private static final String NODE_STYLE_AGENT = "node.agent {fill-color: forestgreen;}";
  private static final String NODE_STYLE_CLOSED = "node.closed {fill-color: grey;}";

  private Viewer viewer;
  private final AtomicBoolean isInitialized = new AtomicBoolean(false);
  private boolean isViewerActive = false;
  private final String agentName;

  public KnowledgeVisualization(String agentName) {
    this.agentName = agentName;
  }

  public boolean initialize() {
    if (isInitialized.get()) {
      return true;
    }

    try {
      System.setProperty("org.graphstream.ui", "javafx");
      isInitialized.set(true);
      return true;
    } catch (Exception e) {
      System.err.println("Failed to initialize visualization :" + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public void updateFromModel(Knowledge knowledge) {
    if (!isInitialized.get()) {
      return;
    }

    if (!isViewerActive && viewer == null) {
      Graph graph = knowledge.getGraph();
      if (graph != null) {
        graph.setAttribute("ui.title", "Knowledge Map - Agent: " + agentName);
        applyGraphStyling(graph);

        Platform.runLater(() -> {
          try {
            openGui(graph);
          } catch (Exception e) {
            System.err.println("Failed to open visualization: " + e.getMessage());
            e.printStackTrace();
          }
        });
      }
    }
  }

  private void applyGraphStyling(Graph graph) {
    StringBuilder styleSheet = new StringBuilder();
    styleSheet.append(DEFAULT_NODE_STYLE);
    styleSheet.append(NODE_STYLE_OPEN);
    styleSheet.append(NODE_STYLE_AGENT);
    styleSheet.append(NODE_STYLE_CLOSED);
    graph.setAttribute("ui.stylesheet", styleSheet.toString());
  }

  private void openGui(Graph graph) {
    try {
      if (this.viewer == null) {
        this.viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        this.viewer.enableAutoLayout();
        this.viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
        this.viewer.addDefaultView(true);
        graph.display();
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
