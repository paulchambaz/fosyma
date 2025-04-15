package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.concurrent.atomic.AtomicBoolean;
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
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.graph.Graph;

class BrainVisualization {
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
  // private Label infoLabel;
  private Stage stage;
  private SplitPane splitPane;

  public BrainVisualization(String agentName) {
    this.agentName = agentName;
  }

  public boolean initialize() {
    if (isInitialized.get()) {
      return true;
    }

    try {
      System.setProperty("org.graphstream.ui", "javafx");
      Platform.runLater(() -> {
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
    stage.setTitle("Brain Map - Agent: " + agentName);

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

  public void updateFromModel(Brain brain) {
    if (!isInitialized.get()) {
      return;
    }

    if (!isViewerActive || viewer == null) {
      Graph graph = brain.map.getGraph();
      if (graph != null) {
        createOrUpdateViewer(graph, brain);
      }
    } else {
      updateExistingVisualization(brain);
    }
  }

  private void createOrUpdateViewer(Graph graph, Brain brain) {
    graph.setAttribute("ui.stylesheet", DEFAULT_STYLESHEET);

    styleNodes(graph, brain);
    styleEdgesForPath(graph, brain);
    updateInfoPanel(brain);

    Platform.runLater(() -> {
      try {
        openGui(graph);
      } catch (Exception e) {
        System.err.println("Error opening GUI: " + e.getMessage());
        e.printStackTrace();
      }
    });
  }

  private void updateExistingVisualization(Brain brain) {
    Graph graph = brain.map.getGraph();
    if (graph == null) {
      return;
    }

    Platform.runLater(() -> {
      try {
        styleNodes(graph, brain);
        styleEdgesForPath(graph, brain);
        updateInfoPanel(brain);
      } catch (Exception e) {
        System.err.println("Error updating visualization: " + e.getMessage());
        e.printStackTrace();
      }
    });
  }

  private void styleNodes(Graph graph, Brain brain) {
    // graph.nodes().forEach(node -> {
    // String nodeId = node.getId();
    //
    // // Set default label
    // node.setAttribute("ui.label", nodeId);
    //
    // // Get node class (default to closed if not open)
    // String nodeClass = (String) node.getAttribute("ui.class");
    // if (nodeClass == null || (!nodeClass.equals("open") &&
    // !nodeClass.equals("closed"))) {
    // nodeClass = "closed";
    // }
    //
    // // Determine what's at this node
    // boolean hasTreasure = brain.entities.hasTreasure(nodeId);
    // String agentHere = brain.entities.getAgentAtPosition(nodeId);
    // boolean isSilo = nodeId.equals(brain.entities.getSiloPosition());
    // boolean isGolem = nodeId.equals(brain.entities.getGolemPosition());
    // boolean isCurrentPosition =
    // nodeId.equals(brain.entities.getAgentData().getPosition());
    //
    // // Set node class and label based on content
    // if (isCurrentPosition) {
    // // Current agent position takes precedence
    // node.setAttribute("ui.class", "me");
    //
    // // Include treasure info in label if present
    // if (hasTreasure) {
    // TreasureData treasure = brain.getTreasureData(nodeId);
    // node.setAttribute("ui.label",
    // nodeId + "-" + this.agentName + "-" + treasure.getType() + "(" +
    // treasure.getQuantity() + ")");
    // } else {
    // node.setAttribute("ui.label", nodeId + "-" + this.agentName);
    // }
    // } else if (agentHere != null) {
    // // Other agent is here
    // node.setAttribute("ui.class", "agent");
    //
    // // Include treasure info in label if present
    // if (hasTreasure) {
    // TreasureData treasure = brain.getTreasureData(nodeId);
    // node.setAttribute("ui.label",
    // nodeId + "-" + agentHere + "-" + treasure.getType() + "(" +
    // treasure.getQuantity() + ")");
    // } else {
    // node.setAttribute("ui.label", nodeId + "-" + agentHere);
    // }
    // } else if (isGolem) {
    // // Golem is here
    // node.setAttribute("ui.class", "golem");
    //
    // // Include treasure info in label if present
    // if (hasTreasure) {
    // TreasureData treasure = brain.getTreasureData(nodeId);
    // node.setAttribute("ui.label", nodeId + "-Golem-" + treasure.getType() + "(" +
    // treasure.getQuantity() + ")");
    // } else {
    // node.setAttribute("ui.label", nodeId + "-Golem");
    // }
    // } else if (isSilo) {
    // // Silo is here
    // node.setAttribute("ui.class", "silo");
    //
    // // Include treasure info in label if present
    // if (hasTreasure) {
    // TreasureData treasure = brain.getTreasureData(nodeId);
    // node.setAttribute("ui.label", nodeId + "-Silo-" + treasure.getType() + "(" +
    // treasure.getQuantity() + ")");
    // } else {
    // node.setAttribute("ui.label", nodeId + "-Silo");
    // }
    // } else if (hasTreasure) {
    // // Only treasure is here
    // TreasureData treasure = brain.getTreasureData(nodeId);
    // node.setAttribute("ui.class", "treasure");
    // node.setAttribute("ui.label", nodeId + "-" + treasure.getType() + "(" +
    // treasure.getQuantity() + ")");
    // } else {
    // // Just a regular node (open or closed)
    // node.setAttribute("ui.class", nodeClass);
    // }
    // });
  }

  private void styleEdgesForPath(Graph graph, Brain brain) {
    // graph.edges().forEach(edge -> edge.removeAttribute("ui.class"));
    // if (!brain.mind.getPathToTarget().isEmpty()) {
    // Deque<String> path = new ArrayDeque<>(brain.mind.getPathToTarget());
    // path.addFirst(brain.getAgentData().getPosition());
    //
    // String[] pathArray = path.toArray(new String[0]);
    // for (int i = 0; i < pathArray.length - 1; i++) {
    // String currentNode = pathArray[i];
    // String nextNode = pathArray[i + 1];
    // graph.edges()
    // .filter(edge -> (edge.getSourceNode().getId().equals(currentNode) &&
    // edge.getTargetNode().getId().equals(nextNode)) ||
    // (edge.getSourceNode().getId().equals(nextNode) &&
    // edge.getTargetNode().getId().equals(currentNode)))
    // .forEach(edge -> edge.setAttribute("ui.class", "path"));
    // }
    // }
  }

  private void updateInfoPanel(Brain brain) {
    // // Update the agent header
    // Platform.runLater(() -> {
    // // Update agent name
    // agentHeaderLabel.setText(agentName);
    //
    // // Clear previous content from each section
    // statusSection.getChildren().clear();
    // treasuresSection.getChildren().clear();
    // agentsSection.getChildren().clear();
    // environmentSection.getChildren().clear();
    // pathSection.getChildren().clear();
    //
    // // Update STATUS section
    // Label desiresLabel = new Label(String.format("Desires: Explore=%d
    // Collect=%d",
    // brain.mind.isCollectionPreferred() ? 0 : 1,
    // brain.mind.isCollectionPreferred() ? 1 : 0));
    //
    // Label introvertLabel = new Label("Introvert Counter: " +
    // brain.mind.getSocialCooldown());
    // Label blockLabel = new Label("Block Counter: " +
    // brain.mind.getStuckCounter());
    //
    // // Add agent expertise and backpack info if available
    // AgentData myAgent = brain.getAgentData();
    // if (myAgent != null) {
    // // Add expertise information
    // if (myAgent.getExpertise() != null && !myAgent.getExpertise().isEmpty()) {
    // StringBuilder expertise = new StringBuilder("Expertise: ");
    // for (Map.Entry<Observation, Integer> exp : myAgent.getExpertise().entrySet())
    // {
    // expertise.append(exp.getKey()).append("=").append(exp.getValue()).append("
    // ");
    // }
    // Label expertiseLabel = new Label(expertise.toString().trim());
    // statusSection.getChildren().add(expertiseLabel);
    // }
    //
    // // Add backpack information
    // Label backpackLabel = new Label(String.format("Backpack: %d/%d free",
    // myAgent.getBackpackFreeSpace(), myAgent.getBackpackCapacity()));
    //
    // // Add treasure type if specified
    // if (myAgent.getTreasureType() != null) {
    // Label treasureTypeLabel = new Label("Collects: " +
    // myAgent.getTreasureType());
    // statusSection.getChildren().add(treasureTypeLabel);
    // }
    //
    // statusSection.getChildren().addAll(desiresLabel, introvertLabel, blockLabel,
    // backpackLabel);
    // } else {
    // statusSection.getChildren().addAll(desiresLabel, introvertLabel, blockLabel);
    // }
    //
    // // Update TREASURES section
    // for (Map.Entry<String, TreasureData> entry :
    // brain.getTreasures().entrySet()) {
    // TreasureData treasure = entry.getValue();
    // if (treasure.getQuantity() > 0) {
    // StringBuilder treasureInfo = new StringBuilder();
    // treasureInfo.append(entry.getKey()).append(": ")
    // .append(treasure.getType()).append(" (")
    // .append(treasure.getQuantity()).append(")");
    //
    // // Add lock information if it's locked
    // if (treasure.isLocked()) {
    // treasureInfo.append(" [Locked:
    // ").append(treasure.getLockStrength()).append("]");
    // }
    //
    // // Add pickup strength if needed
    // if (treasure.getPickStrength() > 0) {
    // treasureInfo.append(" [Strength:
    // ").append(treasure.getPickStrength()).append("]");
    // }
    //
    // Label treasureLabel = new Label(treasureInfo.toString());
    // treasuresSection.getChildren().add(treasureLabel);
    // }
    // }
    //
    // // Update AGENTS section
    // for (Map.Entry<String, AgentData> entry : brain.getAgents().entrySet()) {
    // AgentData agent = entry.getValue();
    // StringBuilder agentInfo = new StringBuilder();
    // agentInfo.append(entry.getKey()).append(": ")
    // .append("Pos=").append(agent.getPosition())
    // .append(" Status=").append(agent.getStatus());
    //
    // // Add expertise if available
    // if (agent.getExpertise() != null && !agent.getExpertise().isEmpty()) {
    // agentInfo.append(" Exp=[");
    // for (Map.Entry<Observation, Integer> exp : agent.getExpertise().entrySet()) {
    // agentInfo.append(exp.getKey()).append("=").append(exp.getValue()).append("
    // ");
    // }
    // agentInfo.append("]");
    // }
    //
    // Label agentLabel = new Label(agentInfo.toString());
    // agentsSection.getChildren().add(agentLabel);
    // }
    //
    // // Update ENVIRONMENT section
    // if (brain.getSilo() != null) {
    // Label siloLabel = new Label("Silo: " + brain.getSiloPosition() +
    // " (Age: " + brain.getSiloUpdateCounter() + ")");
    // environmentSection.getChildren().add(siloLabel);
    // }
    //
    // if (brain.getGolem() != null) {
    // Label golemLabel = new Label("Golem: " + brain.getGolemPosition() +
    // " (Age: " + brain.getGolemUpdateCounter() + ")");
    // environmentSection.getChildren().add(golemLabel);
    // }
    //
    // // Update PATH section
    // if (!brain.mind.getPathToTarget().isEmpty()) {
    // StringBuilder pathInfo = new StringBuilder("Path to closest treasure: ");
    // for (String nodeId : brain.mind.getPathToTarget()) {
    // pathInfo.append(nodeId).append(" → ");
    // }
    // if (pathInfo.length() > 2) {
    // pathInfo.delete(pathInfo.length() - 3, pathInfo.length());
    // }
    //
    // Label pathLabel = new Label(pathInfo.toString());
    // pathLabel.setWrapText(true);
    // pathSection.getChildren().add(pathLabel);
    // }
    // });
  }

  private void openGui(Graph graph) {
    try {
      if (this.viewer == null) {
        this.viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        this.viewer.enableAutoLayout();
        this.viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);

        View view = this.viewer.addDefaultView(false);

        Platform.runLater(() -> {
          if (splitPane.getItems().size() == 1) {
            splitPane.getItems().add((javafx.scene.Node) view);
            splitPane.setDividerPositions(0.3);
            if (!stage.isShowing()) {
              stage.show();
            }
          }
        });
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
          if (stage != null) {
            stage.close();
          }
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
