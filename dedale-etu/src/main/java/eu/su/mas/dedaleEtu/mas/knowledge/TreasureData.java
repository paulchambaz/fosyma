package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import eu.su.mas.dedale.env.Observation;

public class TreasureData implements Serializable {
  private static final long serialVersionUID = -248436651258523402L;
  
  private Observation type; // gold or diamond
  private int quantity;     // available quantity
  private boolean isOpen;   // lock state
  private int lockStrength; // needed strength to open the lock
  private int pickStrength; // needed strength to carry the treasure

  public TreasureData(Observation type, int quantity, boolean isOpen, int lockStrength, int pickStrength) {
    this.type = type;
    this.quantity = quantity;
    this.isOpen = isOpen;
    this.lockStrength = lockStrength;
    this.pickStrength = pickStrength;
  }

  public Observation getType() { return type; }
  public int getQuantity() { return quantity; }
  public boolean isOpen() { return isOpen; }
  public int getLockStrength() { return lockStrength; }
  public int getPickStrength() { return pickStrength; }

  public void setOpen(boolean open) { this.isOpen = open; }
  public void decreaseQuantity(int amount) {
    this.quantity = Math.max(0, this.quantity - amount);
  }
}
