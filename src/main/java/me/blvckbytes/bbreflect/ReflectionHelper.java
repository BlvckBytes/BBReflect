package me.blvckbytes.bbreflect;

import lombok.Getter;
import me.blvckbytes.bbreflect.version.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ReflectionHelper {

  private final MethodHandle M_CIS__AS_NEW_CRAFT_STACK, M_FURNACE__GET_LUT, M_CIS__GET_TYPE;

  private final Map<Material, Integer> burningTimes;

  // Server version information
  @Getter private final String versionStr;
  @Getter private final ServerVersion version;

  public ReflectionHelper(@Nullable Supplier<String> versionSupplier) throws Exception {
    this.burningTimes = new HashMap<>();

    this.versionStr = versionSupplier == null ? findVersion() : versionSupplier.get();
    this.version = parseVersion(this.versionStr);

    ClassHandle C_ITEM = getClass(RClass.ITEM);
    ClassHandle C_CIS = getClass(RClass.CRAFT_ITEM_STACK);
    ClassHandle C_TEF = getClass(RClass.TILE_ENTITY_FURNACE);

    M_CIS__AS_NEW_CRAFT_STACK = C_CIS.locateMethod().withName("asNewCraftStack").withParameters(C_ITEM).withStatic(true).required();
    M_CIS__GET_TYPE = C_CIS.locateMethod().withName("getType").withReturnType(Material.class).required();

    M_FURNACE__GET_LUT = C_TEF.locateMethod()
      .withReturnType(Map.class)
      .withReturnGeneric(C_ITEM)
      .withReturnGeneric(Integer.class)
      .withStatic(true)
      .required();
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

  public Optional<Integer> getBurnTime(Material mat) {
    Integer dur = burningTimes.get(mat);
    if (dur != null)
      return Optional.of(dur);

    try {
      // Iterate all entries
      Map<?, ?> lut = (Map<?, ?>) M_FURNACE__GET_LUT.invoke(null);
      for (Map.Entry<?, ?> e : lut.entrySet()) {
        Object craftStack = M_CIS__AS_NEW_CRAFT_STACK.invoke(null, e.getKey());
        Material m = (Material) M_CIS__GET_TYPE.invoke(craftStack);

        // Material mismatch, continue
        if (!mat.equals(m))
          continue;

        dur = (Integer) e.getValue();
        burningTimes.put(mat, dur);
        return Optional.of(dur);
      }

      return Optional.empty();
    } catch (Exception e) {
      e.printStackTrace();
      return Optional.empty();
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
