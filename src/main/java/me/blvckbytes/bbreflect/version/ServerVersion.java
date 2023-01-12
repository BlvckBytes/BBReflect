package me.blvckbytes.bbreflect.version;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public enum ServerVersion {

  V1_8_R0(1,   8, 0,  47),
  V1_8_R1(1,   8, 1,  47),
  V1_8_R2(1,   8, 2,  47),
  V1_8_R3(1,   8, 3,  47),
  V1_8_R4(1,   8, 4,  47),
  V1_8_R5(1,   8, 5,  47),
  V1_8_R6(1,   8, 6,  47),
  V1_8_R7(1,   8, 7,  47),
  V1_8_R8(1,   8, 8,  47),
  V1_8_R9(1,   8, 9,  47),
  V1_9_R0(1,   9, 0, 107),
  V1_9_R1(1,   9, 1, 108),
  V1_9_R2(1,   9, 2, 109),
  V1_9_R4(1,   9, 4, 110),
  V1_10_R0(1, 10, 0, 210),
  V1_11_R0(1, 11, 0, 315),
  V1_11_R1(1, 11, 1, 316),
  V1_12_R0(1, 12, 0, 335),
  V1_12_R1(1, 12, 1, 338),
  V1_12_R2(1, 12, 2, 340),
  V1_13_R0(1, 13, 0, 393),
  V1_13_R1(1, 13, 1, 401),
  V1_13_R2(1, 13, 2, 404),
  V1_14_R0(1, 14, 0, 477),
  V1_14_R1(1, 14, 1, 480),
  V1_14_R2(1, 14, 2, 485),
  V1_14_R3(1, 14, 3, 490),
  V1_14_R4(1, 14, 4, 498),
  V1_15_R0(1, 15, 0, 573),
  V1_15_R1(1, 15, 1, 575),
  V1_15_R2(1, 15, 2, 578),
  V1_16_R0(1, 16, 0, 735),
  V1_16_R1(1, 16, 1, 736),
  V1_16_R2(1, 16, 2, 751),
  V1_16_R3(1, 16, 3, 753),
  V1_16_R4(1, 16, 4, 754),
  V1_16_R5(1, 16, 5, 754),
  V1_17_R0(1, 17, 0, 755),
  V1_17_R1(1, 17, 1, 756),
  V1_18_R0(1, 18, 0, 757),
  V1_18_R1(1, 18, 1, 757),
  V1_18_R2(1, 18, 2, 758),
  V1_19_R0(1, 19, 0, 759),
  V1_19_R1(1, 19, 1, 760),
  V1_19_R2(1, 19, 2, 760),
  V1_19_R3(1, 19, 3, 761),
  ;

  private final int major, minor, release;
  private final int protocol;
  private static final ServerVersion[] values = values();

  private boolean matches(int major, int minor, int release) {
    if (this.major != major)
      return false;

    if (this.minor != minor)
      return false;

    return this.release == release;
  }

  public int compare(ServerVersion to) {
    int comparison;

    if ((comparison = Integer.compare(this.major, to.major)) != 0)
      return comparison;

    if ((comparison = Integer.compare(this.minor, to.minor)) != 0)
      return comparison;

    return Integer.compare(this.release, to.release);
  }

  @Override
  public String toString() {
    return "v" + this.major + "_" + this.minor + "_R" + this.release;
  }

  public static @Nullable ServerVersion fromVersions(int major, int minor, int release) {
    for (ServerVersion version : values) {
      if (version.matches(major, minor, release))
        return version;
    }

    return null;
  }
}