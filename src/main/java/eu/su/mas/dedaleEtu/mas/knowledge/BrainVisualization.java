package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import org.graphstream.ui.view.View;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.graph.Node;
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

  // private Label infoLabel;
  private Stage stage;
  private SplitPane splitPane;

  private VBox infoContent;

  private Label agentHeaderLabel;
  private VBox mindSection;
  private VBox entitiesSection;

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
    List<Node> nodesList = new ArrayList<>();
    // for (Node node : graph.getNodeSet()) {
    // nodesList.add(node);
    // }
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
    Platform.runLater(() -> {

      Label explorationPriority = new Label(
          "Exploration priority : " + String.valueOf(brain.mind.getExplorationPriority()));
      Label collectionPriority = new Label(
          "Collection priority : " + String.valueOf(brain.mind.getCollectionPriority()));
      Label socialCooldown = new Label(String.valueOf("Social cooldown : " + brain.mind.getSocialCooldown()));
      Label stuckCounter = new Label(String.valueOf("Stuck counter : " + brain.mind.getStuckCounter()));
      Label targetNode = new Label(String.valueOf("Target node : " + brain.mind.getTargetNode()));
      Label pathToTarget = new Label(String.valueOf("Path to target : " + brain.mind.getPathToTarget()));

      mindSection.getChildren().clear();
      mindSection.getChildren().addAll(
          explorationPriority,
          collectionPriority,
          socialCooldown,
          stuckCounter,
          targetNode,
          pathToTarget);

      List<Label> entities = new ArrayList<>();

      for (Map.Entry<String, TreasureData> treasure : brain.entities.getTreasures().entrySet()) {
        entities.add(new Label(String.format(
            "Treasure %s - age: %d, quantity: %d, locked: %s, lock: %d, pick: %d",
            treasure.getKey(),
            treasure.getValue().getUpdateCounter(),
            treasure.getValue().getQuantity(),
            treasure.getValue().isLocked() ? "true" : "false",
            treasure.getValue().getLockStrength(),
            treasure.getValue().getPickStrength())));
      }

      if (brain.entities.getMyself() != null) {
        entities.add(new Label(String.format(
            "Me (%s) - position: %s, capacity: %d, freespace: %d",
            brain.name,
            brain.entities.getMyself().getPosition(),
            brain.entities.getMyself().getBackpackCapacity(),
            brain.entities.getMyself().getBackpackFreeSpace())));
      }

      for (Map.Entry<String, AgentData> agent : brain.entities.getAgents().entrySet()) {
        entities.add(new Label(String.format(
            "%s - position: %s, age: %d, capacity: %d, freespace: %d",
            agent.getKey(),
            agent.getValue().getPosition(),
            agent.getValue().getUpdateCounter(),
            agent.getValue().getBackpackCapacity(),
            agent.getValue().getBackpackFreeSpace())));
      }

      if (brain.entities.getSilo() != null) {
        entities.add(new Label(String.format(
            "Silo - position: %s, age: %d",
            brain.entities.getSilo().getPosition(),
            brain.entities.getSilo().getUpdateCounter())));
      }

      if (brain.entities.getGolem() != null) {
        entities.add(new Label(String.format(
            "Golem - position: %s, age: %d",
            brain.entities.getGolem().getPosition(),
            brain.entities.getGolem().getUpdateCounter())));
      }

      entitiesSection.getChildren().clear();
      entitiesSection.getChildren().addAll(entities);
    });
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
