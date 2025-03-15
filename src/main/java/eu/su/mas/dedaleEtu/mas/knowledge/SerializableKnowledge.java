package eu.su.mas.dedaleEtu.mas.knowledge;

import java.util.Map;
import java.io.Serializable;
import dataStructures.serializableGraph.SerializableSimpleGraph;

public class SerializableKnowledge implements Serializable {
  private static final long serialVersionUID = -1328748247824344372L;

  private SerializableSimpleGraph<String, MapAttribute> graph;
  private Map<String, TreasureData> treasures;
  private Map<String, AgentData> agents;
  private SiloData silo;
  private GolemData golem;

  public SerializableKnowledge(
    SerializableSimpleGraph<String, MapAttribute> graph,
    Map<String, TreasureData> treasures,
    Map<String, AgentData> agents,
    SiloData silo,
    GolemData golem
  ) {
    this.graph = graph;
    this.treasures = treasures;
    this.agents = agents;
    this.silo = silo;
    this.golem = golem;
  }

  public SerializableSimpleGraph<String, MapAttribute> getGraph() {
    return this.graph;
  };

  public Map<String, TreasureData> getTreasures() {
    return this.treasures;
  };

  public Map<String, AgentData> getAgents() {
    return this.agents;
  };

  public SiloData getSilo() {
    return this.silo;
  };

  public GolemData getGolem() {
    return this.golem;
  };

}

