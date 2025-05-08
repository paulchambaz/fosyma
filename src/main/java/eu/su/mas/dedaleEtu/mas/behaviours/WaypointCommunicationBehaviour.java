package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.Serializable;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.CoordinationState;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Communication;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class WaypointCommunicationBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -383293847329437459L;
  private int exitValue = 0;
  private Brain brain;

  private static final String WAYPOINT_PROTOCOL = "waypoint-guidance";
  private static final int TIMEOUT = 100;

  public WaypointCommunicationBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Waypoint Communication");

    Communication comms = brain.mind.getCommunication();
    if (comms == null) {
      brain.log("No communication context available");
      this.exitValue = 0;
      return;
    }

    String partnerName = brain.mind.getCoordinationPartner();
    if (partnerName == null) {
      brain.log("No coordination partner set");
      this.exitValue = 0;
      return;
    }

    AID partnerAID = comms.getFriend();
    if (!partnerAID.getLocalName().equals(partnerName)) {
      brain.log("Communication established with wrong agent, expected:", partnerName);
      this.exitValue = 0;
      return;
    }

    if (brain.mind.getCoordinationState() == CoordinationState.LEADER) {
      sendWaypoint(partnerAID);
    } else {
      receiveWaypoint(partnerAID);
    }

    this.exitValue = 1;
  }

  private void sendWaypoint(AID followerAID) {
    String waypoint = brain.mind.getTargetNode();
    if (waypoint == null) {
      brain.log("No waypoint set to send");
      return;
    }

    try {
      WaypointData waypointData = new WaypointData();
      waypointData.waypointNode = waypoint;

      ACLMessage message = Utils.createACLMessage(
          this.myAgent, WAYPOINT_PROTOCOL, followerAID, waypointData);
      ((AbstractDedaleAgent) this.myAgent).sendMessage(message);

      brain.log("Sent waypoint", waypoint, "to follower", followerAID.getLocalName());
    } catch (Exception e) {
      brain.log("Error sending waypoint:", e.getMessage());
    }
  }

  private void receiveWaypoint(AID leaderAID) {
    MessageTemplate template = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.and(
            MessageTemplate.MatchProtocol(WAYPOINT_PROTOCOL),
            MessageTemplate.MatchSender(leaderAID)));

    ACLMessage message = this.myAgent.blockingReceive(template, TIMEOUT);
    if (message == null) {
      brain.log("No waypoint received from leader");
      return;
    }

    try {
      WaypointData waypointData = (WaypointData) message.getContentObject();
      String waypoint = waypointData.waypointNode;

      brain.log("Received waypoint", waypoint, "from leader", leaderAID.getLocalName());

      brain.mind.setMetaTargetNode(waypoint);
      brain.mind.setTargetNode(waypoint);
    } catch (Exception e) {
      brain.log("Error receiving waypoint:", e.getMessage());
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }

  private static class WaypointData implements Serializable {
    private static final long serialVersionUID = 18878L;
    public String waypointNode;
  }
}
