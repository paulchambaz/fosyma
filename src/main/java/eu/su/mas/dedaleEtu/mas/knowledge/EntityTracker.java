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
  private Map<String, SiloData> silos;
  private Map<String, GolemData> golems;

  private Map<String, Set<String>> agentKnownNodes;

  public EntityTracker(Brain brain) {
    this.brain = brain;
    this.myself = null;
    this.treasures = new HashMap<>();
    this.agents = new HashMap<>();
    this.silos = new HashMap<>();
    this.golems = new HashMap<>();
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

  public synchronized Map<String, SiloData> getSilos() {
    return this.silos;
  }

  public synchronized Map<String, GolemData> getGolems() {
    return this.golems;
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

  public synchronized void loseSiloPosition(String siloName) {
    if (this.silos.containsKey(siloName)) {
      SiloData silo = this.silos.get(siloName);
      silo.setPosition(null);
      brain.notifyVisualization();
    }
  }

  public synchronized void loseGolemPosition(String golemName) {
    if (this.golems.containsKey(golemName)) {
      GolemData golem = this.golems.get(golemName);
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

  public synchronized SiloData getClosestSilo() {
    List<String> siloPositions = new ArrayList<>();
    String closestSiloName = null;
    SiloData closestSilo = null;
    for (SiloData silo : this.silos.values()) {
      String siloPosition = silo.getPosition();
      if (siloPosition != null) {
        siloPositions.add(siloPosition);
      }
    }
    if (!(siloPositions.isEmpty())) {
      closestSiloName = this.brain.findClosestNode(siloPositions, false);
    }
    for (SiloData silo : this.silos.values()) {
      String siloPosition = silo.getPosition();
      if ((siloPosition == closestSiloName) && siloPosition != null) {
        closestSilo = new SiloData(silo);
      }
    }
    return closestSilo;
  }

  public synchronized void updateAgentMeetingPoint(String agentName, String meetingPoint) {
    if (this.agents.containsKey(agentName)) {
      AgentData agent = this.agents.get(agentName);
      agent.setMeetingPoint(meetingPoint);
      agent.resetCounter();
    } else if (agentName.startsWith("Silo") && (this.silos.containsKey(agentName))) {
      this.silos.get(agentName).setMeetingPoint(meetingPoint);
      this.silos.get(agentName).resetCounter();
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

  public synchronized void setSiloPosition(String silo, String nodeId) {
    if (!(silos.containsKey(silo))) {
      this.silos.put(silo, new SiloData(nodeId));
    } else {
      this.silos.get(silo).setPosition(nodeId);
      this.silos.get(silo).resetCounter();
    }
    brain.notifyVisualization();
  }

  private void ageSiloData() {
    if (!(this.silos.isEmpty())) {
      for (SiloData entry : this.silos.values()) {
        entry.incrementCounter();
        brain.notifyVisualization();
      }
    }
  }

  public synchronized void setGolemPosition(String golem, String nodeId) {
    if (!(golems.containsKey(golem))) {
      this.golems.put(golem, new GolemData(nodeId));
    } else {
      this.golems.get(golem).setPosition(nodeId);
      this.golems.get(golem).resetCounter();
    }
    brain.notifyVisualization();
  }

  private void ageGolemData() {
    if (!(this.golems.isEmpty())) {
      for (GolemData entry : this.golems.values()) {
        entry.incrementCounter();
        brain.notifyVisualization();
      }
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

      if (agentName.equals(brain.name) || agentName.startsWith("Silo") || agentName.startsWith("Golem")) {
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

  public synchronized void mergeSilos(Map<String, SiloData> receivedSilos) {
    if (receivedSilos.isEmpty()) {
      return;
    }
    for (Map.Entry<String, SiloData> entry : receivedSilos.entrySet()) {
      String siloName = entry.getKey();

      if (brain.name.startsWith("Silo")) {
        return;
      }

      SiloData receivedSilo = new SiloData(entry.getValue());

      if (!this.silos.containsKey(siloName)
          || this.silos.get(siloName).getUpdateCounter() > receivedSilo.getUpdateCounter()) {
        this.silos.put(siloName, receivedSilo);
      }
    }

    brain.notifyVisualization();
  }

  public synchronized void mergeGolems(Map<String, GolemData> receivedGolems) {
    if (receivedGolems.isEmpty()) {
      return;
    }
    for (Map.Entry<String, GolemData> entry : receivedGolems.entrySet()) {
      String golemName = entry.getKey();
      GolemData receivedGolem = new GolemData(entry.getValue());

      if (!this.golems.containsKey(golemName)
          || this.golems.get(golemName).getUpdateCounter() > receivedGolem.getUpdateCounter()) {
        this.golems.put(golemName, receivedGolem);
      }
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

    for (SiloData silo : this.silos.values()) {
      String siloPosition = silo.getPosition();
      if (siloPosition != null) {
        occupiedPositions.add(siloPosition);
      }
    }

    for (GolemData golem : this.golems.values()) {
      String golemPosition = golem.getPosition();
      if (golemPosition != null) {
        occupiedPositions.add(golemPosition);
      }
    }

    return occupiedPositions;
  }
}
