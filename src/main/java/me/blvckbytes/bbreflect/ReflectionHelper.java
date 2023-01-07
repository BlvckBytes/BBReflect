package me.blvckbytes.bbreflect;

import lombok.Getter;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ReflectionHelper {

  // Server version information
  @Getter private final String versionStr;
  @Getter private final ServerVersion version;

  public ReflectionHelper(@Nullable Supplier<String> versionSupplier) throws Exception {
    this.versionStr = versionSupplier == null ? findVersion() : versionSupplier.get();
    this.version = parseVersion(this.versionStr);
  }

  public ClassHandle getClass(RClass rc) throws ClassNotFoundException {
    return rc.resolve(this.version);
  }

  public @Nullable ClassHandle getClassOptional(RClass rc) {
    try {
      return getClass(rc);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public @Nullable EnumHandle getEnumOptional(RClass rc) {
    try {
      return getClass(rc).asEnum();
    } catch (ClassNotFoundException | IllegalStateException e) {
      return null;
    }
  }

  /**
   * Find the server's version by looking at craftbukkit's package
   * @return Version part of the package
   */
  private String findVersion() {
    return Bukkit.getServer().getClass().getName().split("\\.")[3];
  }

  /**
   * Get the major, minor and revision version numbers the server's running on
   * @return [major, minor, revision]
   */
  private ServerVersion parseVersion(String version) {
    String[] data = version.split("_");

    ServerVersion result = ServerVersion.fromVersions(
      Integer.parseInt(data[0].substring(1)), // remove leading v
      Integer.parseInt(data[1]),
      Integer.parseInt(data[2].substring(1)) // Remove leading R
    );

    if (result == null)
      throw new IllegalStateException("Unsupported version encountered: " + version);

    return result;
  }
}
