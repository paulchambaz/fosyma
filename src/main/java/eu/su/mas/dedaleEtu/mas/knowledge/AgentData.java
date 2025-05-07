package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import eu.su.mas.dedale.env.Observation;

public class AgentData implements Serializable {
  private static final long serialVersionUID = -120113831704116336L;

  private String position; // current position of the agent
  private int updateCounter; // counter since last update (not Unix timestamp)

  private int goldCapacity;
  private int goldAmount;

  private int diamondCapacity;
  private int diamondAmount;

  private Map<Observation, Integer> expertise; // strength / lockpicking
  private Observation treasureType; // type of treasure that the agent can collect

  private String meetingPoint;

  public AgentData(String position) {
    this.position = position;
    this.updateCounter = 0;
    this.expertise = new HashMap<>();
    this.goldCapacity = 0;
    this.goldAmount = 0;
    this.diamondCapacity = 0;
    this.diamondAmount = 0;
    this.meetingPoint = null;
  }

  public AgentData(AgentData o) {
    this.position = o.getPosition();
    this.updateCounter = o.getUpdateCounter();
    this.expertise = o.getExpertise();
    this.goldCapacity = o.getGoldCapacity();
    this.goldAmount = o.getGoldAmount();
    this.diamondCapacity = o.getDiamondCapacity();
    this.diamondAmount = o.getDiamondAmount();
  }

  public String getPosition() {
    return position;
  }

  public Map<Observation, Integer> getExpertise() {
    return expertise;
  }

  public int getGoldCapacity() {
    return goldCapacity;
  }

  public int getGoldAmount() {
    return goldAmount;
  }

  public int getDiamondCapacity() {
    return diamondCapacity;
  }

  public int getDiamondAmount() {
    return diamondAmount;
  }

  public Observation getTreasureType() {
    return treasureType;
  }

  public int getUpdateCounter() {
    return updateCounter;
  }

  public String getMeetingPoint() {
    return meetingPoint;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public void setExpertise(Observation expertise, Integer value) {
    this.expertise.put(expertise, value);
  }

  public void setGoldCapacity(int goldCapacity) {
    this.goldCapacity = goldCapacity;
    this.goldAmount = 0;
  }

  public void setDiamondCapacity(int diamondCapacity) {
    this.diamondCapacity = diamondCapacity;
    this.diamondAmount = 0;
  }

  public void updateGoldAmount(int amount) {
    this.goldAmount += amount;
  }

  public void updateDiamondAmount(int amount) {
    this.diamondAmount += amount;
  }

  public void setTreasureType(Observation treasureType) {
    this.treasureType = treasureType;
  }

  public void setUpdateCounter(int counter) {
    this.updateCounter = counter;
  }

  public void setMeetingPoint(String meetingPoint) {
    this.meetingPoint = meetingPoint;
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
}
