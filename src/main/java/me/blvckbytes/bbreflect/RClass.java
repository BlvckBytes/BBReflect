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

package me.blvckbytes.bbreflect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.bbreflect.version.ServerVersion;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum RClass {
  WORLD_MAP((ver, after) -> after ?
    Class.forName("net.minecraft.world.level.saveddata.maps.WorldMap") :
    Class.forName("net.minecraft.server." + ver + ".WorldMap")
  ),
  ENUM_DIRECTION((ver, after) -> after ?
    Class.forName("net.minecraft.core.EnumDirection") :
    Class.forName("net.minecraft.server." + ver + ".EnumDirection")
  ),
  ENTITY_ITEM_FRAME((ver, after) -> after ?
    Class.forName("net.minecraft.world.entity.decoration.EntityItemFrame") :
    Class.forName("net.minecraft.server." + ver + ".EntityItemFrame")
  ),
  WORLD_SERVER((ver, after) -> after ?
    Class.forName("net.minecraft.server.level.WorldServer") :
    Class.forName("net.minecraft.server." + ver + ".WorldServer")
  ),
  WORLD((ver, after) -> after ?
    Class.forName("net.minecraft.world.level.World") :
    Class.forName("net.minecraft.server." + ver + ".World")
  ),
  BLOCK_POSITION((ver, after) -> after ?
    Class.forName("net.minecraft.core.BlockPosition") :
    Class.forName("net.minecraft.server." + ver + ".BlockPosition")
  ),
  MINECRAFT_SERVER((ver, after) -> after ?
    Class.forName("net.minecraft.server.MinecraftServer") :
    Class.forName("net.minecraft.server." + ver + ".MinecraftServer")
  ),
  SCOREBOARD_SERVER((ver, after) -> after ?
    Class.forName("net.minecraft.server.ScoreboardServer") :
    Class.forName("net.minecraft.server." + ver + ".ScoreboardServer")
  ),
  SCOREBOARD_TEAM((ver, after) -> after ?
    Class.forName("net.minecraft.world.score.ScoreboardTeam") :
    Class.forName("net.minecraft.server." + ver + ".ScoreboardTeam")
  ),
  SCOREBOARD((ver, after) -> after ?
    Class.forName("net.minecraft.world.score.Scoreboard") :
    Class.forName("net.minecraft.server." + ver + ".Scoreboard")
  ),
  CRAFT_WORLD((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".CraftWorld")
  ),
  CRAFT_TEAM((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".scoreboard.CraftTeam")
  ),
  CRAFT_SCOREBOARD((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".scoreboard.CraftScoreboard")
  ),
  CRAFT_SCOREBOARD_MANAGER((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".scoreboard.CraftScoreboardManager")
  ),
  PACKET((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.Packet") :
    Class.forName("net.minecraft.server." + ver + ".Packet")
  ),
  I_CHAT_BASE_COMPONENT((ver, after) -> after ?
    Class.forName("net.minecraft.network.chat.IChatBaseComponent") :
    Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent")
  ),
  CHAT_SERIALIZER((ver, after) -> after ?
    Class.forName("net.minecraft.network.chat.IChatBaseComponent$ChatSerializer") :
    Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent$ChatSerializer")
  ),
  CHAT_MESSAGE_TYPE((ver, after) -> after ?
    Class.forName("net.minecraft.network.chat.ChatMessageType") :
    Class.forName("net.minecraft.server." + ver + ".ChatMessageType")
  ),
  FILTERED_TEXT((ver, after) -> after ?
    Class.forName("net.minecraft.server.network.FilteredText") :
    null
  ),
  MESSAGE_SIGNATURE((ver, after) -> after ?
    Class.forName("net.minecraft.network.chat.MessageSignature") :
    null
  ),
  PLAYER_CHAT_MESSAGE((ver, after) -> after ?
    Class.forName("net.minecraft.network.chat.PlayerChatMessage") :
    null
  ),
  NETWORK_MANAGER((ver, after) -> after ?
    Class.forName("net.minecraft.network.NetworkManager") :
    Class.forName("net.minecraft.server." + ver + ".NetworkManager")
  ),
  QUEUED_PACKET((ver, after) -> after ?
    Class.forName("net.minecraft.network.NetworkManager$QueuedPacket") :
    Class.forName("net.minecraft.server." + ver + ".NetworkManager$QueuedPacket")
  ),
  SERVER_CONNECTION((ver, after) -> after ?
    Class.forName("net.minecraft.server.network.ServerConnection") :
    Class.forName("net.minecraft.server." + ver + ".ServerConnection")
  ),
  PLAYER_CONNECTION((ver, after) -> after ?
    Class.forName("net.minecraft.server.network.PlayerConnection") :
    Class.forName("net.minecraft.server." + ver + ".PlayerConnection")
  ),
  DATA_WATCHER((ver, after) -> after ?
    Class.forName("net.minecraft.network.syncher.DataWatcher") :
    Class.forName("net.minecraft.server." + ver + ".DataWatcher")
  ),
  ENTITY_TYPES((ver, after) -> after ?
    Class.forName("net.minecraft.world.entity.EntityTypes") :
    Class.forName("net.minecraft.server." + ver + ".EntityTypes")
  ),
  PLAYER_LIST((ver, after) -> after ?
    Class.forName("net.minecraft.server.players.PlayerList") :
    Class.forName("net.minecraft.server." + ver + ".PlayerList")
  ),
  RESOURCE_KEY((ver, after) -> after ?
    Class.forName("net.minecraft.resources.ResourceKey") :
    null
  ),
  PACKET_DATA_SERIALIZER((ver, after) -> after ?
    Class.forName("net.minecraft.network.PacketDataSerializer") :
    Class.forName("net.minecraft.server." + ver + ".PacketDataSerializer")
  ),
  ITEM((ver, after) -> after ?
    Class.forName("net.minecraft.world.item.Item") :
    Class.forName("net.minecraft.server." + ver + ".Item")
  ),
  ITEM_STACK((ver, after) -> after ?
    Class.forName("net.minecraft.world.item.ItemStack") :
    Class.forName("net.minecraft.server." + ver + ".ItemStack")
  ),
  GENERIC_ATTRIBUTES((ver, after) -> after ?
    Class.forName("net.minecraft.world.entity.ai.attributes.GenericAttributes") :
    Class.forName("net.minecraft.server." + ver + ".GenericAttributes")
  ),
  ATTRIBUTE_BASE((ver, after) -> after ?
    Class.forName("net.minecraft.world.entity.ai.attributes.AttributeBase") :
    Class.forName("net.minecraft.server." + ver + ".AttributeBase")
  ),
  CHAT_COMPONENT_TEXT((ver, after) -> after ?
    Class.forName("net.minecraft.network.chat.ChatComponentText") :
    Class.forName("net.minecraft.server." + ver + ".ChatComponentText")
  ),
  PACKET_O_MAP((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutMap") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutMap")
  ),
  PACKET_O_ENTITY_DESTROY((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutEntityDestroy")
  ),
  PACKET_O_ENTITY_TELEPORT((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutEntityTeleport")
  ),
  PACKET_O_ENTITY_METADATA((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutEntityMetadata")
  ),
  PACKET_O_SPAWN_ENTITY((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutSpawnEntity")
  ),
  PACKET_O_OPEN_WINDOW((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutOpenWindow") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutOpenWindow")
  ),
  PACKET_O_SET_SLOT((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSetSlot") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutSetSlot")
  ),
  PACKET_O_PLAYER_INFO((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutPlayerInfo")
  ),
  ENUM_PLAYER_INFO_ACTION((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo$EnumPlayerInfoAction") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
  ),
  PLAYER_INFO_DATA((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutPlayerInfo$PlayerInfoData") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutPlayerInfo$PlayerInfoData")
  ),
  PACKET_O_CHAT((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutChat") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutChat")
  ),
  PACKET_O_WINDOW_DATA((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutWindowData") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutWindowData")
  ),
  PACKET_O_TITLE((ver, after) -> after ?
    null :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutTitle")
  ),
  PACKET_O_LOGIN((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.login.PacketLoginOutSuccess") :
    Class.forName("net.minecraft.server." + ver + ".PacketLoginOutSuccess")
  ),
  PACKET_O_KEEP_ALIVE((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutKeepAlive") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutKeepAlive")
  ),
  PACKET_O_SCOREBOARD_TEAM((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutScoreboardTeam")
  ),
  ENUM_TITLE_ACTION((ver, after) -> after ?
    null :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutTitle$EnumTitleAction")
  ),
  CLIENTBOUND_TITLES_ANIMATION((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket") :
    null
  ),
  CLIENTBOUND_TITLE_SET((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket") :
    null
  ),
  CLIENTBOUND_SUBTITLE_SET((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket") :
    null
  ),
  CLIENTBOUND_SYSTEM_CHAT_PACKET((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.ClientboundSystemChatPacket") :
    null
  ),
  PACKET_I_B_EDIT((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInBEdit") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInBEdit")
  ),
  PACKET_I_SET_CREATIVE_SLOT((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInSetCreativeSlot") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInSetCreativeSlot")
  ),
  PACKET_I_ITEM_NAME((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInItemName") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInItemName")
  ),
  PACKET_I_HANDSHAKE((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.handshake.PacketHandshakingInSetProtocol") :
    Class.forName("net.minecraft.server." + ver + ".PacketHandshakingInSetProtocol")
  ),
  PACKET_I_LOGIN((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.login.PacketLoginInStart") :
    Class.forName("net.minecraft.server." + ver + ".PacketLoginInStart")
  ),
  PACKET_I_KEEP_ALIVE((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInKeepAlive") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInKeepAlive")
  ),
  PACKET_I_CHAT((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInChat") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInChat")
  ),
  TILE_ENTITY_FURNACE((ver, after) -> after ?
    Class.forName("net.minecraft.world.level.block.entity.TileEntityFurnace") :
    Class.forName("net.minecraft.server." + ver + ".TileEntityFurnace")
  ),
  NBT_READ_LIMITER((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTReadLimiter") :
    Class.forName("net.minecraft.server." + ver + ".NBTReadLimiter")
  ),
  NBT_TAG_LIST((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagList") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagList")
  ),
  NBT_LIST((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTList") :
    Class.forName("net.minecraft.server." + ver + ".NBTList")
  ),
  NBT_NUMBER((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTNumber") :
    Class.forName("net.minecraft.server." + ver + ".NBTNumber")
  ),
  NBT_TAG_COMPOUND((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagCompound") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagCompound")
  ),
  NBT_TAG_STRING((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagString") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagString")
  ),
  NBT_TAG_INT((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagInt") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagInt")
  ),
  NBT_TAG_INT_ARRAY((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagIntArray") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagIntArray")
  ),
  NBT_TAG_BYTE_ARRAY((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagByteArray") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagByteArray")
  ),
  NBT_TAG_LONG_ARRAY((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagLongArray") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagLongArray")
  ),
  NBT_TAG_FLOAT((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagFloat") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagFloat")
  ),
  NBT_TAG_BYTE((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagByte") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagByte")
  ),
  NBT_TAG_SHORT((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagShort") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagShort")
  ),
  NBT_TAG_LONG((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagLong") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagLong")
  ),
  NBT_TAG_DOUBLE((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTTagDouble") :
    Class.forName("net.minecraft.server." + ver + ".NBTTagDouble")
  ),
  NBT_BASE((ver, after) -> after ?
    Class.forName("net.minecraft.nbt.NBTBase") :
    Class.forName("net.minecraft.server." + ver + ".NBTBase")
  ),
  ENTITY((ver, after) -> after ?
    Class.forName("net.minecraft.world.entity.Entity") :
    Class.forName("net.minecraft.server." + ver + ".Entity")
  ),
  ENTITY_HUMAN((ver, after) -> after ?
    Class.forName("net.minecraft.world.entity.player.EntityHuman") :
    Class.forName("net.minecraft.server." + ver + ".EntityHuman")
  ),
  ENTITY_PLAYER((ver, after) -> after ?
    Class.forName("net.minecraft.server.level.EntityPlayer") :
    Class.forName("net.minecraft.server." + ver + ".EntityPlayer")
  ),
  CONTAINER((ver, after) -> after ?
    Class.forName("net.minecraft.world.inventory.Container") :
    Class.forName("net.minecraft.server." + ver + ".Container")
  ),
  CRAFT_ITEM_STACK((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".inventory.CraftItemStack")
  ),
  CRAFT_SERVER((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".CraftServer")
  ),
  CRAFT_MAP_VIEW((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".map.CraftMapView")
  ),
  CRAFT_META_ITEM((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".inventory.CraftMetaItem")
  ),
  CRAFT_PLAYER((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftPlayer")
  ),
  CRAFT_ITEM((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftItem")
  ),
  ENUM_GAME_MODE((ver, after) -> after ?
    Class.forName("net.minecraft.world.level.EnumGamemode") :
    Class.forName("net.minecraft.server." + ver + ".EnumGamemode")
  ),
  PROFILE_PUBLIC_KEY((ver, after) -> after ?
    Class.forName("net.minecraft.world.entity.player.ProfilePublicKey") :
    null
  )
  ;

  private final IUnsafeBiFunction<ServerVersion, Boolean, Class<?>, ClassNotFoundException> resolve;
  private static final Map<RClass, ClassHandle> cache;

  static {
    cache = new HashMap<>();
  }

  public ClassHandle resolve(ServerVersion version) throws ClassNotFoundException {
    ClassHandle res = cache.get(this);

    if (res != null)
      return res;

    Class<?> c = resolve.apply(version, version.getMinor() >= 17);

    if (c == null)
      throw new IllegalStateException("Could not resolve the target class");

    res = new ClassHandle(c, version);
    cache.put(this, res);

    return res;
  }
}
