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

  public float getExplorationPriority() {
    return this.explorationPriority;
  }

  public float getCollectionPriority() {
    return this.collectionPriority;
  }

  public void updateBehaviouralPriorities() {
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

  public boolean isCollectionPreferred() {
    return this.collectionPriority > this.explorationPriority;
  }

  public int getSocialCooldown() {
    return this.socialCooldown;
  }

  public void resetSocialCooldown() {
    this.socialCooldown = 0;
    brain.notifyVisualization();
  }

  public void initiateSocialCooldown() {
    this.socialCooldown = SOCIAL_COOLDOWN_PERIOD;
    brain.notifyVisualization();
  }

  public void incrementSocialCooldown() {
    this.socialCooldown += 1;
    brain.notifyVisualization();
  }

  public boolean isReadyForSocialInteraction() {
    return this.socialCooldown > SOCIAL_COOLDOWN_PERIOD;
  }

  public int getStuckCounter() {
    return this.stuckCounter;
  }

  public void incrementStuckCounter() {
    this.stuckCounter += 1;
    brain.notifyVisualization();
  }

  public void decrementStuckCounter() {
    this.stuckCounter -= 1;
    if (this.stuckCounter < 0) {
      this.stuckCounter = 0;
    }
    brain.notifyVisualization();
  }

  public void resetStuckCounter() {
    this.stuckCounter = 0;
    brain.notifyVisualization();
  }

  public boolean isStuck() {
    return this.stuckCounter > STUCK_MAX;
  }

  public String getTargetNode() {
    return this.targetNodeId;
  }

  public void setTargetNode(String nodeId) {
    this.targetNodeId = nodeId;
  }

  public Deque<String> getPathToTarget() {
    return this.pathToTarget;
  }

  public void setPathToTarget(List<String> path) {
    if (path == null)
      return;
    this.pathToTarget = new ArrayDeque<>(path);
  }
}
