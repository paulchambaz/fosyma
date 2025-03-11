package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
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

// MapRepresentation serves as the agent's internal model of the environment in a multi-agent 
// exploration system. It maintains a graph representation of discovered locations, tracks
// treasure information, agent positions, and provides pathfinding capabilities.
// 
// The class provides functionality to:
// - Build and visualize a graph-based map of the environment
// - Track treasure locations, types, quantities and state
// - Monitor agent positions and capabilities
// - Calculate optimal paths between nodes
// - Serialize the map for agent communication
// - Merge maps from other agents
//
// Each node in the graph represents a location that can be in different states (open, closed, agent)
// and edges represent connections between locations. The class also maintains information about
// treasures, agents, and special locations like silos.
public class MapRepresentation implements Serializable {

  // MapAttribute represents the possible states of a node in the map
  // - agent: Node is currently occupied by an agent
  // - open: Node is discovered but not fully explored
  // - closed: Node is fully explored with no further actions needed
  public enum MapAttribute {
    agent,
    open,
    closed;
  }

  private static final long serialVersionUID = -1333959882640838272L;

  private String defaultNodeStyle =
      "node {fill-color: black; size-mode:fit;text-alignment:under;"
          + " text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";

  private String nodeStyle_open = "node.agent {" + "fill-color: forestgreen;" + "}";
  private String nodeStyle_agent = "node.open {" + "fill-color: blue;" + "}";
  private String nodeStyle = defaultNodeStyle + nodeStyle_agent + nodeStyle_open;

  private Graph worldGraph;
  private Viewer viewer;
  private Integer nbEdges;
  private SerializableSimpleGraph<String, MapAttribute> serializableGraph;

  private Map<String, TreasureData> treasures;
  private Map<String, AgentData> agents;
  private Couple<String, Integer> siloPosition;
  private Couple<String, Integer> golemPosition;

  public MapRepresentation() {
    System.setProperty("org.graphstream.ui", "javafx");
    this.worldGraph = new SingleGraph("My world vision");
    this.worldGraph.setAttribute("ui.stylesheet", nodeStyle);

    Platform.runLater(() -> {
      openGui();
    });

    this.nbEdges = 0;

    this.treasures = new HashMap<>();
    this.agents = new HashMap<>();
    this.siloPosition = null;
    this.golemPosition = null;
  }

  // addNode adds or updates a node in the graph with the specified attribute.
  // The node will be visually styled according to its attribute type.
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
    } catch (IdAlreadyInUseException e1) {
      System.exit(1);
    } catch (EdgeRejectedException e2) {
      this.nbEdges--;
    } catch (ElementNotFoundException e3) {

    }
  }

  // addTreasure records the discovery of a treasure at a specific node, tracking its
  // type, quantity, lock and pick strength requirements.
  // The node is visually highlighted in yellow with the treasure type displayed.
  public synchronized void addTreasure(String nodeId, Observation type, int quantity, int lockStrength, int pickStrength) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData existing = this.treasures.get(nodeId);
      existing.setNodeId(nodeId);
      if (existing.getUpdateCounter() > 0) {
        existing.resetCounter();

        if (quantity > 0) {
          // TODO; we should just have an updateQuantity
          existing.decreaseQuantity(existing.getQuantity());
          existing.decreaseQuantity(-quantity);
        }
      }
    } else {
      TreasureData treasure = new TreasureData(nodeId, type, quantity, quantity > 0, lockStrength, pickStrength);
      this.treasures.put(nodeId, treasures);
    }

    if (this.worldGraph != null && this.worldGraph.getNode(nodeId) != null) {
      Node currentNode = this.worldGraph.getNode(nodeId);
      currentNode.setAttribute("ui.style", "fill-color: yellow;");
      currentNode.setAttribute("ui.label", nodeId + "-" + type);
    }
  }

  // updateTreasureState updates whether a treasure at the specified node is open (accessible).
  // Used when agents successfully unlock a treasure chest.
  public synchronized void updateTreasureState(String nodeId, boolean isLocked) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData treasure = this.treasures.get(nodeId);
      treasure.setLocked(isLocked);
      treasure.resetCounter();
    }
  }

  // updateTreasureQuantity decreases the quantity of treasure at a node by the specified amount.
  // Used when agents collect treasures from a location.
  public synchronized void updateTreasureQuantity(String nodeId, int quantityPicked) {
    if (this.treasures.containsKey(nodeId)) {
      this.treasures.get(nodeId).decreaseQuantity(quantityPicked);
    }
  }

  // hasTreasure checks if a node contains any remaining treasure.
  // Returns true if treasure exists and quantity is greater than zero.
  public synchronized boolean hasTreasure(String nodeId) {
    return this.treasures.containsKey(nodeId) && this.treasures.get(nodeId).getQuantity() > 0;
  }

  // getTreasureData retrieves all information about a treasure at a specific node.
  public synchronized TreasureData getTreasureData(String nodeId) {
    return this.treasures.get(nodeId);
  }

  // getNodesWithTreasureType finds all nodes containing a specific type of treasure with quantity > 0.
  // Useful for targeting specific treasure types during exploration.
  public synchronized List<String> getNodesWithTreasureType(Observation type) {
    return this.treasures.entrySet().stream()
      .filter(entry -> entry.getValue().getType() == type && entry.getValue().getQuantity() > 0)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  // updateAgentPosition tracks the current position of an agent in the environment.
  // Creates a new agent record if it doesn't exist or updates an existing one.
  public synchronized void updateAgentPosition(String agentName, String nodeId) {
    if (!this.agents.containsKey(agentName)) {
      this.agents.put(agentName, new AgentData(nodeId));
    } else{
      this.agents.get(agentName).setPosition(nodeId);
    }
  }

  // updateAgentExpertise records the expertise levels of an agent for different treasure types.
  // Expertise determines an agent's efficiency in handling specific treasures.
  public synchronized void updateAgentExpertise(String agentName, Map<Observation, Integer> expertise) {
    if (!this.agents.containsKey(agentName)) {
      AgentData data = new AgentData(null);
      data.setExpertise(expertise);
      this.agents.put(agentName, data);
    } else {
      this.agents.get(agentName).setExpertise(expertise);
    }
  }

  // updateAgentBackpack updates information about an agent's carrying capacity and available space.
  // Important for planning treasure collection strategies.
  public synchronized void updateAgentBackpack(String agentName, int capacity, int freeSpace) {
    if (!this.agent(containsKey(agentName)) {
      AgentData data = new AgentData(agentName, null);
      data.setBackpackCapacity(capacity);
      data.setBackpackFreeSpace(freeSpace);
    } else {
      AgentData data = this.agent.get(agentName);
      data.setBackpackCapacity(capacity);
      data.setBackpackFreeSpace(freeSpace);
      data.resetCounter();
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

  // setSiloPosition marks the location of a silo in the environment.
  // Silos are special locations where treasures can be deposited.
  // The node is visually highlighted in orange and labeled as "SILO".
  public synchronized void setSiloPosition(String nodeId) {
    this.siloPosition = new Couple<>(nodeId, 0);

    if (this.worldGraph != null && this.worldGraph.getNode(nodeId) != null) {
        Node currentNode = this.worldGraph.getNode(nodeId);
        currentNode.setAttribute("ui.style", "fill-color: orange; size: 20px;");
        currentNode.setAttribute("ui.label", "SILO");
    }
  }

  // getSiloPosition retrieves the location of the silo.
  public synchronized String getSiloPosition() {
    return (siloPosition != null) ? siloPosition.getLeft() : null;
  }

  public synchronized int getSiloPositionCounter() {
    return (siloPosition != null) ? siloPosition.getRight() : -1;
  }

  // getShortestPathToSilo calculates the most efficient path from a given position to the silo.
  // Important for optimizing treasure delivery.
  public synchronized List<String> getShortestPathToSilo(String currentPosition) {
    if (siloPosition == null) return null;
    return getShortestPath(currentPosition, siloPosition.getLeft());
  }

  // getShortestPath calculates the most efficient path between any two nodes.
  // Uses Dijkstra's algorithm to find the optimal route.
  public synchronized List<String> getShortestPath(String idFrom, String idTo) {
    List<String> shortestPath = new ArrayList<String>();

    Dijkstra dijkstra = new Dijkstra();
    dijkstra.init(this.worldGraph);
    dijkstra.setSource(this.worldGraph.getNode(idFrom));
    dijkstra.compute();

    List<Node> path = dijkstra.getPath(this.worldGraph.getNode(idTo)).getNodePath();

    Iterator<Node> iter = path.iterator();
    while (iter.hasNext()) {
      shortestPath.add(iter.next().getId());
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
    List<String> opennodes = getOpenNodes();

    List<Couple<String, Integer>> pathDistances = opennodes.stream()
      .map(on ->
        (getShortestPath(myPosition, on) != null)
          ? new Couple<String, Integer>(on, getShortestPath(myPosition, on).size())
          : new Couple<String, Integer>(on, Integer.MAX_VALUE)
      )
      .collect(Collectors.toList());

    Optional<Couple<String, Integer>> closest = pathDistances.stream().min(Comparator.comparing(Couple::getRight));

    return getShortestPath(myPosition, closest.get().getLeft());
  }

  // getOpenNodes retrieves all nodes marked as open (discovered but not fully explored).
  // Helps prioritize exploration efforts.
  public List<String> getOpenNodes() {
    return this.worldGraph
        .nodes()
        .filter(currentNode -> currentNode.getAttribute("ui.class") == MapAttribute.open.toString())
        .map(Node::getId)
        .collect(Collectors.toList());
  }

  // prepareMigration prepares the map for serialization before agent movement.
  // Serializes the graph topology and closes the visualization.
  public void prepareMigration() {
    serializeGraphTopology();

    closeGui();

    this.worldGraph = null;
  }

  // getSerializableGraph creates a serializable version of the map for communication.
  // Enables sharing of map information between agents.
  private void serializeGraphTopology() {
    this.serializableGraph = new SerializableSimpleGraph<String, MapAttribute>();

    Iterator<Node> iter = this.worldGraph.iterator();
    while (iter.hasNext()) {
      Node currentNode = iter.next();
      this.serializableGraph.addNode(currentNode.getId(), MapAttribute.valueOf((String) currentNode.getAttribute("ui.class")));
    }

    Iterator<Edge> edgeIterator = this.worldGraph.edges().iterator();
    while (edgeIterator.hasNext()) {
      Edge currentEdge = edgeIterator.next();
      Node sourceNode = currentEdge.getSourceNode();
      Node targetNode = currentEdge.getTargetNode();
      this.serializableGraph.addEdge(currentEdge.getId(), sourceNode.getId(), targetNode.getId());
    }
  }

  public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializableGraph() {
    serializeGraphTopology();
    return this.serializableGraph;
  }

  // loadSavedData reconstructs the map from a serialized representation.
  // Used when receiving map information from other agents.
  public synchronized void loadSavedData() {
    this.worldGraph = new SingleGraph("My world vision");
    this.worldGraph.setAttribute("ui.stylesheet", nodeStyle);

    openGui();

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

    System.out.println("Loading done");
  }

  private synchronized void closeGui() {
    if (this.viewer == null) {
      return;
    }

    try {
      this.viewer.close();
    } catch (NullPointerException e) {
      System.err.println("Bug graphstream viewer.close() work-around -" + " https://github.com/graphstream/gs-core/issues/150"); 
    }
    this.viewer = null;
  }

  private synchronized void openGui() {
    this.viewer = new FxViewer(this.worldGraph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
    viewer.enableAutoLayout();
    viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
    viewer.addDefaultView(true);

    this.worldGraph.display();
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
        if (
          ((String) newnode.getAttribute("ui.class")) == MapAttribute.closed.toString()
          || currentNode.getNodeContent().toString() == MapAttribute.closed.toString()
        ) {
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

  // hasOpenNode checks if any unexplored nodes remain in the graph.
  // Used to determine if exploration should continue.
  public boolean hasOpenNode() {
    return (this.worldGraph
      .nodes()
      .filter(currentNode -> currentNode.getAttribute("ui.class") == MapAttribute.open.toString())
      .findAny()
    ).isPresent();
  }
}
