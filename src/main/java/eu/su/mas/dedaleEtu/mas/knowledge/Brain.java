package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.serializableGraph.SerializableNode;
import dataStructures.tuple.Couple;
import java.io.Serializable;
import jade.core.Agent;
import eu.su.mas.dedale.env.Location;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class Brain implements Serializable {
  private static final long serialVersionUID = -1333959882640838272L;

  private String name;
  public AgentMind mind;
  public WorldMap map;
  public EntityTracker entities;

  private BrainVisualization visualization;

  public Brain(String name) {
    this.name = name;
    this.mind = new AgentMind(this);
    this.map = new WorldMap(this);
    this.entities = new EntityTracker(this);
  }

  public synchronized void attachVisualization(BrainVisualization visualization) {
    this.visualization = visualization;
    notifyVisualization();
  }

  public synchronized void observe(Agent agent) {
    String position = ((AbstractDedaleAgent) agent).getCurrentPosition().getLocationId();
    this.entities.updatePosition(position);

    List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) agent).observe();
    this.map.addNode(position, MapAttribute.CLOSED);

    Map<String, String> observedAgents = new HashMap<>();

    String nextNodeId = null;
    for (Couple<Location, List<Couple<Observation, String>>> entry : observations) {
      String accessibleNode = entry.getLeft().getLocationId();

      // add new node to map representation
      boolean isNewNode = this.map.addNewNode(accessibleNode);
      if (!position.equals(accessibleNode)) {
        this.map.addEdge(position, accessibleNode);
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
            observedAgents.put(observed, accessibleNode);
            if (observed.startsWith("Silo")) {
              // this.map.setSiloPosition(accessibleNode);
            } else if (observed.startsWith("Golem")) {
              // this.map.setGolemPosition(accessibleNode);
            } else {
              // this.map.updateAgentsPosition(observed, accessibleNode);
            }
            break;

          case GOLD:
          case DIAMOND:
            System.out.println("Found treasure : " + observed);
            int treasureValue = Integer.parseInt(observed);
            // this.map.addTreasure(accessibleNode, observeKind, treasureValue, -1, -1);
            break;

          default:
            assert false : "Unhandled observation type: " + observeKind;
        }
      }
    }

    boolean isHere, shouldBeHere;
    for (Map.Entry<String, AgentData> entry : this.entities.getAgents().entrySet()) {
      String agentName = entry.getKey();
      String agentPosition = entry.getValue().getPosition();

      isHere = false;
      shouldBeHere = false;
      for (Map.Entry<String, String> neighbour : observedAgents.entrySet()) {
        if (agentName == neighbour.getKey()) {
          isHere = true;
        }
        if (agentPosition == neighbour.getValue()) {
          shouldBeHere = true;
        }
      }

      if (this.entities.isAgentMissing(entry.getKey(), entry.getValue().getPosition(),
          observedAgents)) {
        System.out.println(entry.getKey() + " was supposed to be here but isn't...");
        // this.entities.updateAgentsPosition(agentName, null);
      }
    }

    // if (this.silo != null && agentMissing("Silo", this.silo.getPosition(),
    // observedAgents)) {
    // this.silo.setPosition(null);
    // }

    // if (this.golem != null && agentMissing("Golem", this.golem.getPosition(),
    // observedAgents)) {
    // this.golem.setPosition(null);
    // }
  }

  public void notifyVisualization() {
    if (this.visualization != null) {
      this.visualization.updateFromModel(this);
    }
  }

  public void createVisualization() {
    try {
      this.visualization = new BrainVisualization(this.name);
      if (this.visualization.initialize()) {
        attachVisualization(this.visualization);
      }
    } catch (Exception e) {
      System.err.println("Failed to create visualization: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void beforeMove() {
    this.map.beforeMove();
  }

  public void afterMove() {
    this.map.afterMove();
  }
}
