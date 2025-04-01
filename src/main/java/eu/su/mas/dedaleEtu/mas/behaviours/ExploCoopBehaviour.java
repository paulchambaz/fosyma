package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Knowledge;
import eu.su.mas.dedaleEtu.mas.knowledge.MapAttribute;
import jade.core.behaviours.OneShotBehaviour;
import java.util.List;
import java.util.ArrayList;

// ExploCoopBehaviour implements cooperative exploration logic for agents
// to discover and map an environment while sharing topological information.
public class ExploCoopBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = 8567689731496787661L;

  private boolean finished = false;
  private Knowledge knowledge;
  private int exitValue;

  // ExploCoopBehaviour constructor initializes the exploration behavior with
  // a reference to the agent, its map representation, and cooperating agents.
  public ExploCoopBehaviour(final AbstractDedaleAgent myagent, Knowledge knowledge) {
    super(myagent);
    this.knowledge = knowledge;
    this.exitValue = 0;
  }

  @Override
  public void action() {
    Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
    if (myPosition == null) {
      return;
    }

    this.knowledge.updateAgentPosition(myPosition.getLocationId());

    List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent)
        .observe();

    try {
      this.myAgent.doWait(500);
    } catch (Exception e) {
      e.printStackTrace();
    }

    this.knowledge.addNode(myPosition.getLocationId(), MapAttribute.closed);

    // iterate on observations in order to handle them correctly
    String nextNodeId = null;
    List<String> receiversAgents = new ArrayList<>();
    for (Couple<Location, List<Couple<Observation, String>>> entry : observations) {
      Location accessibleNode = entry.getLeft();

      // add new nodes to map representation
      boolean isNewNode = this.knowledge.addNewNode(accessibleNode.getLocationId());
      if (myPosition.getLocationId() != accessibleNode.getLocationId()) {
        this.knowledge.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
        if (nextNodeId == null && isNewNode)
          nextNodeId = accessibleNode.getLocationId();
      }

      // collect agent names
      for (Couple<Observation, String> observation : entry.getRight()) {
        Observation observeKind = observation.getLeft();
        String observed = observation.getRight();

        switch (observeKind) {
          case AGENTNAME:

            if (observed.startsWith("Silo")) {
              this.knowledge.setSiloPosition(accessibleNode.getLocationId());
            } else if (observed.startsWith("Golem")) {
              this.knowledge.setGolemPosition(accessibleNode.getLocationId());
            } else {
              this.knowledge.updateAgentsPosition(observed, accessibleNode.getLocationId());
            }
            break;

          case GOLD:
          case DIAMOND:
            // handle treasure observations
            int treasureValue = Integer.parseInt(observed);
            this.knowledge.addTreasure(
                accessibleNode.getLocationId(),
                observeKind,
                treasureValue,
                -1, // default lock strength until we observe it
                -1 // default pick strength until we observe it
            );
            break;

          case STENCH:
          case WIND:
            System.out.println("Environmental cue detected: " + observeKind);
            break;

          case LOCKSTATUS:
            System.out.println("Lock status observed: " + observed);
            break;

          default:
            assert false : "Unhandled observation type: " + observeKind;
        }
      }
    }

    // if there are no more nodes to be learnt about the graph, then we should
    // switch to a simpler protocol
    if (!this.knowledge.hasOpenNode()) {
      System.out.println(this.myAgent.getLocalName() + " - Exploration successufully done, behaviour removed.");
      this.exitValue = 1;
      return;
    }

    // if there are still nodes to be learned
    if (nextNodeId == null) {
      var path = this.knowledge.getShortestPathToClosestOpenNode(myPosition.getLocationId());
      if (path != null && path.size() > 0) {
        nextNodeId = path.get(0);
      } else {
        return;
      }
    }

    // mise a jour hashList
    ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNodeId));
    this.knowledge.introvertRecovery();
  }

  // done signals when the exploration is complete by checking
  // if all nodes in the environment have been visited.

  @Override
  public int onEnd() {
    return this.exitValue;
  }
}
