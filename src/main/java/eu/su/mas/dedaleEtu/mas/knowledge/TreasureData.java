package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import eu.su.mas.dedale.env.Observation;

public class TreasureData implements Serializable {
  private static final long serialVersionUID = -248436651258523402L;

  private String nodeId; // location of the treasure
  private Observation type; // gold or diamond
  private int updateCounter; // counter since last update (not Unix timestamp)
  private int quantity; // available quantity
  private boolean isLocked; // lock state (true = locked, false = unlocked)
  private int lockStrength; // needed strength to open the lock
  private int pickStrength; // needed strength to carry the treasure

  public TreasureData(String nodeId, Observation type, int quantity, boolean isLocked, int lockStrength,
      int pickStrength) {
    this.nodeId = nodeId;
    this.type = type;
    this.updateCounter = 0;
    this.quantity = quantity;
    this.isLocked = isLocked;
    this.lockStrength = lockStrength;
    this.pickStrength = pickStrength;
  }

  public TreasureData(TreasureData o) {
    this.nodeId = o.getNodeId();
    this.type = o.getType();
    this.updateCounter = o.getUpdateCounter();
    this.quantity = o.getQuantity();
    this.isLocked = o.isLocked();
    this.lockStrength = o.getLockStrength();
    this.pickStrength = o.getPickStrength();
  }

  public String getNodeId() {
    return nodeId;
  }

  public Observation getType() {
    return type;
  }

  public int getQuantity() {
    return quantity;
  }

  public boolean isLocked() {
    return isLocked;
  }

  public int getLockStrength() {
    return lockStrength;
  }

  public int getPickStrength() {
    return pickStrength;
  }

  public int getUpdateCounter() {
    return updateCounter;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public void setLocked(boolean locked) {
    this.isLocked = locked;
  }

  public void setStrength(int lockstrength) {
    this.lockStrength = lockstrength;
  }

  public void setLockpicking(int lockpicking) {
    this.pickStrength = lockpicking;
  }

  public void setUpdateCounter(int counter) {
    this.updateCounter = counter;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public void decreaseQuantity(int amount) {
    this.quantity = Math.max(0, this.quantity - amount);
  }

  public void incrementCounter() {
    this.updateCounter++;
  }

  public void resetCounter() {
    this.updateCounter = 0;
  }
}
