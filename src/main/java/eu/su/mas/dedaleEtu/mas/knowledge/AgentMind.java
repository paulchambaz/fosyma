package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;
import eu.su.mas.dedaleEtu.princ.Utils;
import eu.su.mas.dedaleEtu.princ.Communication;

public class AgentMind implements Serializable {
  private static final long serialVersionUID = -5420389237029721035L;
  private static final int SOCIAL_COOLDOWN_PERIOD = 16;
  private static final int STUCK_MAX = 10;

  private String behaviour;
  private Communication currentCommunication;

  private float explorationPriority;
  private float collectionPriority;

  private int socialCooldown;
  private int stuckCounter;

  private String targetNodeId;
  private Deque<String> pathToTarget;

  private String metaTargetNodeId;

  private CoordinationState coordinationState;
  private String coordinationPartner;
  private String coordinationTreasureNode;

  private final Brain brain;

  public AgentMind(Brain brain) {
    this.brain = brain;
    this.explorationPriority = 1.0f;
    this.collectionPriority = 0.0f;
    this.socialCooldown = 0;
    this.stuckCounter = 0;
    this.targetNodeId = "";
    this.pathToTarget = new ArrayDeque<>();
    this.metaTargetNodeId = "";
    this.coordinationState = CoordinationState.NONE;
    this.coordinationPartner = null;
    this.coordinationTreasureNode = null;
  }

  public synchronized String getBehaviour() {
    return this.behaviour;
  }

  public synchronized void setBehaviour(String behaviour) {
    this.behaviour = behaviour;
    brain.log(behaviour);
    brain.notifyVisualization();
  }

  public synchronized Communication getCommunication() {
    return this.currentCommunication;
  }

  public synchronized void setCommunication(Communication comms) {
    this.currentCommunication = comms;
  }

  public synchronized void resetCommunication() {
    this.currentCommunication = null;
  }

  public synchronized float getExplorationPriority() {
    return this.explorationPriority;
  }

  public synchronized float getCollectionPriority() {
    return this.collectionPriority;
  }

  public synchronized void updateBehaviouralPriorities() {
    float gradualTransition = 0.0001f;
    float accelerateEffect = 0.5f;
    boolean wasCollectionPreferred = isCollectionPreferred();

    float targetExploration = wasCollectionPreferred ? 1.0f : 0.0f;
    float targetCollection = wasCollectionPreferred ? 0.0f : 1.0f;

    // brain.log("exploration before", explorationPriority);
    this.explorationPriority = Utils.lerp(this.explorationPriority, targetExploration, gradualTransition);
    // brain.log("exploration after", explorationPriority);
    // brain.log("collection before", collectionPriority);
    this.collectionPriority = Utils.lerp(this.collectionPriority, targetCollection, gradualTransition);
    // brain.log("collection after", collectionPriority);

    if (isCollectionPreferred() != wasCollectionPreferred) {
      this.explorationPriority = Utils.lerp(this.explorationPriority, targetExploration, accelerateEffect);
      this.collectionPriority = Utils.lerp(this.collectionPriority, targetCollection, accelerateEffect);
    }

    brain.notifyVisualization();
  }

  public synchronized boolean isCollectionPreferred() {
    return this.collectionPriority > this.explorationPriority;
  }

  public synchronized int getSocialCooldown() {
    return this.socialCooldown;
  }

  public synchronized void wantsToTalk() {
    this.socialCooldown = SOCIAL_COOLDOWN_PERIOD + 1;
    brain.notifyVisualization();
  }

  public synchronized void resetSocialCooldown() {
    this.socialCooldown = 0;
    brain.notifyVisualization();
  }

  public synchronized void initiateSocialCooldown() {
    this.socialCooldown = SOCIAL_COOLDOWN_PERIOD - 1;
    brain.notifyVisualization();
  }

  public synchronized void incrementSocialCooldown() {
    this.socialCooldown += 1;
    brain.notifyVisualization();
  }

  public synchronized boolean isReadyForSocialInteraction() {
    return this.socialCooldown > SOCIAL_COOLDOWN_PERIOD;
  }

  public synchronized int getStuckCounter() {
    return this.stuckCounter;
  }

  public synchronized void incrementStuckCounter() {
    this.stuckCounter += 1;
    brain.notifyVisualization();
  }

  public synchronized void decrementStuckCounter() {
    this.stuckCounter -= 1;
    if (this.stuckCounter < 0) {
      this.stuckCounter = 0;
    }
    brain.notifyVisualization();
  }

  public synchronized void resetStuckCounter() {
    this.stuckCounter = 0;
    brain.notifyVisualization();
  }

  public synchronized boolean isStuck() {
    return this.stuckCounter > STUCK_MAX;
  }

  public synchronized String getTargetNode() {
    return this.targetNodeId;
  }

  public synchronized void setTargetNode(String nodeId) {
    this.targetNodeId = nodeId;
  }

  public synchronized Deque<String> getPathToTarget() {
    return this.pathToTarget;
  }

  public synchronized void setPathToTarget(List<String> path) {
    if (path == null)
      return;
    this.pathToTarget = new ArrayDeque<>(path);
  }

  public synchronized String getMetaTargetNode() {
    return this.metaTargetNodeId;
  }

  public synchronized void setMetaTargetNode(String nodeId) {
    this.metaTargetNodeId = nodeId;
    brain.notifyVisualization();
  }

  public synchronized CoordinationState getCoordinationState() {
    return this.coordinationState;
  }

  public synchronized void setCoordinationState(CoordinationState state) {
    this.coordinationState = state;
    brain.notifyVisualization();
  }

  public synchronized String getCoordinationPartner() {
    return this.coordinationPartner;
  }

  public synchronized void setCoordinationPartner(String agentName) {
    this.coordinationPartner = agentName;
    brain.notifyVisualization();
  }

  public synchronized String getCoordinationTreasureNode() {
    return this.coordinationTreasureNode;
  }

  public synchronized void setCoordinationTreasureNode(String nodeId) {
    this.coordinationTreasureNode = nodeId;
    brain.notifyVisualization();
  }

  public synchronized boolean isCoordinating() {
    return this.coordinationState != CoordinationState.NONE;
  }
}
