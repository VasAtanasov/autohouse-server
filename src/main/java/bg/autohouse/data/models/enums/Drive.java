package bg.autohouse.data.models.enums;

import bg.autohouse.data.models.annotations.SelectCriteria;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@SelectCriteria
public enum Drive implements Textable {
  FRONT_WHEEL_DRIVE("Front Wheel Drive"),
  REAR_WHEEL_DRIVE("Rear Wheel Drive"),
  FOUR_WHEEL_DRIVE("Four Wheel Drive"),
  ALL_WHEEL_DRIVE("All Wheel Drive");

  private final String text;

  @Override
  public String toString() {
    return text;
  }
}
