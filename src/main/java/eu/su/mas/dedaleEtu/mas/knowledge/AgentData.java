package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import eu.su.mas.dedale.env.Observation;

public class AgentData implements Serializable {
  private static final long serialVersionUID = -120113831704116336L;

  private String position; // current position of the agent
  private int updateCounter; // counter since last update (not Unix timestamp)
  private int backpackCapacity; // total capacity of the backpack
  private int backpackFreeSpace; // free space in the backpack
  private Map<Observation, Integer> expertise; // strength / lockpicking
  private Observation treasureType; // type of treasure that the agent can collect
  private String status; // current agent status (exploring, collecting, etc.)

  public AgentData(String position) {
    this.position = position;
    this.updateCounter = 0;
    this.expertise = new HashMap<>();
    this.backpackCapacity = 0;
    this.backpackFreeSpace = 0;
    this.status = "exploring";
  }

  public String getPosition() {
    return position;
  }

  public Map<Observation, Integer> getExpertise() {
    return expertise;
  }

  public int getBackpackCapacity() {
    return backpackCapacity;
  }

  public int getBackpackFreeSpace() {
    return backpackFreeSpace;
  }

  public Observation getTreasureType() {
    return treasureType;
  }

  public int getUpdateCounter() {
    return updateCounter;
  }

  public String getStatus() {
    return status;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public void setExpertise(Observation expertise, Integer value) {
    this.expertise.put(expertise, value);
  }

  public void setBackpackCapacity(int backpackCapacity) {
    this.backpackCapacity = backpackCapacity;
  }

  public void setBackpackFreeSpace(int backpackFreeSpace) {
    this.backpackFreeSpace = backpackFreeSpace;
  }

  public void setTreasureType(Observation treasureType) {
    this.treasureType = treasureType;
  }

  public void setUpdateCounter(int counter) {
    this.updateCounter = counter;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void incrementCounter() {
    this.updateCounter++;
  }

  public void resetCounter() {
    this.updateCounter = 0;
  }

  public boolean canOpenLock(int requiredStrength) {
    return expertise.getOrDefault(Observation.LOCKPICKING, 0) >= requiredStrength;
  }

  public boolean canPickTreasure(int requiredStrength) {
    return expertise.getOrDefault(Observation.STRENGH, 0) >= requiredStrength;
  }

  public int calculateRemainingSpace() {
    return backpackCapacity - (backpackCapacity - backpackFreeSpace);
  }
}
