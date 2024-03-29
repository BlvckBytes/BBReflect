/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.bbreflect.version;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ServerVersion {

  V1_7_R0(1,   7, 0,   3),
  V1_7_R1(1,   7, 1,   3),
  V1_7_R2(1,   7, 2,   4),
  V1_7_R3(1,   7, 3,   4),
  V1_7_R4(1,   7, 4,   4),
  V1_7_R5(1,   7, 5,   4),
  V1_7_R6(1,   7, 6,   5),
  V1_7_R7(1,   7, 7,   5),
  V1_7_R8(1,   7, 8,   5),
  V1_7_R9(1,   7, 9,   5),
  V1_7_R10(1,  7, 10,  5),
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
  V1_10_R1(1, 10, 1, 210),
  V1_10_R2(1, 10, 2, 210),
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
  V1_19_R4(1, 19, 4, 762),
  ;

  private static final Pattern VERSION_PATTERN = Pattern.compile(".*\\(.*MC.\\s*([a-zA-z\\d\\-.]+).*");
  private static final ServerVersion[] values = values();

  public final int major, minor, release;
  public final int protocol;
  public final String bukkit;

  ServerVersion(int major, int minor, int release, int protocol) {
    this.major = major;
    this.minor = minor;
    this.release = release;
    this.protocol = protocol;
    this.bukkit = Bukkit.getServer().getClass().getName().split("\\.")[3];
  }

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

  private static int parseIntegerOrZero(String[] data, int index) {
    if (index >= data.length)
      return 0;
    return Integer.parseInt(data[index]);
  }

  private static String tryFindNMSPackageVersion() {
    Class<?> currentClass = Bukkit.getServer().getClass();
    while (currentClass != null && currentClass != Object.class) {
      for (Field field : currentClass.getDeclaredFields()) {
        String typeName = field.getType().getSimpleName();
        if (typeName.equals("DedicatedServer") || typeName.equals("MinecraftServer")) {
          return field.getType().getName().split("\\.")[3];
        }
      }
      currentClass = currentClass.getSuperclass();
    }
    return null;
  }

  private static @Nullable ServerVersion tryParseNMSPackageVersion(@Nullable String version) {
    if (version == null)
      return null;

    try {
      String[] versionData = version.split("_");
      return ServerVersion.fromVersions(
        Integer.parseInt(versionData[0].substring(1)),
        Integer.parseInt(versionData[1]),
        Integer.parseInt(versionData[2].substring(1))
      );
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Find the server's version by looking at craftbukkit's version string
   */
  public static ServerVersion current() {
    String version = tryFindNMSPackageVersion();
    ServerVersion result = tryParseNMSPackageVersion(version);

    if (result == null) {
      version = extractVersion(Bukkit.getVersion());
      String[] data = version.split("\\.");

      result = ServerVersion.fromVersions(
        parseIntegerOrZero(data, 0),
        parseIntegerOrZero(data, 1),
        parseIntegerOrZero(data, 2)
      );
    }

    if (result == null)
      throw new IllegalStateException("Unsupported version encountered: " + version);

    return result;
  }

  /**
   * Extract the minecraft version string major.minor.release from bukkit's version
   * string (Example: 3610-Spigot-6198b5a-19df23a (MC: 1.19.2))
   * @param text Input to extract from
   * @return Extracted version string
   */
  private static String extractVersion(String text) {
    Matcher versionMatcher = VERSION_PATTERN.matcher(text);

    if (!versionMatcher.matches() || versionMatcher.group(1) == null)
      throw new IllegalStateException("Cannot parse version string '" + text + "'");

    return versionMatcher.group(1);
  }
}
