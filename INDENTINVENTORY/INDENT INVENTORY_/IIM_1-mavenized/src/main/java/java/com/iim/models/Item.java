package com.iim.models;

public class Item {
  private int id;
  private String itemName;
  private String codeBarcode;

  private String status; // active/inactive
  private int quantity;  // joined from inventory

  private int minQuantity;            // items.min_quantity
  private int criticalMinQuantity;    // items.critical_min_quantity

  public int getId(){ return id; }
  public void setId(int id){ this.id = id; }

  public String getItemName(){ return itemName; }
  public void setItemName(String itemName){ this.itemName = itemName; }

  public String getCodeBarcode(){ return codeBarcode; }
  public void setCodeBarcode(String codeBarcode){ this.codeBarcode = codeBarcode; }

  public String getStatus(){ return status; }
  public void setStatus(String status){ this.status = status; }

  public int getQuantity(){ return quantity; }
  public void setQuantity(int quantity){ this.quantity = quantity; }

  public int getMinQuantity(){ return minQuantity; }
  public void setMinQuantity(int minQuantity){ this.minQuantity = minQuantity; }

  public int getCriticalMinQuantity(){ return criticalMinQuantity; }
  public void setCriticalMinQuantity(int criticalMinQuantity){ this.criticalMinQuantity = criticalMinQuantity; }
}
