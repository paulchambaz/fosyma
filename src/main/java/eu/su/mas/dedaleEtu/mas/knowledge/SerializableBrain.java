package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

import dataStructures.serializableGraph.SerializableSimpleGraph;

public class SerializableBrain implements Serializable {
  private static final long serialVersionUID = -1328748247824344372L;

  private SerializableSimpleGraph<String, MapAttribute> serializableGraph;
  private Map<String, AgentData> agents;
  private Map<String, TreasureData> treasures;
  private SiloData silo;
  private GolemData golem;

  public SerializableBrain(Brain brain) {
    this.serializableGraph = brain.map.getSerializableGraph();
    this.agents = new HashMap<>(brain.entities.getAgents());
    this.agents.put(brain.name, brain.entities.getMyself());

    this.treasures = new HashMap<>(brain.entities.getTreasures());
    this.silo = brain.entities.getSilo();
    this.golem = brain.entities.getGolem();
  }

  public SerializableSimpleGraph<String, MapAttribute> getGraph() {
    return this.serializableGraph;
  }

  public Map<String, AgentData> getAgents() {
    return this.agents;
  }

  public Map<String, TreasureData> getTreasures() {
    return this.treasures;
  }

  public SiloData getSilo() {
    return this.silo;
  }

  public GolemData getGolem() {
    return this.golem;
  }
}
