package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import jade.core.AID;
import java.io.IOException;
import java.util.ArrayList;

// ExploCoopBehaviour implements cooperative exploration logic for agents
// to discover and map an environment while sharing topological information.
public class ExploCoopBehaviour extends SimpleBehaviour {
  private static final long serialVersionUID = 8567689731496787661L;

  private boolean finished = false;
  private MapRepresentation myMap;
  private List<String> agentNames;
  private List<Integer> myHashList;

  // ExploCoopBehaviour constructor initializes the exploration behavior with
  // a reference to the agent, its map representation, and cooperating agents.
  public ExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames, List<Integer> myHashList) {
    super(myagent);
    this.myMap = myMap;
    this.agentNames = agentNames;
    this.myHashList = myHashList;
  }

  @Override
  public void action() {
    if (this.myMap == null) {
      this.myMap = new MapRepresentation();
      this.myAgent.addBehaviour(new ShareMapBehaviour(this.myAgent, this.myMap, agentNames, this.myHashList));
    }

    Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
    if (myPosition == null) {
      return;
    }

    List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent).observe();

    try {
      this.myAgent.doWait(500);
    } catch(Exception e) {
      e.printStackTrace();
    }

    this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);

    // iterate on observations in order to handle them correctly
    String nextNodeId = null;
    List<String> receiversAgents = new ArrayList<>();
    for (Couple<Location, List<Couple<Observation, String>>> entry : observations) {
      Location accessibleNode = entry.getLeft();

      // add new nodes to map representation
      boolean isNewNode = this.myMap.addNewNode(accessibleNode.getLocationId());
      if (myPosition.getLocationId() != accessibleNode.getLocationId()) {
        this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
        if (nextNodeId == null && isNewNode) nextNodeId = accessibleNode.getLocationId();
      }

      // collect agent names
      for (Couple<Observation, String> observation : entry.getRight()) {
        Observation observeKind = observation.getLeft();
        String observed = observation.getRight();

        switch (observeKind) {
          case AGENTNAME:

            if (observed.startsWith("Silo")) {
              this.myMap.setSiloPosition(accessibleNode.getLocationId());
              System.out.println("Found silo agent at : " + accessibleNode.getLocationId());
            } else if (observed.startsWith("Golem")) {
              this.myMap.setGolemPosition(accessibleNode.getLocationId());
              System.out.println("Found golem agent at : " + accessibleNode.getLocationId());
            } else {
              this.myMap.updateAgentPosition(observed, accessibleNode.getLocationId());
            }
            break;

          case GOLD:
          case DIAMOND:
            // handle treasure observations
            int treasureValue = Integer.parseInt(observed);
            this.myMap.addTreasure(
              accessibleNode.getLocationId(),
              observeKind,
              treasureValue,
              -1, // default lock strength until we observe it
              -1 // default pick strength until we observe it
            );
            break;

          case STENCH:
          case WIND:
            // TODO: handle environmental cues
            // TODO: it would be good to use the proper logs on the gui that operates in proper sync time
            System.out.println("Environmental cue detected: " + observeKind);
            break;

          case LOCKSTATUS:
            // TODO: handle lock status - figure out why this does not return
            // any values
            // TODO: it would be good to use the proper logs on the gui that
            // operates in proper sync time
            System.out.println("Lock status observed: " + observed);
            break;

          default:
            // TODO: remove assert in production - agents should ignore by
            // default in case of crashing - but this is useful to detect new
            // observations
            assert false : "Unhandled observation type: " + observeKind;
        }
      }
    }

    if (!this.myMap.hasOpenNode()) {
      finished = true;
      System.out.println(this.myAgent.getLocalName() + " - Exploration successufully done, behaviour removed.");
      return;
    }

    if (nextNodeId == null) {
      nextNodeId = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
    }

    MessageTemplate msgTemplate = MessageTemplate.and(
      MessageTemplate.MatchProtocol("SHARE-TOPO"),
      MessageTemplate.MatchPerformative(ACLMessage.INFORM)
    );

    ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
    if (msgReceived != null) {
      SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
      try {
        sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
      } catch (UnreadableException e) {
        e.printStackTrace();
      }
      this.myMap.mergeMap(sgreceived);
    }

    // mise a jour hashList
    int myHashCode = this.myMap.hashCode();
    myHashList.add(myHashCode);
    ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNodeId));
  }

  // done signals when the exploration is complete by checking
  // if all nodes in the environment have been visited.
  @Override
  public boolean done() {
    return finished;
  }
}
