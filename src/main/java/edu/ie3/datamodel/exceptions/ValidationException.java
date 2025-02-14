/*
 * © 2021. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
*/
package edu.ie3.datamodel.exceptions;

public abstract class ValidationException extends Exception {
  protected ValidationException(String s) {
    super(s);
  }

  protected ValidationException(String s, Throwable throwable) {
    super(s, throwable);
  }
}
