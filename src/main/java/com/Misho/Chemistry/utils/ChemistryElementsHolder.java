package com.Misho.Chemistry.utils;

public final class ChemistryElementsHolder {

  private ChemistryElementsHolder() {}

  public static String getElementByTwoPow(int value) {
    switch (value) {
      case 2:
        return "H";
      case 4:
        return "He";
      case 8:
        return "Li";
      case 16:
        return "Be";
      case 32:
        return "B";
      case 64:
        return "C";
      case 128:
        return "N";
      case 256:
        return "O";
      case 512:
        return "F";
      case 1024:
        return "Ne";
      case 2048:
        return "Na";
      case 4096:
        return "Mg";
      case 8192:
        return "Al";
      case 16384:
        return "Si";
      case 32768:
        return "P";
      default:
        return "Na";
    }
  }

}
