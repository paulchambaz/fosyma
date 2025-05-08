package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import org.graphstream.ui.view.View;

import eu.su.mas.dedaleEtu.princ.Communication;

import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

public class BrainVisualization {
  private static final long UPDATE_THROTTLE_MS = 33;

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
      "node.target { fill-color: red; size: 15px; }" +
      "node.treasure { fill-color: yellow; }" +
      "node.agent { fill-color: forestgreen; size: 10px; }" +
      "node.silo { fill-color: orange; size: 10px; }" +
      "node.golem { fill-color: red; size: 10px; }" +
      "edge { fill-color: #999; }" +
      "edge.path { fill-color: green; size: 4px; }";

  private long lastUpdateTime = 0;
  private boolean initialized = false;

  final private Brain parentBrain;
  private final String agentName;

  // UI components
  private FxViewer viewer;
  private Graph uiGraph;
  private Stage stage;
  private SplitPane splitPane;
  private VBox infoContent;
  private VBox mindSection;
  private VBox entitiesSection;
  private Label agentHeaderLabel;

  // A single dedicated thread for UI updates
  private final BlockingQueue<Void> updateSignal;
  private volatile boolean running = true;

  public BrainVisualization(Brain brain, String agentName) {
    this.parentBrain = brain;
    this.agentName = agentName;
    this.updateSignal = new LinkedBlockingQueue<>(1);
  }

  public synchronized boolean initialize() {
    if (initialized) {
      return true;
    }

    try {
      System.setProperty("org.graphstream.ui", "javafx");

      final CountDownLatch latch = new CountDownLatch(1);

      Platform.runLater(() -> {
        try {
          initializeLayout();
          startUpdateThread();
          initialized = true;
          latch.countDown();
        } catch (Exception e) {
          System.err.println("Error in JavaFX initialization: " + e.getMessage());
          latch.countDown();
        }
      });

      if (!latch.await(5, TimeUnit.SECONDS)) {
        System.err.println("Timeout waiting for JavaFX initialization");
        return false;
      }

      return initialized;
    } catch (Exception e) {
      System.err.println("Failed to initialize visualization: " + e.getMessage());
      return false;
    }
  }

  private void startUpdateThread() {
    Thread updateThread = new Thread(() -> {
      while (running) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= UPDATE_THROTTLE_MS) {
          lastUpdateTime = currentTime;

          final BrainSnapshot snapshot = createBrainSnapshot(parentBrain);

          Platform.runLater(() -> {
            try {
              if (viewer == null) {
                createVisualization(snapshot);
              } else {
                updateVisualization(snapshot);
              }
            } catch (Exception e) {
              System.err.println("Error updating visualization: " + e.getMessage());
            }
          });
        }
      }
    });

    updateThread.setDaemon(true);
    updateThread.setName("BrainVisualization-UpdateThread");
    updateThread.start();
  }

  public void updateFromModel() {
    if (!initialized || updateSignal == null)
      return;

    try {
      updateSignal.clear();
      updateSignal.offer(null);
    } catch (NullPointerException e) {
    }
  }

  private void initializeLayout() {
    stage = new Stage();
    stage.setTitle("Brain of " + agentName);

    splitPane = new SplitPane();
    splitPane.setOrientation(Orientation.HORIZONTAL);

    infoContent = new VBox(10);
    infoContent.setPadding(new Insets(15));
    infoContent.setStyle("-fx-background-color: #f4f4f4;");

    agentHeaderLabel = new Label(agentName);
    agentHeaderLabel.setStyle("-fx-font-size: 18px; -fx-font-style: italic; -fx-font-weight: bold;");
    agentHeaderLabel.setMaxWidth(Double.MAX_VALUE);
    agentHeaderLabel.setAlignment(javafx.geometry.Pos.CENTER);
    infoContent.getChildren().add(agentHeaderLabel);

    Label mindHeader = createSectionHeader("Mind");
    mindSection = new VBox(5);
    infoContent.getChildren().addAll(mindHeader, mindSection);

    Label entitiesHeader = createSectionHeader("Entities");
    entitiesSection = new VBox(5);
    infoContent.getChildren().addAll(entitiesHeader, entitiesSection);

    ScrollPane infoScrollPane = new ScrollPane(infoContent);
    infoScrollPane.setFitToWidth(true);
    infoScrollPane.setPrefWidth(300);
    infoScrollPane.setStyle("-fx-background: #f4f4f4; -fx-background-color: #f4f4f4;");

    splitPane.getItems().add(infoScrollPane);

    Scene scene = new Scene(splitPane, 1000, 700);
    stage.setScene(scene);

    stage.setOnCloseRequest(event -> {
      close();
    });
  }

  private Label createSectionHeader(String text) {
    Label header = new Label(text);
    header.setStyle("-fx-font-weight: bold; -fx-font-style: italic;");
    header.setMaxWidth(Double.MAX_VALUE);
    header.setAlignment(javafx.geometry.Pos.CENTER);
    header.setPadding(new Insets(5, 0, 5, 0));
    return header;
  }

  private BrainSnapshot createBrainSnapshot(Brain brain) {
    BrainSnapshot snapshot = new BrainSnapshot();

    // Copy all necessary data directly from the brain
    snapshot.nodeAttributes = new HashMap<>(brain.map.getNodeAttributes());
    snapshot.pathToTarget = new ArrayList<>(brain.mind.getPathToTarget());
    snapshot.treasures = new HashMap<>(brain.entities.getTreasures());
    snapshot.agents = new HashMap<>(brain.entities.getAgents());
    snapshot.silo = brain.entities.getSilo();
    snapshot.golem = brain.entities.getGolem();
    snapshot.behaviour = brain.mind.getBehaviour();
    snapshot.communication = brain.mind.getCommunication();
    snapshot.explorationPriority = brain.mind.getExplorationPriority();
    snapshot.collectionPriority = brain.mind.getCollectionPriority();
    snapshot.socialCooldown = brain.mind.getSocialCooldown();
    snapshot.stuckCounter = brain.mind.getStuckCounter();
    snapshot.targetNode = brain.mind.getTargetNode();
    snapshot.agentName = brain.name;
    snapshot.myself = brain.entities.getMyself();

    // Copy the graph structure
    Graph originalGraph = brain.map.getGraph();
    if (originalGraph != null) {
      snapshot.nodes = new ArrayList<>();
      snapshot.edges = new ArrayList<>();

      originalGraph.nodes().forEach(node -> {
        snapshot.nodes.add(new NodeInfo(node.getId()));
      });

      originalGraph.edges().forEach(edge -> {
        snapshot.edges.add(new EdgeInfo(
            edge.getId(),
            edge.getSourceNode().getId(),
            edge.getTargetNode().getId()));
      });
    }

    return snapshot;
  }

  private void createVisualization(BrainSnapshot snapshot) {
    uiGraph = new SingleGraph("ui-graph");
    uiGraph.setAttribute("ui.stylesheet", DEFAULT_STYLESHEET);

    // Add all nodes and edges from the snapshot
    for (NodeInfo nodeInfo : snapshot.nodes) {
      uiGraph.addNode(nodeInfo.id);
    }

    for (EdgeInfo edgeInfo : snapshot.edges) {
      try {
        uiGraph.addEdge(edgeInfo.id, edgeInfo.sourceId, edgeInfo.targetId);
      } catch (EdgeRejectedException e) {
        // Edge already exists
      } catch (Exception e) {
        System.err.println("Error adding edge: " + e.getMessage());
      }
    }

    // Apply styles to nodes and edges
    styleNodesFromSnapshot(snapshot);
    styleEdgesForPathFromSnapshot(snapshot);
    updateInfoPanelFromSnapshot(snapshot);

    // Setup and show the viewer
    openGui();
  }

  private void updateVisualization(BrainSnapshot snapshot) {
    if (uiGraph == null) {
      createVisualization(snapshot);
      return;
    }

    // Rebuild the graph to match the snapshot
    synchronizeGraphWithSnapshot(snapshot);

    // Apply styles and update the info panel
    styleNodesFromSnapshot(snapshot);
    styleEdgesForPathFromSnapshot(snapshot);
    updateInfoPanelFromSnapshot(snapshot);
  }

  private void synchronizeGraphWithSnapshot(BrainSnapshot snapshot) {
    Set<String> currentNodeIds = new HashSet<>();
    uiGraph.nodes().forEach(node -> currentNodeIds.add(node.getId()));

    Set<String> newNodeIds = new HashSet<>();
    snapshot.nodes.forEach(nodeInfo -> newNodeIds.add(nodeInfo.id));

    Set<String> nodesToRemove = new HashSet<>(currentNodeIds);
    nodesToRemove.removeAll(newNodeIds);
    for (String nodeId : nodesToRemove) {
      uiGraph.removeNode(nodeId);
    }

    Set<String> nodesToAdd = new HashSet<>(newNodeIds);
    nodesToAdd.removeAll(currentNodeIds);
    for (String nodeId : nodesToAdd) {
      uiGraph.addNode(nodeId);
    }

    Map<String, EdgeInfo> newEdges = new HashMap<>();
    for (EdgeInfo edge : snapshot.edges) {
      newEdges.put(edge.id, edge);
    }

    List<String> edgesToRemove = new ArrayList<>();
    uiGraph.edges().forEach(edge -> {
      if (!newEdges.containsKey(edge.getId())) {
        edgesToRemove.add(edge.getId());
      }
    });

    for (String edgeId : edgesToRemove) {
      uiGraph.removeEdge(edgeId);
    }

    for (EdgeInfo edge : snapshot.edges) {
      if (uiGraph.getEdge(edge.id) == null &&
          uiGraph.getNode(edge.sourceId) != null &&
          uiGraph.getNode(edge.targetId) != null) {
        try {
          uiGraph.addEdge(edge.id, edge.sourceId, edge.targetId);
        } catch (EdgeRejectedException e) {
          // Edge already exists
        } catch (Exception e) {
          System.err.println("Error adding edge: " + e.getMessage());
        }
      }
    }
  }

  private void styleNodesFromSnapshot(BrainSnapshot snapshot) {
    uiGraph.nodes().forEach(node -> {
      String nodeId = node.getId();

      MapAttribute nodeAttribute = snapshot.nodeAttributes.get(nodeId);
      String nodeClass = (nodeAttribute == MapAttribute.OPEN) ? "open" : "closed";

      boolean hasTreasure = snapshot.treasures.containsKey(nodeId);
      String agentHere = null;
      for (Map.Entry<String, AgentData> entry : snapshot.agents.entrySet()) {
        if (nodeId.equals(entry.getValue().getPosition())) {
          agentHere = entry.getKey();
          break;
        }
      }

      boolean isSilo = snapshot.silo != null && nodeId.equals(snapshot.silo.getPosition());
      boolean isGolem = snapshot.golem != null && nodeId.equals(snapshot.golem.getPosition());
      boolean isCurrentPosition = nodeId.equals(snapshot.myself.getPosition());
      boolean isCurrentTarget = nodeId.equals(snapshot.targetNode);

      if (isCurrentPosition) {
        node.setAttribute("ui.class", "me");

        if (hasTreasure) {
          TreasureData treasure = snapshot.treasures.get(nodeId);
          node.setAttribute("ui.label", String.format(
              "%s - %s (%d)", nodeId, treasure.getType(), treasure.getQuantity()));
        } else {
          node.setAttribute("ui.label", nodeId);
        }
      } else if (isCurrentTarget) {
        node.setAttribute("ui.class", "target");

        String entityInfo = "";
        if (agentHere != null) {
          entityInfo = agentHere;
        } else if (isGolem) {
          entityInfo = "Golem";
        } else if (isSilo) {
          entityInfo = "Silo";
        }

        if (hasTreasure) {
          TreasureData treasure = snapshot.treasures.get(nodeId);
          if (!entityInfo.isEmpty()) {
            node.setAttribute("ui.label", String.format(
                "%s %s - %s (%d)", nodeId, entityInfo, treasure.getType(), treasure.getQuantity()));
          } else {
            node.setAttribute("ui.label", String.format(
                "%s - %s (%d)", nodeId, treasure.getType(), treasure.getQuantity()));
          }
        } else {
          if (!entityInfo.isEmpty()) {
            node.setAttribute("ui.label", String.format("%s %s", nodeId, entityInfo));
          } else {
            node.setAttribute("ui.label", nodeId);
          }
        }
      } else if (agentHere != null) {
        node.setAttribute("ui.class", "agent");

        if (hasTreasure) {
          TreasureData treasure = snapshot.treasures.get(nodeId);
          node.setAttribute("ui.label", String.format(
              "%s %s - %s (%d)", nodeId, agentHere, treasure.getType(), treasure.getQuantity()));
        } else {
          node.setAttribute("ui.label", String.format("%s %s", nodeId, agentHere));
        }
      } else if (isGolem) {
        node.setAttribute("ui.class", "golem");

        if (hasTreasure) {
          TreasureData treasure = snapshot.treasures.get(nodeId);
          node.setAttribute("ui.label", String.format(
              "%s %s - %s (%d)", nodeId, "Golem", treasure.getType(), treasure.getQuantity()));
        } else {
          node.setAttribute("ui.label", String.format("%s %s", nodeId, "Golem"));
        }
      } else if (isSilo) {
        node.setAttribute("ui.class", "silo");

        if (hasTreasure) {
          TreasureData treasure = snapshot.treasures.get(nodeId);
          node.setAttribute("ui.label", String.format(
              "%s %s - %s (%d)", nodeId, "Silo", treasure.getType(), treasure.getQuantity()));
        } else {
          node.setAttribute("ui.label", String.format("%s %s", nodeId, "Silo"));
        }
      } else if (hasTreasure) {
        node.setAttribute("ui.class", "treasure");
        TreasureData treasure = snapshot.treasures.get(nodeId);

        node.setAttribute("ui.label", String.format(
            "%s - %s (%d)", nodeId, treasure.getType(), treasure.getQuantity()));
      } else {
        node.setAttribute("ui.label", nodeId);
        node.setAttribute("ui.class", nodeClass);
      }
    });
  }

  private void styleEdgesForPathFromSnapshot(BrainSnapshot snapshot) {
    uiGraph.edges().forEach(edge -> edge.removeAttribute("ui.class"));

    if (snapshot.pathToTarget != null && !snapshot.pathToTarget.isEmpty()) {
      Deque<String> path = new ArrayDeque<>(snapshot.pathToTarget);
      if (snapshot.myself.getPosition() != null) {
        path.addFirst(snapshot.myself.getPosition());
      }

      String[] pathArray = path.toArray(new String[0]);
      for (int i = 0; i < pathArray.length - 1; i++) {
        String currentNode = pathArray[i];
        String nextNode = pathArray[i + 1];
        uiGraph.edges()
            .filter(edge -> (edge.getSourceNode().getId().equals(currentNode) &&
                edge.getTargetNode().getId().equals(nextNode)) ||
                (edge.getSourceNode().getId().equals(nextNode) &&
                    edge.getTargetNode().getId().equals(currentNode)))
            .forEach(edge -> edge.setAttribute("ui.class", "path"));
      }
    }
  }

  private void updateInfoPanelFromSnapshot(BrainSnapshot snapshot) {
    Label behaviour = new Label(String.format("Behaviour: %s", snapshot.behaviour));
    Label explorationPriority = new Label(String.format("Exploration priority: %f", snapshot.explorationPriority));
    Label collectionPriority = new Label(String.format("Collection priority: %f", snapshot.collectionPriority));
    Label socialCooldown = new Label(String.format("Social cooldown: %d", snapshot.socialCooldown));
    Label stuckCounter = new Label(String.format("Stuck counter: %d", snapshot.stuckCounter));
    Label targetNode = new Label(String.format("Target node: %s", snapshot.targetNode));
    Label pathToTarget = new Label(String.format("Path to target: %s", snapshot.pathToTarget));

    Label communication = new Label(
        (snapshot.communication != null)
            ? String.format("Communication: %s - %s - %s", snapshot.communication.getFriend().getLocalName(),
                snapshot.communication.getProtocol(), snapshot.communication.shouldSpeak() ? "true" : "false")
            : "Communication:");

    mindSection.getChildren().clear();
    mindSection.getChildren().addAll(
        behaviour,
        communication,
        explorationPriority,
        collectionPriority,
        socialCooldown,
        stuckCounter,
        targetNode,
        pathToTarget);

    List<Label> entities = new ArrayList<>();

    for (Map.Entry<String, TreasureData> treasure : snapshot.treasures.entrySet()) {
      // parentBrain.log(treasure.getValue().getType());
      entities.add(new Label(String.format(
          "Treasure %s - age: %d, type: %s, quantity: %d, locked: %s, strength: %d, lockpicking: %d",
          treasure.getKey(),
          treasure.getValue().getUpdateCounter(),
          treasure.getValue().getType(),
          treasure.getValue().getQuantity(),
          treasure.getValue().isLocked() ? "true" : "false",
          treasure.getValue().getCarryStrength(),
          treasure.getValue().getLockpickStrength())));
    }

    if (snapshot.myself != null) {
      entities.add(new Label(String.format(
          "Me (%s) - position: %s, gold: %d/%d, diamond: %d/%d, expertise: %s, type: %s",
          snapshot.agentName,
          snapshot.myself.getPosition(),
          snapshot.myself.getGoldAmount(), snapshot.myself.getGoldCapacity(),
          snapshot.myself.getDiamondAmount(), snapshot.myself.getDiamondCapacity(),
          snapshot.myself.getExpertise(),
          snapshot.myself.getTreasureType())));
    }

    for (Map.Entry<String, AgentData> agent : snapshot.agents.entrySet()) {
      entities.add(new Label(String.format(
          "%s - position: %s, age: %d, gold: %d/%d, diamond: %d/%d, expertise: %s, type: %s",
          agent.getKey(),
          agent.getValue().getPosition(),
          agent.getValue().getUpdateCounter(),
          agent.getValue().getGoldAmount(), agent.getValue().getGoldCapacity(),
          agent.getValue().getDiamondAmount(), agent.getValue().getDiamondCapacity(),
          agent.getValue().getExpertise(),
          agent.getValue().getTreasureType())));
    }

    if (snapshot.silo != null) {
      entities.add(new Label(String.format(
          "Silo - position: %s, age: %d",
          snapshot.silo.getPosition(),
          snapshot.silo.getUpdateCounter())));
    }

    if (snapshot.golem != null) {
      entities.add(new Label(String.format(
          "Golem - position: %s, age: %d",
          snapshot.golem.getPosition(),
          snapshot.golem.getUpdateCounter())));
    }

    entitiesSection.getChildren().clear();
    entitiesSection.getChildren().addAll(entities);
  }

  private void openGui() {
    try {
      if (viewer == null) {
        viewer = new FxViewer(uiGraph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        viewer.enableAutoLayout();
        viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);

        View view = viewer.addDefaultView(false);

        if (splitPane.getItems().size() == 1) {
          splitPane.getItems().add((javafx.scene.Node) view);
          splitPane.setDividerPositions(0.3);
          if (!stage.isShowing()) {
            stage.show();
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Error opening GUI: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void close() {
    running = false;

    if (viewer != null) {
      Platform.runLater(() -> {
        try {
          if (stage != null) {
            stage.close();
          }
          viewer.close();
        } catch (NullPointerException e) {
          System.err.println(
              "Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
        } finally {
          viewer = null;
          uiGraph = null;
        }
      });
    }
  }

  public boolean isActive() {
    return viewer != null;
  }

  public boolean isInitialized() {
    return initialized;
  }

  // These inner classes remain the same
  private static class NodeInfo {
    public final String id;

    public NodeInfo(String id) {
      this.id = id;
    }
  }

  private static class EdgeInfo {
    public final String id;
    public final String sourceId;
    public final String targetId;

    public EdgeInfo(String id, String sourceId, String targetId) {
      this.id = id;
      this.sourceId = sourceId;
      this.targetId = targetId;
    }
  }

  private static class BrainSnapshot {
    public List<NodeInfo> nodes;
    public List<EdgeInfo> edges;

    public Map<String, MapAttribute> nodeAttributes;

    public AgentData myself;
    public List<String> pathToTarget;
    public Map<String, TreasureData> treasures;
    public Map<String, AgentData> agents;
    public SiloData silo;
    public GolemData golem;

    public Communication communication;
    public String behaviour;
    public float explorationPriority;
    public float collectionPriority;
    public int socialCooldown;
    public int stuckCounter;
    public String targetNode;
    public String agentName;
  }
}
