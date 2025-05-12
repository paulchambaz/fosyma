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
import eu.su.mas.dedaleEtu.princ.Computes;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Communication;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;

public class CoordinationNegotiationBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -673824763487238374L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  private static final String COORDINATION_PROTOCOL = "treasure-coordination";
  private static final int TIMEOUT = 100;

  public CoordinationNegotiationBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);
    this.brain.observe(this.myAgent);

    Communication comms = brain.mind.getCommunication();
    if (comms == null) {
      this.exitValue = 0;
      return;
    }

    String targetPartner = brain.mind.getCoordinationPartner();
    if (targetPartner != null && !comms.getFriend().getLocalName().equals(targetPartner)) {
      brain.log(
          "Not communicating with coordination partner, expected:",
          targetPartner,
          "got",
          comms.getFriend().getLocalName());
      this.exitValue = 0;
      return;
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

      determineRolesAndPositions(myData, partnerData, comms.getFriend().getLocalName());

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

      determineRolesAndPositions(myData, partnerData, comms.getFriend().getLocalName());
    }
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

  private CoordinationData prepareCoordinationData() {
    CoordinationData data = new CoordinationData();
    data.currentPosition = brain.entities.getPosition();

    String partner = brain.mind.getCoordinationPartner();
    if (partner != null) {
      String treasureNode = brain.mind.getCoordinationTreasureNode();

      data.needsHelp = true;
      data.treasureNode = treasureNode;

      List<String> coalitionMembers = brain.mind.getCoalitionMembers();
      if (coalitionMembers != null && !coalitionMembers.isEmpty()) {
        List<String> remainingCoalition = new ArrayList<>(coalitionMembers);
        remainingCoalition.remove(partner);
        data.coalition = remainingCoalition;
      }

      data.coalitionMembersPresent = brain.mind.getCoalitionMembersPresent();
    }

    return data;
  }

  private void determineRolesAndPositions(CoordinationData myData, CoordinationData partnerData, String partnerName) {
    boolean iNeedHelp = myData.needsHelp;
    boolean partnerNeedsHelp = partnerData.needsHelp;

    if (!iNeedHelp && !partnerNeedsHelp) {
      brain.log("No agent needs help with treasure - aborting coordination");
      resetCoordination();
      this.exitValue = 0;
      return;
    }

    String treasureNode;
    boolean iAmLeader;
    List<String> remainingCoalition;
    int present;

    if (iNeedHelp && !partnerNeedsHelp) {
      iAmLeader = true;
      treasureNode = myData.treasureNode;
      remainingCoalition = myData.coalition;
      present = myData.coalitionMembersPresent;
      brain.log("I need help and partner doesn't - I'll be leader'");
    } else if (!iNeedHelp && partnerNeedsHelp) {
      iAmLeader = false;
      treasureNode = partnerData.treasureNode;
      remainingCoalition = partnerData.coalition;
      present = partnerData.coalitionMembersPresent;
      brain.log("Partner needs help and I don't - I'll be follower or manager");
    } else {
      double[][] criteriaMatrix = new double[2][2];

      List<String> pathToMyTreasure = brain.map.findShortestPath(
          myData.currentPosition, myData.treasureNode, new ArrayList<>());
      double distanceToMyTreasure = (pathToMyTreasure != null) ? pathToMyTreasure.size() : Double.MAX_VALUE;
      criteriaMatrix[0][0] = distanceToMyTreasure;

      criteriaMatrix[0][1] = -myData.coalitionMembersPresent;

      List<String> pathToPartnerTreasure = brain.map.findShortestPath(
          partnerData.currentPosition, partnerData.treasureNode, new ArrayList<>());
      double distanceToPartnerTreasure = (pathToPartnerTreasure != null) ? pathToPartnerTreasure.size()
          : Double.MAX_VALUE;
      criteriaMatrix[1][0] = distanceToPartnerTreasure;
      criteriaMatrix[1][1] = -partnerData.coalitionMembersPresent;

      double[] weights = { 2.0, 1.0 };

      int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);
      iAmLeader = (bestIndex == 0);

      treasureNode = iAmLeader ? myData.treasureNode : partnerData.treasureNode;
      remainingCoalition = iAmLeader ? myData.coalition : partnerData.coalition;
      present = iAmLeader ? myData.coalitionMembersPresent : partnerData.coalitionMembersPresent;
      brain.log("Both need help - determined " + (iAmLeader ? "I" : "partner") +
          " should be leader based on distance and coalition size");
    }

    brain.log("Coalition", remainingCoalition);

    if (iAmLeader) {
      if (brain.mind.getCoordinationState() == CoordinationState.LEADER) {
        brain.mind.setCoalitionMembers(remainingCoalition);
        brain.mind.incrementCoalitionMembersPresent();

        brain.mind.setMetaTargetNode(treasureNode);
        brain.mind.setTargetNode(treasureNode);

        brain.mind.setCoalitionParent(null);
        brain.mind.setCoalitionChild(partnerName);

        brain.log("I am the leader, heading to treasure at: " + treasureNode);

        this.exitValue = 1;
      } else if (brain.mind.getCoordinationState() == CoordinationState.MANAGER) {
        brain.mind.setCoordinationState(CoordinationState.FOLLOWER);
        brain.mind.incrementCoalitionMembersPresent();

        brain.mind.setMetaTargetNode(treasureNode);
        brain.mind.setTargetNode(treasureNode);

        brain.log("I was the manager, now follower, heading to treasure at: " + treasureNode);

        brain.mind.setCoalitionChild(partnerName);

        this.exitValue = 3;
      }
    } else {
      if (remainingCoalition != null && !remainingCoalition.isEmpty()) {
        brain.mind.setCoordinationState(CoordinationState.MANAGER);

        brain.mind.setCoalitionMembers(remainingCoalition);
        brain.mind.setCoalitionMembersPresent(present + 1);

        List<String> pathToTreasure = brain.map.findShortestPath(
            brain.entities.getPosition(), treasureNode, new ArrayList<>());

        if (pathToTreasure != null && pathToTreasure.size() >= 2) {
          String myPosition = pathToTreasure.get(pathToTreasure.size() - 2);
          brain.mind.setCoordinationTreasureNode(myPosition);
          brain.log("I am a manager, heading to position: " + myPosition);
        } else {
          brain.mind.setCoordinationTreasureNode(treasureNode);
          brain.log("I am a manager (fallback), heading to treasure: " + treasureNode);
        }

        this.exitValue = 2;

        brain.mind.setCoalitionParent(partnerName);
        brain.mind.setCoalitionChild(null);
      } else {
        brain.mind.setCoordinationState(CoordinationState.FOLLOWER);

        List<String> pathToTreasure = brain.map.findShortestPath(
            brain.entities.getPosition(), treasureNode, new ArrayList<>());

        if (pathToTreasure != null && pathToTreasure.size() >= 2) {
          String myPosition = pathToTreasure.get(pathToTreasure.size() - 2);
          brain.mind.setMetaTargetNode(myPosition);
          brain.mind.setTargetNode(myPosition);
          brain.log("I am a follower, heading to position: " + myPosition);
        } else {
          brain.mind.setMetaTargetNode(treasureNode);
          brain.mind.setTargetNode(treasureNode);
          brain.log("I am a follower (fallback), heading to treasure: " + treasureNode);
        }

        brain.mind.setCoordinationTreasureNode(treasureNode);

        brain.mind.setCoalitionParent(partnerName);
        brain.mind.setCoalitionChild(null);

        this.exitValue = 3;
      }
    }

    brain.log("Current position: " + brain.entities.getPosition() +
        ", Target: " + brain.mind.getTargetNode() +
        ", Role: " + brain.mind.getCoordinationState());
  }

  private void resetCoordination() {
    brain.mind.setCoordinationState(CoordinationState.NONE);
    brain.mind.setCoordinationPartner(null);
    brain.mind.setCoordinationTreasureNode(null);
    brain.mind.setCoalitionMembers(null);
    brain.mind.setCoalitionMembersPresent(0);
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
    public List<String> coalition = new ArrayList<>();
    public int coalitionMembersPresent = 0;
  }
}
