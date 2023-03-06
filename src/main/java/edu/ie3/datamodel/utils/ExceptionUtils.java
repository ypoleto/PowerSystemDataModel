/*
 * © 2023. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
*/
package edu.ie3.datamodel.utils;

import java.util.ArrayList;
import java.util.List;

public class ExceptionUtils {
  /**
   * Creates a string containing multiple exception messsages.
   *
   * @param exceptions list of exceptions
   * @return str containing the messages
   */
  public static String getMessages(List<? extends Exception> exceptions) {
    Exception firstInList = exceptions.remove(0);
    return exceptions.stream()
        .map(Throwable::getMessage)
        .reduce(firstInList.getMessage(), (a, b) -> a + ", " + b);
  }

  /**
   * Creates a new {@link Exception} for multiple given exceptions. The new exception contains all
   * messages of the given exceptions.
   *
   * @param exceptions list of exceptions
   * @return new exceptions
   */
  public static Exception getExceptions(List<? extends Exception> exceptions) {
    ArrayList<? extends Exception> list = new ArrayList<>(exceptions);
    String messages = getMessages(list);
    return new Exception(messages);
  }
}
