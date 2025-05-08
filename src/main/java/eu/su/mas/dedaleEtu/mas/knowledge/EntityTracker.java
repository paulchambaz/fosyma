package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
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

  private Map<String, Set<String>> agentKnownNodes;

  public EntityTracker(Brain brain) {
    this.brain = brain;
    this.myself = null;
    this.treasures = new HashMap<>();
    this.agents = new HashMap<>();
    this.silo = null;
    this.golem = null;
    this.agentKnownNodes = new HashMap<>();
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

  public synchronized void setTreasureType(Observation treasureType) {
    myself.setTreasureType(treasureType);
  }

  public synchronized void setExpertise(Observation expertise, Integer value) {
    myself.setExpertise(expertise, value);
  }

  public synchronized void setGoldCapacity(Integer value) {
    myself.setGoldCapacity(value);
  }

  public synchronized void setDiamondCapacity(Integer value) {
    myself.setDiamondCapacity(value);
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

  public synchronized void updateTreasureStatus(String nodeId, boolean locked) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData treasure = this.treasures.get(nodeId);
      treasure.setLocked(locked);
      treasure.resetCounter();
    }
    brain.notifyVisualization();
  }

  public synchronized void updateTreasureLockpinging(String nodeId, int lockpicking) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData treasure = this.treasures.get(nodeId);
      treasure.setLockpickStrength(lockpicking);
      treasure.resetCounter();
    }
    brain.notifyVisualization();
  }

  public synchronized void updateTreasureStrength(String nodeId, int strength) {
    if (this.treasures.containsKey(nodeId)) {
      TreasureData treasure = this.treasures.get(nodeId);
      treasure.setCarryStrength(strength);
      treasure.resetCounter();
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

  public synchronized void loseSiloPosition() {
    if (silo != null) {
      silo.setPosition(null);
      brain.notifyVisualization();
    }
  }

  public synchronized void loseGolemPosition() {
    if (golem != null) {
      golem.setPosition(null);
      brain.notifyVisualization();
    }
  }

  public synchronized void loseTreasure(String nodeId) {
    if (treasures.containsKey(nodeId)) {
      treasures.remove(nodeId);
      brain.notifyVisualization();
    }
  }

  public synchronized void updateAgentMeetingPoint(String agentName, String meetingPoint) {
    if (this.agents.containsKey(agentName)) {
      AgentData agent = this.agents.get(agentName);
      agent.setMeetingPoint(meetingPoint);
      agent.resetCounter();
    } else if (agentName.startsWith("Silo") && silo != null) {
      silo.setMeetingPoint(meetingPoint);
      silo.resetCounter();
    }
    brain.notifyVisualization();
  }

  public synchronized String getAgentMeetingPoint(String agentName) {
    if (!this.agents.containsKey(agentName)) {
      return null;
    }
    AgentData agent = this.agents.get(agentName);
    return agent.getMeetingPoint();
  }

  public synchronized void updateAgentKnownNodes(String agentName, List<String> newNodes) {
    if (this.agentKnownNodes.containsKey(agentName)) {
      Set<String> knownNodes = this.agentKnownNodes.get(agentName);
      knownNodes.addAll(newNodes);
    } else {
      Set<String> knownNodes = new HashSet<>(newNodes);
      this.agentKnownNodes.put(agentName, knownNodes);
    }
  }

  public synchronized List<String> getAgentKnownNodes(String agentName) {
    if (this.agentKnownNodes.containsKey(agentName)) {
      return new ArrayList<>(this.agentKnownNodes.get(agentName));
    } else {
      return new ArrayList<>();
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
        .filter(entry -> entry.getValue().canCarryTreasure(requiredStrength))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  public synchronized boolean isAgentMissing(String name, String position, Map<String, String> observedAgents) {
    boolean isObserved = false;

    boolean positionIsObserved = false;

    for (Map.Entry<String, String> entry : observedAgents.entrySet()) {
      if (name.equals(entry.getKey())) {
        isObserved = true;
      }

      if (position.equals(entry.getValue())) {
        positionIsObserved = true;
      }
    }

    return !isObserved && positionIsObserved;
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
      TreasureData receivedTreasure = new TreasureData(entry.getValue());

      if (!this.treasures.containsKey(nodeId)) {
        this.treasures.put(nodeId, receivedTreasure);
      } else {
        TreasureData currentTreasure = this.treasures.get(nodeId);

        if (receivedTreasure.getQuantity() < currentTreasure.getQuantity()) {
          this.treasures.put(nodeId, receivedTreasure);
        } else if (receivedTreasure.getQuantity() == currentTreasure.getQuantity()
            && currentTreasure.getUpdateCounter() > receivedTreasure.getUpdateCounter()) {
          this.treasures.put(nodeId, receivedTreasure);
        }
      }
    }
    brain.notifyVisualization();
  }

  public synchronized void mergeAgents(Map<String, AgentData> receivedAgents) {
    for (Map.Entry<String, AgentData> entry : receivedAgents.entrySet()) {
      String agentName = entry.getKey();

      if (agentName.equals(brain.name) || agentName.equals("Silo") || agentName.equals("Golem")) {
        continue;
      }

      AgentData receivedAgent = new AgentData(entry.getValue());

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

    if (brain.name.equals("Silo")) {
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
