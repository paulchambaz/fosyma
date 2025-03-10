package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import eu.su.mas.dedale.env.Observation;

public class AgentData implements Serializable {
  private static final long serialVersionUID = -120113831704116336L;

  private String position;                     // current position of the agent
  private Map<Observation, Integer> expertise; // strength / lockpick capacity
  private int backpackCapacity;                // total capacity of the backpack
  private int backpackFreespace;               // free space in the backpack
  private Observation treasureType;            // type of treasure that the agent can collect

  public AgentData(String position) {
    this.position = position;
    this.expertise = new HashMap<>();
    this.backpackCapacity = 0;
    this.backpackFreespace = 0;
  }

  public String getPosition() { return position; }
  public void setPosition(String position) { this.position = position; }

  public Map<Observation, Integer> getExpertise() { return expertise; }
  public void setExpertise(Map<Observation, Integer> expertise) { this.expertise = expertise; }

  public int getBackpackCapacity() { return backpackCapacity; }
  public void setBackpackCapacity(int backpackCapacity) { this.backpackCapacity = backpackCapacity; }

  public int getBackpackFreespace() { return backpackFreespace; }
  public void setBackpackFreespace(int backpackFreespace) { this.backpackFreespace = backpackFreespace; }

  public Observation getTreasureType() { return treasureType; }
  public void setTreasureType(Observation treasureType) { this.treasureType = treasureType; }
}
