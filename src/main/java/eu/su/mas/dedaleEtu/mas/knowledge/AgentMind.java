package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;
import eu.su.mas.dedaleEtu.princ.Utils;

public class AgentMind implements Serializable {
  private static final long serialVersionUID = -5420389237029721035L;
  private static final int SOCIAL_COOLDOWN_PERIOD = 16;
  private static final int STUCK_MAX = 10;

  private String behaviour;

  private float explorationPriority;
  private float collectionPriority;

  private int socialCooldown;
  private int stuckCounter;

  private String targetNodeId;
  private Deque<String> pathToTarget;

  private final Brain brain;

  public AgentMind(Brain brain) {
    this.brain = brain;
    this.explorationPriority = 1.0f;
    this.collectionPriority = 0.0f;
    this.socialCooldown = 0;
    this.stuckCounter = 0;
    this.targetNodeId = "";
    this.pathToTarget = new ArrayDeque<>();
  }

  public synchronized String getBehaviour() {
    return this.behaviour;
  }

  public synchronized void setBehaviour(String behaviour) {
    this.behaviour = behaviour;
    brain.notifyVisualization();
  }

  public synchronized float getExplorationPriority() {
    return this.explorationPriority;
  }

  public synchronized float getCollectionPriority() {
    return this.collectionPriority;
  }

  public synchronized void updateBehaviouralPriorities() {
    float gradualTransition = 0.99f;
    float accelerateEffect = 0.5f;
    boolean wasCollectionPreferred = isCollectionPreferred();

    float targetExploration = wasCollectionPreferred ? 1.0f : 0.0f;
    float targetCollection = wasCollectionPreferred ? 0.0f : 1.0f;

    this.explorationPriority = Utils.lerp(this.explorationPriority, targetExploration, gradualTransition);
    this.collectionPriority = Utils.lerp(this.collectionPriority, targetCollection, gradualTransition);

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

  public synchronized void resetSocialCooldown() {
    this.socialCooldown = 0;
    brain.notifyVisualization();
  }

  public synchronized void initiateSocialCooldown() {
    this.socialCooldown = SOCIAL_COOLDOWN_PERIOD;
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
}
