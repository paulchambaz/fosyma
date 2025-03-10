package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.serializableGraph.*;
import dataStructures.tuple.Couple;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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

/**
 * This simple topology representation only deals with the graph, not its content.</br> The
 * knowledge representation is not well written (at all), it is just given as a minimal
 * example.</br> The viewer methods are not independent of the data structure, and the dijkstra is
 * recomputed every-time.
 *
 * @author hc
 */
public class MapRepresentation implements Serializable {

  /**
   * A node is open, closed, or agent
   *
   * @author hc
   */
  public enum MapAttribute {
    agent,
    open,
    closed;
  }

  private static final long serialVersionUID = -1333959882640838272L;

  /*********************************
   * Parameters for graph rendering
   ********************************/

  private String defaultNodeStyle =
      "node {fill-color: black; size-mode:fit;text-alignment:under;"
          + " text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";

  private String nodeStyle_open = "node.agent {" + "fill-color: forestgreen;" + "}";
  private String nodeStyle_agent = "node.open {" + "fill-color: blue;" + "}";
  private String nodeStyle = defaultNodeStyle + nodeStyle_agent + nodeStyle_open;

  private Graph g; // data structure non serializable
  private Viewer viewer; // ref to the display,  non serializable
  private Integer nbEdges; // used to generate the edges ids

  private Map<String, TreasureData> treasures;
  private Map<String, AgentData> agents;
  private String siloPosition;

  private SerializableSimpleGraph<String, MapAttribute>
      sg; // used as a temporary dataStructure during migration

  public MapRepresentation() {
    // System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    System.setProperty("org.graphstream.ui", "javafx");
    this.g = new SingleGraph("My world vision");
    this.g.setAttribute("ui.stylesheet", nodeStyle);

    Platform.runLater(
        () -> {
          openGui();
        });
    // this.viewer = this.g.display();

    this.nbEdges = 0;

    this.treasures = new HashMap<>();
    this.agents = new HashMap<>();
    this.siloPosition = null;
  }

  /**
   * Add or replace a node and its attribute
   *
   * @param id unique identifier of the node
   * @param mapAttribute attribute to process
   */
  public synchronized void addNode(String id, MapAttribute mapAttribute) {
    Node n;
    if (this.g.getNode(id) == null) {
      n = this.g.addNode(id);
    } else {
      n = this.g.getNode(id);
    }
    n.clearAttributes();
    n.setAttribute("ui.class", mapAttribute.toString());
    n.setAttribute("ui.label", id);
  }

  /**
   * Add a node to the graph. Do nothing if the node already exists. If new, it is labeled as open
   * (non-visited)
   *
   * @param id id of the node
   * @return true if added
   */
  public synchronized boolean addNewNode(String id) {
    if (this.g.getNode(id) == null) {
      addNode(id, MapAttribute.open);
      return true;
    }
    return false;
  }

  /**
   * Add an undirect edge if not already existing.
   *
   * @param idNode1 unique identifier of node1
   * @param idNode2 unique identifier of node2
   */
  public synchronized void addEdge(String idNode1, String idNode2) {
    this.nbEdges++;
    try {
      this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
    } catch (IdAlreadyInUseException e1) {
      System.err.println("ID existing");
      System.exit(1);
    } catch (EdgeRejectedException e2) {
      this.nbEdges--;
    } catch (ElementNotFoundException e3) {

    }
  }

  public synchronized void addTreasure(String nodeId, Observation type, int quantity, int lockStrength, int pickStrength) {
      TreasureData treasure = new TreasureData(type, quantity, false, lockStrength, pickStrength);
      this.treasures.put(nodeId, treasure);
      
      // Mettre à jour le style du nœud pour indiquer qu'il contient un trésor
      if (this.g != null && this.g.getNode(nodeId) != null) {
          Node n = this.g.getNode(nodeId);
          n.setAttribute("ui.style", "fill-color: yellow;");
          n.setAttribute("ui.label", nodeId + "-" + type);
      }
  }

  public synchronized void updateTreasureState(String nodeId, boolean isOpen) {
    if (this.treasures.containsKey(nodeId)) {
      this.treasures.get(nodeId).setOpen(isOpen);
    }
  }

  public synchronized void updateTreasureQuantity(String nodeId, int quantityPicked) {
    if (this.treasures.containsKey(nodeId)) {
      this.treasures.get(nodeId).decreaseQuantity(quantityPicked);
    }
  }

  public synchronized boolean hasTreasure(String nodeId) {
    return this.treasures.containsKey(nodeId) && this.treasures.get(nodeId).getQuantity() > 0;
  }

  public synchronized TreasureData getTreasureData(String nodeId) {
    return this.treasures.get(nodeId);
  }

  public synchronized List<String> getNodesWithTreasureType(Observation type) {
    return this.treasures.entrySet().stream()
      .filter(entry -> entry.getValue().getType() == type && entry.getValue().getQuantity() > 0)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  public synchronized void updateAgentPosition(String agentName, String nodeId) {
    if (!this.agents.containsKey(agentName)) {
      this.agents.put(agentName, new AgentData(nodeId));
    } else{
      this.agents.get(agentName).setPosition(nodeId);
    }
  }

  public synchronized void updateAgentExpertise(String agentName, Map<Observation, Integer> expertise) {
    if (!this.agents.containsKey(agentName)) {
      AgentData data = new AgentData(null);
      data.setExpertise(expertise);
      this.agents.put(agentName, data);
    } else {
      this.agents.get(agentName).setExpertise(expertise);
    }
  }

  public synchronized void updateAgentBackpack(String agentName, int capacity, int freespace) {
    if (!this.agents.containsKey(agentName)) {
      AgentData data = new AgentData(null);
      data.setBackpackCapacity(capacity);
      data.setBackpackFreespace(freespace);
      this.agents.put(agentName, data);
    } else {
      AgentData data = this.agents.get(agentName);
      data.setBackpackCapacity(capacity);
      data.setBackpackFreespace(freespace);
    }
  }

  public synchronized AgentData getAgentData(String agentName) {
    return this.agents.get(agentName);
  }

  public synchronized List<String> getAgentsAtPosition(String nodeId) {
      return this.agents.entrySet().stream()
              .filter(entry -> nodeId.equals(entry.getValue().getPosition()))
              .map(Map.Entry::getKey)
              .collect(Collectors.toList());
  }

  public synchronized void setSiloPosition(String nodeId) {
    this.siloPosition = nodeId;

    if (this.g != null && this.g.getNode(nodeId) != null) {
        Node n = this.g.getNode(nodeId);
        n.setAttribute("ui.style", "fill-color: orange; size: 20px;");
        n.setAttribute("ui.label", "SILO");
    }
  }

  public synchronized String getSiloPosition() {
    return this.siloPosition;
  }

  public synchronized List<String> getShortestPathToSilo(String currentPosition) {
    if (siloPosition == null) return null;
    return getShortestPath(currentPosition, siloPosition);
  }

  /**
   * Compute the shortest Path from idFrom to IdTo. The computation is currently not very efficient
   *
   * @param idFrom id of the origin node
   * @param idTo id of the destination node
   * @return the list of nodes to follow, null if the targeted node is not currently reachable
   */
  public synchronized List<String> getShortestPath(String idFrom, String idTo) {
    List<String> shortestPath = new ArrayList<String>();

    Dijkstra dijkstra = new Dijkstra(); // number of edge
    dijkstra.init(g);
    dijkstra.setSource(g.getNode(idFrom));
    dijkstra.compute(); // compute the distance to all nodes from idFrom
    List<Node> path =
        dijkstra.getPath(g.getNode(idTo)).getNodePath(); // the shortest path from idFrom to idTo
    Iterator<Node> iter = path.iterator();
    while (iter.hasNext()) {
      shortestPath.add(iter.next().getId());
    }
    dijkstra.clear();
    if (shortestPath.isEmpty()) { // The openNode is not currently reachable
      return null;
    } else {
      shortestPath.remove(0); // remove the current position
    }
    return shortestPath;
  }

  public List<String> getShortestPathToClosestOpenNode(String myPosition) {
    // 1) Get all openNodes
    List<String> opennodes = getOpenNodes();

    // 2) select the closest one
    List<Couple<String, Integer>> lc =
        opennodes.stream()
            .map(
                on ->
                    (getShortestPath(myPosition, on) != null)
                        ? new Couple<String, Integer>(on, getShortestPath(myPosition, on).size())
                        : new Couple<String, Integer>(
                            on,
                            Integer.MAX_VALUE)) // some nodes my be unreachable if the agents do not
            // share at least one common node.
            .collect(Collectors.toList());

    Optional<Couple<String, Integer>> closest =
        lc.stream().min(Comparator.comparing(Couple::getRight));
    // 3) Compute shorterPath

    return getShortestPath(myPosition, closest.get().getLeft());
  }

  public List<String> getOpenNodes() {
    return this.g
        .nodes()
        .filter(x -> x.getAttribute("ui.class") == MapAttribute.open.toString())
        .map(Node::getId)
        .collect(Collectors.toList());
  }

  /**
   * Before the migration we kill all non serializable components and store their data in a
   * serializable form
   */
  public void prepareMigration() {
    serializeGraphTopology();

    closeGui();

    this.g = null;
  }

  /** Before sending the agent knowledge of the map it should be serialized. */
  private void serializeGraphTopology() {
    this.sg = new SerializableSimpleGraph<String, MapAttribute>();

    Iterator<Node> iter = this.g.iterator();
    while (iter.hasNext()) {
      Node n = iter.next();
      String nodeId = n.getId();

      sg.addNode(nodeId, MapAttribute.valueOf((String) n.getAttribute("ui.class")));

      if (treasures.containsKey(nodeId)) {
        sg.getNode(nodeId).setAttribute("treasure", treasures.get(nodeId));
      }

      List<String> agentsHere = getAgentsAtPosition(nodeId);
      if (!agentsHere.isEmpty()) {
        sg.getNode(nodeId).setAttribute("agents", agentsHere);
      }

      if (nodeId.equals(siloPosition)) {
        sg.getNode(nodeId).setAttributes("silo", true);
      }

    }

    Iterator<Edge> iterE = this.g.edges().iterator();
    while (iterE.hasNext()) {
      Edge e = iterE.next();
      Node sn = e.getSourceNode();
      Node tn = e.getTargetNode();
      sg.addEdge(e.getId(), sn.getId(), tn.getId());
    }

    sg.setAttribute("agents_data", this.agents);
  }

  public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializableGraph() {
    serializeGraphTopology();
    return this.sg;
  }

  /**
   * After migration we load the serialized data and recreate the non serializable components
   * (Gui,..)
   */
  public synchronized void loadSavedData() {

    this.g = new SingleGraph("My world vision");
    this.g.setAttribute("ui.stylesheet", nodeStyle);

    openGui();

    Integer nbEd = 0;
    for (SerializableNode<String, MapAttribute> n : this.sg.getAllNodes()) {
      String nodeId = n.getNodeId();

      this.g.addNode(nodeId).setAttribute("ui.class", n.getNodeContent().toString());

      for (String s : this.sg.getEdges(nodeId)) {
        this.g.addEdge(nbEd.toString(), nodeId, s);
        nbEd++;
      }

      if (n.getAttribute("treasures") != null) {
        treasure.put(nodeId, (TreasureData) n.getAttribute("treasure"));
      }

      if (n.Attribute("agents") != null) {
        List<String> agentsHere = (List<String>) n.getAttribute("agents");
        for (String agentName : agentsHere) {
          if (agents.containsKey(agentName)) {
            agents.get(agentName).setPosition(nodeId);
          }
        }
      }

      if (n.getAttribute("silo") != null && (Boolean) n.getAttribute("silo")) {
        siloPosition = nodeId;
      }
    }

    if (sg.getAttribute("agents_data") != null) {
      Map<String, AgentData> loadedAgents = (Map<String, AgentData>) sg.getAttribute("agents_data");
      this.agents.putAll(loadedAgents);
    }

    System.out.println("Loading done");
  }

  /** Method called before migration to kill all non serializable graphStream components */
  private synchronized void closeGui() {
    // once the graph is saved, clear non serializable components
    if (this.viewer != null) {
      // Platform.runLater(() -> {
      try {
        this.viewer.close();
      } catch (NullPointerException e) {
        System.err.println(
            "Bug graphstream viewer.close() work-around -"
                + " https://github.com/graphstream/gs-core/issues/150");
      }
      // });
      this.viewer = null;
    }
  }

  /** Method called after a migration to reopen GUI components */
  private synchronized void openGui() {
    this.viewer =
        new FxViewer(
            this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD); // GRAPH_IN_GUI_THREAD)
    viewer.enableAutoLayout();
    viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
    viewer.addDefaultView(true);

    g.display();
  }

  /**
  * Merges a received serializable graph with the current graph.
  * This includes topology (nodes and edges) as well as all additional data
  * (treasures, agents positions, silo location).
  *
  * @param sgreceived The serializable graph received from another agent
  */
  public void mergeMap(SerializableSimpleGraph<String, MapAttribute> sgreceived) {
     // First, merge the topology (nodes and edges)
     for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
         boolean alreadyIn = false;
         // 1. Add the node if it doesn't exist
         Node newnode = null;
         try {
             newnode = this.g.addNode(n.getNodeId());
         } catch (IdAlreadyInUseException e) {
             alreadyIn = true;
         }
         
         if (!alreadyIn) {
             newnode.setAttribute("ui.label", newnode.getId());
             newnode.setAttribute("ui.class", n.getNodeContent().toString());
         } else {
             newnode = this.g.getNode(n.getNodeId());
             // 2. Update node attribute if necessary
             // If either the current node or received node is closed, mark it as closed
             if (((String) newnode.getAttribute("ui.class")) == MapAttribute.closed.toString()
                 || n.getNodeContent().toString() == MapAttribute.closed.toString()) {
                 newnode.setAttribute("ui.class", MapAttribute.closed.toString());
             }
         }
         
         // 3. Merge treasure data if present in the received node
         if (n.getAttribute("treasure") != null) {
             TreasureData incomingTreasure = (TreasureData) n.getAttribute("treasure");
             String nodeId = n.getNodeId();
             
             // Add treasure if not already known or update if more recent information
             if (!treasures.containsKey(nodeId) || 
                 treasures.get(nodeId).getQuantity() < incomingTreasure.getQuantity()) {
                 treasures.put(nodeId, incomingTreasure);
                 
                 // Update node appearance to show it contains a treasure
                 if (newnode != null) {
                     newnode.setAttribute("ui.style", "fill-color: yellow;");
                     newnode.setAttribute("ui.label", nodeId + "-" + incomingTreasure.getType());
                 }
             }
         }
         
         // 4. Update silo position if present in the received node
         if (n.getAttribute("silo") != null && (Boolean) n.getAttribute("silo") && siloPosition == null) {
             siloPosition = n.getNodeId();
             
             // Update node appearance to show it's the silo
             if (newnode != null) {
                 newnode.setAttribute("ui.style", "fill-color: orange; size: 20px;");
                 newnode.setAttribute("ui.label", "SILO");
             }
         }
     }
     
     // 5. Add all edges from the received graph
     for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
         for (String s : sgreceived.getEdges(n.getNodeId())) {
             addEdge(n.getNodeId(), s);
         }
     }
     
     // 6. Merge agent data if available
     if (sgreceived.getAttribute("agents_data") != null) {
         @SuppressWarnings("unchecked")
         Map<String, AgentData> incomingAgents = (Map<String, AgentData>) sgreceived.getAttribute("agents_data");
         
         // For each incoming agent, update information if it's more recent
         for (Map.Entry<String, AgentData> entry : incomingAgents.entrySet()) {
             String agentName = entry.getKey();
             AgentData incomingData = entry.getValue();
             
             if (!agents.containsKey(agentName)) {
                 // Add new agent data
                 agents.put(agentName, incomingData);
             } else {
                 // Update existing agent data
                 // For simplicity, we assume position data is most important to update
                 // In a more sophisticated implementation, we could use timestamps
                 // to determine which information is most recent
                 if (incomingData.getPosition() != null) {
                     agents.get(agentName).setPosition(incomingData.getPosition());
                 }
                 
                 // Only update expertise if it contains more information
                 if (!incomingData.getExpertise().isEmpty() && 
                     agents.get(agentName).getExpertise().isEmpty()) {
                     agents.get(agentName).setExpertise(incomingData.getExpertise());
                 }
                 
                 // Update backpack info if available
                 if (incomingData.getBackpackCapacity() > 0) {
                     agents.get(agentName).setBackpackCapacity(incomingData.getBackpackCapacity());
                     agents.get(agentName).setBackpackFreespace(incomingData.getBackpackFreespace());
                 }
                 
                 // Update treasure type if available
                 if (incomingData.getTreasureType() != null) {
                     agents.get(agentName).setTreasureType(incomingData.getTreasureType());
                 }
             }
         }
     }
  }

  /**
   * @return true if there exist at least one openNode on the graph
   */
  public boolean hasOpenNode() {
    return (this.g
            .nodes()
            .filter(n -> n.getAttribute("ui.class") == MapAttribute.open.toString())
            .findAny())
        .isPresent();
  }
}
