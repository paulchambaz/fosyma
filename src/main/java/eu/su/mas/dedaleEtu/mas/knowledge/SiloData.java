package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;

public class SiloData implements Serializable {
  private static final long serialVersionUID = -122928321314116336L;

  private String position;
  private int updateCounter;

  private String meetingPoint;

  public SiloData(String position) {
    this.position = position;
    this.updateCounter = 0;
    this.meetingPoint = "";
  }

  public void copy(SiloData silo) {
    this.position = silo.getPosition();
    this.updateCounter = silo.getUpdateCounter();
    this.meetingPoint = silo.getMeetingPoint();
  }

  public String getPosition() {
    return position;
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

  public void setMeetingPoint(String meetingPoint) {
    this.meetingPoint = meetingPoint;
  }

  public void incrementCounter() {
    this.updateCounter++;
  }

  public void resetCounter() {
    this.updateCounter = 0;
  }
}
