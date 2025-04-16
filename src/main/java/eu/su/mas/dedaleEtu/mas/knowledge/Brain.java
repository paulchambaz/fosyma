package eu.su.mas.dedaleEtu.mas.knowledge;

import dataStructures.tuple.Couple;
import java.io.Serializable;
import jade.core.Agent;
import eu.su.mas.dedale.env.Location;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import eu.su.mas.dedaleEtu.princ.Utils;

public class Brain implements Serializable {
  private static final long serialVersionUID = -1333959882640838272L;

  public final String name;
  public final AgentMind mind;
  public final WorldMap map;
  public final EntityTracker entities;

  private BrainVisualization visualization;

  public Brain(String name) {
    this.name = name;
    this.mind = new AgentMind(this);
    this.map = new WorldMap(this);
    this.entities = new EntityTracker(this);
  }

  public synchronized boolean moveTo(Agent agent, String node) {
    Utils.waitFor(agent, 200);
    try {
      ((AbstractDedaleAgent) agent).moveTo(new GsLocation(node));
    } catch (Exception e) {
      return false;
    }
    String position = ((AbstractDedaleAgent) agent).getCurrentPosition().getLocationId();
    if (position != entities.getPosition()) {
      this.entities.updatePosition(position);
      return true;
    }
    return false;
  }

  public synchronized String findClosestOpenNode(boolean excludeOccupied) {
    String position = this.entities.getPosition();
    List<String> occupiedPositions = this.entities.getOccupiedPositions();
    return map.findClosestOpenNode(position, (excludeOccupied) ? occupiedPositions : new ArrayList<>());
  }

  public synchronized void computePathToTarget(boolean excludeOccupied) {
    String position = this.entities.getPosition();
    String target = this.mind.getTargetNode();
    List<String> occupiedPositions = this.entities.getOccupiedPositions();
    List<String> path = map.findShortestPath(position, target,
        (excludeOccupied) ? occupiedPositions : new ArrayList<>());
    mind.setPathToTarget(path);
  }

  public synchronized String findRandomNode() {
    List<String> occupiedPositions = this.entities.getOccupiedPositions();
    return map.findRandomNode(occupiedPositions);
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
              this.entities.setSiloPosition(accessibleNode);
            } else if (observed.startsWith("Golem")) {
              this.entities.setGolemPosition(accessibleNode);
            } else {
              this.entities.updateAgentPosition(observed, accessibleNode);
            }
            break;

          case GOLD:
          case DIAMOND:
            int treasureValue = Integer.parseInt(observed);
            this.entities.updateTreasure(accessibleNode, observeKind, treasureValue, true, -1, -1);
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

  public synchronized void log(Object... args) {
    StringBuilder message = new StringBuilder();

    String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    message.append(timestamp)
        .append(" - ")
        .append(this.name)
        .append(" - ");

    if (args.length > 0) {
      message.append(args[0]);
      for (int i = 1; i < args.length; i++) {
        message.append(" ").append(args[i]);
      }
    }

    System.out.println(message);
  }

  public synchronized void notifyVisualization() {
    if (this.visualization != null) {
      this.visualization.updateFromModel();
    }
  }

  public synchronized void createVisualization() {
    this.visualization = new BrainVisualization(this, this.name);

    if (this.visualization.initialize()) {
      notifyVisualization();
    }
  }

  public synchronized void beforeMove() {
    this.map.beforeMove();
  }

  public synchronized void afterMove() {
    this.map.afterMove();
  }
}
