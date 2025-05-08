package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.mas.knowledge.CoordinationState;
import eu.su.mas.dedaleEtu.mas.knowledge.TreasureData;
import eu.su.mas.dedaleEtu.princ.Computes;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Communication;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class TreasureCoordinationNegotiationBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -673824763487238374L;
  private int exitValue = 0;
  private Brain brain;

  private static final String COORDINATION_PROTOCOL = "treasure-coordination";
  private static final int TIMEOUT = 100;

  public TreasureCoordinationNegotiationBehaviour(Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour("Coordination Negotiation");

    Communication comms = brain.mind.getCommunication();
    if (comms == null) {
      this.exitValue = 0;
      return;
    }

    String targetPartner = brain.mind.getCoordinationPartner();
    if (targetPartner != null && !comms.getFriend().getLocalName().equals(targetPartner)) {
      brain.log("Not communicating with coordination partner, expected:", targetPartner);
      this.exitValue = 0;
      return;
    }

    if (targetPartner == null) {
      brain.mind.setCoordinationPartner(comms.getFriend().getLocalName());
    }

    AID partnerAID = comms.getFriend();
    boolean isSpeaker = comms.shouldSpeak();

    if (isSpeaker) {
      CoordinationData myData = prepareCoordinationData();

      ACLMessage message = Utils.createACLMessage(
          this.myAgent, COORDINATION_PROTOCOL, partnerAID, myData);
      ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
      brain.log("Sent coordination data to", partnerAID.getLocalName());

      CoordinationData partnerData = receiveCoordinationData(partnerAID);
      if (partnerData == null) {
        brain.log("Failed to receive coordination data from", partnerAID.getLocalName());
        resetCoordination();
        this.exitValue = 0;
        return;
      }

      determineRoles(myData, partnerData);

    } else {
      CoordinationData partnerData = receiveCoordinationData(partnerAID);
      if (partnerData == null) {
        brain.log("Failed to receive coordination data from", partnerAID.getLocalName());
        resetCoordination();
        this.exitValue = 0;
        return;
      }

      CoordinationData myData = prepareCoordinationData();
      ACLMessage message = Utils.createACLMessage(
          this.myAgent, COORDINATION_PROTOCOL, partnerAID, myData);
      ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
      brain.log("Sent coordination data to", partnerAID.getLocalName());

      determineRoles(myData, partnerData);
    }
  }

  private CoordinationData prepareCoordinationData() {
    CoordinationData data = new CoordinationData();
    data.currentPosition = brain.entities.getPosition();

    String treasureNode = brain.mind.getCoordinationTreasureNode();
    if (treasureNode != null) {
      TreasureData treasure = brain.entities.getTreasures().get(treasureNode);
      if (treasure != null) {
        data.needsHelp = true;
        data.treasureNode = treasureNode;
        data.treasureQuantity = treasure.getQuantity();
        data.lockStrength = treasure.getLockpickStrength();
      }
    }

    return data;
  }

  private CoordinationData receiveCoordinationData(AID partnerAID) {
    MessageTemplate template = MessageTemplate.and(
        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
        MessageTemplate.and(
            MessageTemplate.MatchProtocol(COORDINATION_PROTOCOL),
            MessageTemplate.MatchSender(partnerAID)));

    ACLMessage message = this.myAgent.blockingReceive(template, TIMEOUT);
    if (message == null) {
      return null;
    }

    try {
      return (CoordinationData) message.getContentObject();
    } catch (Exception e) {
      brain.log("Error receiving coordination data:", e.getMessage());
      return null;
    }
  }

  private void determineRoles(CoordinationData myData, CoordinationData partnerData) {
    // Check if either agent needs help
    boolean iNeedHelp = myData.needsHelp;
    boolean partnerNeedsHelp = partnerData.needsHelp;

    if (!iNeedHelp && !partnerNeedsHelp) {
      brain.log("No agent needs help with treasure - aborting coordination");
      resetCoordination();
      this.exitValue = 0;
      return;
    }

    if (iNeedHelp && !partnerNeedsHelp) {
      brain.mind.setCoordinationState(CoordinationState.LEADER);
      brain.mind.setMetaTargetNode(myData.treasureNode);
      brain.log("I need help and partner doesn't - I'll be leader");
      this.exitValue = 1;
      return;
    }

    if (!iNeedHelp && partnerNeedsHelp) {
      brain.mind.setCoordinationState(CoordinationState.FOLLOWER);
      brain.mind.setCoordinationTreasureNode(partnerData.treasureNode);
      brain.log("Partner needs help and I don't - I'll be follower");
      this.exitValue = 2;
      return;
    }

    double[][] criteriaMatrix = new double[2][3];

    criteriaMatrix[0][0] = myData.treasureQuantity;
    criteriaMatrix[0][1] = -myData.lockStrength;

    List<String> pathToMyTreasure = brain.map.findShortestPath(
        myData.currentPosition, myData.treasureNode, new ArrayList<>());
    double distanceToMyTreasure = (pathToMyTreasure != null) ? pathToMyTreasure.size() : Double.MAX_VALUE;
    criteriaMatrix[0][2] = -distanceToMyTreasure;

    criteriaMatrix[1][0] = partnerData.treasureQuantity;
    criteriaMatrix[1][1] = -partnerData.lockStrength;

    List<String> pathToPartnerTreasure = brain.map.findShortestPath(
        partnerData.currentPosition, partnerData.treasureNode, new ArrayList<>());
    double distanceToPartnerTreasure = (pathToPartnerTreasure != null) ? pathToPartnerTreasure.size()
        : Double.MAX_VALUE;
    criteriaMatrix[1][2] = -distanceToPartnerTreasure;

    double[] weights = { 1.5, 1.0, 2.0 };

    int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);
    boolean iAmLeader = (bestIndex == 0);

    if (iAmLeader) {
      brain.mind.setCoordinationState(CoordinationState.LEADER);
      brain.mind.setMetaTargetNode(myData.treasureNode);
      brain.log("Both need help - determined I should be leader based on criteria");
      this.exitValue = 1;
    } else {
      brain.mind.setCoordinationState(CoordinationState.FOLLOWER);
      brain.mind.setCoordinationTreasureNode(partnerData.treasureNode);
      brain.log("Both need help - determined partner should be leader based on criteria");
      this.exitValue = 2;
    }
  }

  private void resetCoordination() {
    brain.mind.setCoordinationState(CoordinationState.NONE);
    brain.mind.setCoordinationPartner(null);
    brain.mind.setCoordinationTreasureNode(null);
    brain.mind.setMetaTargetNode(null);
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }

  private static class CoordinationData implements Serializable {
    private static final long serialVersionUID = 28238665831L;
    public String currentPosition;
    public boolean needsHelp = false;
    public String treasureNode;
    public int treasureQuantity;
    public int lockStrength;
  }
}
