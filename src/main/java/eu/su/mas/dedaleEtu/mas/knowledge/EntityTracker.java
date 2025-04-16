package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import eu.su.mas.dedale.env.Observation;

public class EntityTracker implements Serializable {
  private static final long serialVersionUID = -2390384720937841984L;

  private final Brain brain;

  private AgentData myself;

  private Map<String, TreasureData> treasures;
  private Map<String, AgentData> agents;
  private SiloData silo;
  private GolemData golem;

  public EntityTracker(Brain brain) {
    this.brain = brain;
    this.myself = null;
    this.treasures = new HashMap<>();
    this.agents = new HashMap<>();
    this.silo = null;
    this.golem = null;
  }

  public synchronized AgentData getMyself() {
    return this.myself;
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

  public synchronized String getPosition() {
    return (this.myself != null) ? this.myself.getPosition() : null;
  }

  public synchronized void updatePosition(String nodeId) {
    if (this.myself == null) {
      this.myself = new AgentData(nodeId);
    } else {
      this.myself.setPosition(nodeId);
    }
    brain.notifyVisualization();
  }

  public synchronized void updateTreasure(String nodeId, Observation type, int quantity, boolean locked,
      int lockStrength,
      int pickStrength) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData treasure = this.treasures.get(nodeId);
      treasure.setNodeId(nodeId);
      treasure.setLocked(locked);
      treasure.setQuantity(quantity);
      treasure.resetCounter();
    } else {
      TreasureData treasure = new TreasureData(nodeId, type, quantity, locked, lockStrength, pickStrength);
      this.treasures.put(nodeId, treasure);
    }
    brain.notifyVisualization();
  }

  private void ageTreasureData() {
    for (TreasureData treasure : this.treasures.values()) {
      treasure.incrementCounter();
    }
    brain.notifyVisualization();
  }

  public synchronized List<String> getNodesWithTreasureType(Observation type) {
    return this.treasures.entrySet().stream()
        .filter(entry -> entry.getValue().getType() == type && entry.getValue().getQuantity() > 0)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  public synchronized boolean hasTreasure(String nodeId) {
    return this.treasures.containsKey(nodeId);
  }

  public synchronized void loseAgentPosition(String agentName) {
    if (this.agents.containsKey(agentName)) {
      AgentData agent = this.agents.get(agentName);
      agent.setPosition(null);
      brain.notifyVisualization();
    }
  }

  public synchronized void updateAgent(String agentName, String nodeId, Map<Observation, Integer> expertise,
      int capacity,
      int freeSpace, String status) {
    if (this.agents.containsKey(agentName)) {
      AgentData agent = this.agents.get(agentName);
      agent.setPosition(nodeId);
      agent.setExpertise(expertise);
      agent.setBackpackCapacity(capacity);
      agent.setBackpackFreeSpace(freeSpace);
      agent.setStatus(status);
      agent.resetCounter();
    } else {
      AgentData agent = new AgentData(nodeId);
      agent.setExpertise(expertise);
      agent.setBackpackCapacity(capacity);
      agent.setBackpackFreeSpace(freeSpace);
      agent.setStatus(status);
      this.agents.put(agentName, agent);
    }
  }

  public synchronized void updateAgentPosition(String agentName, String nodeId) {
    if (this.agents.containsKey(agentName)) {
      AgentData agent = this.agents.get(agentName);
      agent.setPosition(nodeId);
      agent.resetCounter();
    } else {
      AgentData agent = new AgentData(nodeId);
      this.agents.put(agentName, agent);
    }
  }

  private void ageAgentData() {
    for (AgentData agent : this.agents.values()) {
      agent.incrementCounter();
    }
    brain.notifyVisualization();
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

  public synchronized boolean isAgentMissing(String name, String position, Map<String, String> observedAgents) {
    boolean isHere = false;
    boolean shouldBeHere = false;
    for (Map.Entry<String, String> neighbour : observedAgents.entrySet()) {
      if (name == neighbour.getKey()) {
        isHere = true;
      }
      if (position == neighbour.getValue()) {
        shouldBeHere = true;
      }
    }

    return !isHere && shouldBeHere;
  }

  public synchronized String getAgentAtPosition(String nodeId) {
    return this.agents.entrySet().stream()
        .filter(entry -> nodeId.equals(entry.getValue().getPosition()))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  public synchronized void setSiloPosition(String nodeId) {
    if (silo == null) {
      this.silo = new SiloData(nodeId);
    } else {
      this.silo.setPosition(nodeId);
      this.silo.resetCounter();
    }
    brain.notifyVisualization();
  }

  private void ageSiloData() {
    if (this.silo != null) {
      this.silo.incrementCounter();
      brain.notifyVisualization();
    }
  }

  public synchronized void setGolemPosition(String nodeId) {
    if (this.golem == null) {
      this.golem = new GolemData(nodeId);
    } else {
      this.golem.setPosition(nodeId);
      this.golem.resetCounter();
    }
    brain.notifyVisualization();
  }

  private void ageGolemData() {
    if (this.golem != null) {
      this.golem.incrementCounter();
      brain.notifyVisualization();
    }
  }

  public synchronized void ageEntities() {
    ageTreasureData();
    ageAgentData();
    ageSiloData();
    ageGolemData();
  }

  public synchronized void mergeTreasures(Map<String, TreasureData> receivedTreasures) {
    for (Map.Entry<String, TreasureData> entry : receivedTreasures.entrySet()) {
      String nodeId = entry.getKey();
      TreasureData receivedTreasure = entry.getValue();

      if (!this.treasures.containsKey(nodeId)
          || this.treasures.get(nodeId).getUpdateCounter() > receivedTreasure.getUpdateCounter()) {
        this.treasures.put(nodeId, receivedTreasure);
      }
    }

    brain.notifyVisualization();
  }

  public synchronized void mergeAgents(Map<String, AgentData> receivedAgents) {
    for (Map.Entry<String, AgentData> entry : receivedAgents.entrySet()) {
      String agentName = entry.getKey();
      AgentData receivedAgent = entry.getValue();

      if (!this.agents.containsKey(agentName)
          || this.agents.get(agentName).getUpdateCounter() > receivedAgent.getUpdateCounter()) {
        this.agents.put(agentName, receivedAgent);
      }
    }

    brain.notifyVisualization();
  }

  public synchronized void mergeSilo(SiloData receivedSilo) {
    if (receivedSilo == null) {
      return;
    }

    if (this.silo == null) {
      this.silo = receivedSilo;
    } else if (this.silo.getUpdateCounter() > receivedSilo.getUpdateCounter()) {
      this.silo.copy(receivedSilo);
    }

    brain.notifyVisualization();
  }

  public synchronized void mergeGolem(GolemData receivedGolem) {
    if (receivedGolem == null) {
      return;
    }

    if (this.golem == null) {
      this.golem = receivedGolem;
    } else if (this.golem.getUpdateCounter() > receivedGolem.getUpdateCounter()) {
      this.golem.copy(receivedGolem);
    }

    brain.notifyVisualization();
  }

  public synchronized List<String> getOccupiedPositions() {
    List<String> occupiedPositions = new ArrayList<>();

    for (AgentData agent : this.agents.values()) {
      String position = agent.getPosition();
      if (position != null) {
        occupiedPositions.add(position);
      }
    }

    if (silo != null) {
      String siloPosition = silo.getPosition();
      if (siloPosition != null) {
        occupiedPositions.add(siloPosition);
      }
    }

    if (golem != null) {
      String golemPosition = silo.getPosition();
      if (golemPosition != null) {
        occupiedPositions.add(golemPosition);
      }
    }

    return occupiedPositions;
  }
}
