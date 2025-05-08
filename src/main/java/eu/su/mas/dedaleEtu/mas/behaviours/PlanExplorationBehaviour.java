package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import jade.core.Agent;
import eu.su.mas.dedaleEtu.mas.knowledge.Brain;
import eu.su.mas.dedaleEtu.princ.Communication;
import eu.su.mas.dedaleEtu.princ.Computes;
import eu.su.mas.dedaleEtu.princ.Utils;

public class PlanExplorationBehaviour extends OneShotBehaviour {
  private static final long serialVersionUID = -537837587358659414L;

  private String state;
  private int exitValue = 0;

  private Brain brain;

  private static int TIMEOUT = 100;
  private static String PROTOCOL_NAME = "plan-exploration";

  public PlanExplorationBehaviour(String state, Agent agent, Brain brain) {
    super(agent);
    this.brain = brain;
    this.state = state;
  }

  @Override
  public void action() {
    brain.mind.setBehaviour(state);

    Communication comms = brain.mind.getCommunication();

    if (comms.shouldSpeak()) {
      Couple<String, String> planExploration = computePlanExploration(comms);
      if (planExploration == null) {
        return;
      }
      brain.mind.setTargetNode(planExploration.getLeft());

      ACLMessage message = Utils.createACLMessage(
          this.myAgent, PROTOCOL_NAME, comms.getFriend(), planExploration.getRight());
      ((AbstractDedaleAgent) this.myAgent).sendMessage(message);
    } else {
      MessageTemplate filter = MessageTemplate.and(
          MessageTemplate.and(
              MessageTemplate.MatchPerformative(ACLMessage.INFORM),
              MessageTemplate.MatchProtocol(PROTOCOL_NAME)),
          MessageTemplate.MatchSender(comms.getFriend()));

      ACLMessage message = this.myAgent.blockingReceive(filter, TIMEOUT);

      if (message == null) {
        return;
      }

      String planExploration = null;
      try {
        planExploration = (String) message.getContentObject();
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      brain.mind.setTargetNode(planExploration);
    }
  }

  @Override
  public int onEnd() {
    return this.exitValue;
  }

  private Couple<String, String> computePlanExploration(Communication comms) {
    AID friend = comms.getFriend();
    String friendName = friend.getLocalName();
    String currentPosition = brain.entities.getPosition();

    String friendPosition = null;
    if (friendName.equals("Silo")) {
      if (brain.entities.getSilo() != null) {
        friendPosition = brain.entities.getSilo().getPosition();
      } else {
        return null;
      }
    } else {
      if (brain.entities.getAgents().get(friendName) != null) {
        friendPosition = brain.entities.getAgents().get(friendName).getPosition();
      } else {
        return null;
      }
    }
    List<String> openNodes = brain.map.getOpenNodes();

    if (openNodes.isEmpty()) {
      return null;
    }

    List<String> occupiedPositions = new ArrayList<>();
    List<Couple<String, Integer>> myNodesWithDistances = new ArrayList<>();
    for (String node : openNodes) {
      List<String> path = brain.map.findShortestPath(currentPosition, node, occupiedPositions);
      int distance = (path != null) ? path.size() : Integer.MAX_VALUE;
      myNodesWithDistances.add(new Couple<>(node, distance));
    }

    List<Couple<String, Integer>> friendNodesWithDistances = new ArrayList<>();
    for (String node : openNodes) {
      List<String> path = brain.map.findShortestPath(friendPosition, node, occupiedPositions);
      int distance = (path != null) ? path.size() : Integer.MAX_VALUE;
      friendNodesWithDistances.add(new Couple<>(node, distance));
    }

    double[] myDistances = myNodesWithDistances.stream()
        .mapToDouble(couple -> couple.getRight())
        .toArray();

    double[] friendDistances = friendNodesWithDistances.stream()
        .mapToDouble(couple -> couple.getRight())
        .toArray();

    double[] negatedMyDistances = Arrays.stream(myDistances)
        .map(d -> -d)
        .toArray();

    double[] negatedFriendDistances = Arrays.stream(friendDistances)
        .map(d -> -d)
        .toArray();

    double[] myProbs = Computes.toSoftmax(negatedMyDistances);
    double[] friendProbs = Computes.toSoftmax(negatedFriendDistances);

    List<Integer> myIndices = Computes.sampleFromDistribution(myProbs, 5);
    List<Integer> friendIndices = Computes.sampleFromDistribution(friendProbs, 5);

    int numCombinations = myIndices.size() * friendIndices.size();
    if (numCombinations == 0) {
      return null;
    }

    double[][] criteriaMatrix = new double[numCombinations][3];
    List<String> mySelectedNodes = new ArrayList<>();
    List<String> friendSelectedNodes = new ArrayList<>();

    int comboIndex = 0;
    for (int myIdx : myIndices) {
      String myNode = myNodesWithDistances.get(myIdx).getLeft();
      int myDistance = myNodesWithDistances.get(myIdx).getRight();

      for (int friendIdx : friendIndices) {
        String friendNode = friendNodesWithDistances.get(friendIdx).getLeft();
        int friendDistance = friendNodesWithDistances.get(friendIdx).getRight();

        List<String> pathBetween = brain.map.findShortestPath(myNode, friendNode, occupiedPositions);
        int distanceBetween = (pathBetween != null) ? pathBetween.size() : Integer.MAX_VALUE;

        mySelectedNodes.add(myNode);
        friendSelectedNodes.add(friendNode);

        criteriaMatrix[comboIndex][0] = myDistance;
        criteriaMatrix[comboIndex][1] = friendDistance;
        criteriaMatrix[comboIndex][2] = -distanceBetween;

        comboIndex++;
      }
    }

    double[] weights = { 1.0, 1.0, 1.0 };
    int bestIndex = Computes.solveMinMaxRegret(criteriaMatrix, weights);

    String selectedNodeForMe = mySelectedNodes.get(bestIndex);
    String selectedNodeForFriend = friendSelectedNodes.get(bestIndex);

    return new Couple<>(selectedNodeForMe, selectedNodeForFriend);
  }
}
