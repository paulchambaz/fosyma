package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class GolemData implements Serializable {
  private static final long serialVersionUID = -122998495783116336L;

  private String position;
  private int updateCounter;

  public GolemData(String position) {
    this.position = position;
    this.updateCounter = 0;
  }

  public void copy(GolemData golem) {
    this.position = golem.getPosition();
    this.updateCounter = golem.getUpdateCounter();
  }

  public String getPosition() { return position; }
  public int getUpdateCounter() { return updateCounter; }

  public void setPosition(String position) { this.position = position; }

  public void incrementCounter() {
    this.updateCounter++;
  }

  public void resetCounter() {
    this.updateCounter = 0;
  }
}
