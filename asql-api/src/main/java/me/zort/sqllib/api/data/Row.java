package me.zort.sqllib.api.data;

import java.util.HashMap;

public class Row extends HashMap<String, Object> {

  public String getString(String key) {
    return (String) get(key);
  }

  public int getInt(String key) {
    return (int) get(key);
  }

  public long getLong(String key) {
    return (long) get(key);
  }

  public double getDouble(String key) {
    return (double) get(key);
  }

  public float getFloat(String key) {
    return (float) get(key);
  }

  public boolean getBoolean(String key) {
    return (boolean) get(key);
  }

  public byte getByte(String key) {
    return (byte) get(key);
  }

  public short getShort(String key) {
    return (short) get(key);
  }

}
