package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class SiloData implements Serializable {
  private static final long serialVersionUID = -122928321314116336L;

  private String position;
  private int updateCounter;

  public SiloData(String position) {
    this.position = position;
    this.updateCounter = 0;
  }

  public void copy(SiloData silo) {
    this.position = silo.getPosition();
    this.updateCounter = silo.getUpdateCounter();
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
