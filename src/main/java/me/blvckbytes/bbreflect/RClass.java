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

import me.blvckbytes.bbreflect.handle.ClassHandle;
import me.blvckbytes.bbreflect.version.ServerVersion;
import me.blvckbytes.utilitytypes.FUnsafeBiFunction;

import java.util.HashMap;
import java.util.Map;

public enum RClass {
  ENUM_PROTOCOL((ver, after) -> after ?
    Class.forName("net.minecraft.network.EnumProtocol") :
    Class.forName("net.minecraft.server." + ver + ".EnumProtocol")
  ),
  INVENTORY_CLICK_TYPE((ver, after) -> after ?
    Class.forName("net.minecraft.world.inventory.InventoryClickType") :
    Class.forName("net.minecraft.server." + ver + ".InventoryClickType")
  ),
  ENUM_PROTOCOL_DIRECTION((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.EnumProtocolDirection") :
    Class.forName("net.minecraft.server." + ver + ".EnumProtocolDirection")
  ),
  PACKET_ENCODER((ver, after) -> after ?
    Class.forName("net.minecraft.network.PacketEncoder") :
    Class.forName("net.minecraft.server." + ver + ".PacketEncoder")
  ),
  MINECRAFT_KEY((ver, after) -> after ?
    Class.forName("net.minecraft.resources.MinecraftKey") :
    Class.forName("net.minecraft.server." + ver + ".MinecraftKey")
  ),
  CHANNEL_HANDLER_CONTEXT((ver, after) -> {
    if (ver.compare(ServerVersion.V1_8_R0) < 0)
      return Class.forName("net.minecraft.util.io.netty.channel.ChannelHandlerContext");
    return Class.forName("io.netty.channel.ChannelHandlerContext");
  }),
  BYTE_BUF((ver, after) -> {
    if (ver.compare(ServerVersion.V1_8_R0) < 0)
      return Class.forName("net.minecraft.util.io.netty.buffer.ByteBuf");
    return Class.forName("io.netty.buffer.ByteBuf");
  }),
  VEC3D((ver, after) -> after ?
    Class.forName("net.minecraft.world.phys.Vec3D") :
    Class.forName("net.minecraft.server." + ver + ".Vec3D")
  ),
  MATERIAL_MAP_COLOR((ver, after) -> after ?
    Class.forName("net.minecraft.world.level.material.MaterialMapColor") :
    Class.forName("net.minecraft.server." + ver + ".MaterialMapColor")
  ),
  PACKET_SEND_LISTENER((ver, after) -> {
    if (ver.compare(ServerVersion.V1_19_R1) >= 0)
      return Class.forName("net.minecraft.network.PacketSendListener");
    return null;
  }),
  GAME_PROFILE((ver, after) -> {
    if (ver.compare(ServerVersion.V1_7_R10) <= 0)
      return Class.forName("net.minecraft.util.com.mojang.authlib.GameProfile");
    return Class.forName("com.mojang.authlib.GameProfile");
  }),
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
  I_BLOCK_DATA((ver, after) -> after ?
    Class.forName("net.minecraft.world.level.block.state.IBlockData") :
    Class.forName("net.minecraft.server." + ver + ".IBlockData")
  ),
  BLOCK((ver, after) -> after ?
    Class.forName("net.minecraft.world.level.block.Block") :
    null
  ),
  SECTION_POSITION((ver, after) -> after ?
    Class.forName("net.minecraft.core.SectionPosition") :
    null
  ),
  BASE_BLOCK_POSITION((ver, after) -> after ?
    Class.forName("net.minecraft.core.BaseBlockPosition") :
    null
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
  CRAFT_COMMAND_MAP((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".command.CraftCommandMap")
  ),
  CRAFT_BLOCK((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".block.CraftBlock")
  ),
  CRAFT_BLOCK_DATA((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".block.data.CraftBlockData")
  ),
  CRAFT_BLOCK_STATE((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".block.CraftBlockState")
  ),
  CRAFT_WORLD((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".CraftWorld")
  ),
  CRAFT_TEAM((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".scoreboard.CraftTeam")
  ),
  CRAFT_SCOREBOARD((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".scoreboard.CraftScoreboard")
  ),
  CRAFT_SCOREBOARD_MANAGER((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".scoreboard.CraftScoreboardManager")
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
  STATISTIC_MANAGER((ver, after) -> after ?
    Class.forName("net.minecraft.stats.StatisticManager") :
    null
  ),
  SCOREBOARD_CRITERIA((ver, after) -> after ?
    Class.forName("net.minecraft.world.scores.criteria.IScoreboardCriteria") :
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
  PACKET_O_LOGIN_SUCCESS((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.login.PacketLoginOutSuccess") :
    Class.forName("net.minecraft.server." + ver + ".PacketLoginOutSuccess")
  ),
  PACKET_O_MULTI_BLOCK_CHANGE((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutMultiBlockChange") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutMultiBlockChange")
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
  PACKET_I_WINDOW_CLICK((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInWindowClick") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInWindowClick")
  ),
  PACKET_O_WINDOW_ITEMS((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutWindowItems") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutWindowItems")
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
  PACKET_O_ENTITY_STATUS((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityStatus") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutEntityStatus")
  ),
  PACKET_O_NAMED_ENTITY_SPAWN((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutNamedEntitySpawn") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutNamedEntitySpawn")
  ),
  PACKET_O_SCOREBOARD_TEAM((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutScoreboardTeam")
  ),
  ENUM_TITLE_ACTION((ver, after) -> after ?
    null :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayOutTitle$EnumTitleAction")
  ),
  CLIENTBOUND_LEVEL_CHUNK_WITH_LIGHT_PACKET((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket") :
    null
  ),
  CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData") :
    null
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
  PACKET_I_CLOSE_WINDOW((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInCloseWindow") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInCloseWindow")
  ),
  PACKET_I_FLYING((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInFlying") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInFlying")
  ),
  PACKET_I_LOOK((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInFlying$PacketPlayInLook") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInFlying$PacketPlayInLook")
  ),
  PACKET_I_POSITION((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInFlying$PacketPlayInPosition") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInFlying$PacketPlayInPosition")
  ),
  PACKET_I_POSITION_LOOK((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInFlying$PacketPlayInPositionLook") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInFlying$PacketPlayInPositionLook")
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
  PACKET_I_CUSTOM_PAYLOAD((ver, after) -> after ?
    Class.forName("net.minecraft.network.protocol.game.PacketPlayInCustomPayload") :
    Class.forName("net.minecraft.server." + ver + ".PacketPlayInCustomPayload")
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
  ENTITY_FIREWORKS((ver, after) -> after ?
    Class.forName("net.minecraft.world.entity.projectile.EntityFireworks") :
    Class.forName("net.minecraft.server." + ver + ".EntityFireworks")
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
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".inventory.CraftItemStack")
  ),
  CRAFT_ENTITY((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".entity.CraftEntity")
  ),
  CRAFT_SERVER((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".CraftServer")
  ),
  CRAFT_MAP_VIEW((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".map.CraftMapView")
  ),
  CRAFT_META_ITEM((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".inventory.CraftMetaItem")
  ),
  CRAFT_PLAYER((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".entity.CraftPlayer")
  ),
  CRAFT_ITEM((ver, after) ->
    Class.forName("org.bukkit.craftbukkit." + ver.bukkit + ".entity.CraftItem")
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

  private final FUnsafeBiFunction<ServerVersion, Boolean, Class<?>, ClassNotFoundException> resolve;
  private static final Map<RClass, ClassHandle> cache;

  RClass(FUnsafeBiFunction<ServerVersion, Boolean, Class<?>, ClassNotFoundException> resolve) {
    this.resolve = resolve;
  }

  static {
    cache = new HashMap<>();
  }

  public ClassHandle resolve(ServerVersion version) throws ClassNotFoundException {
    ClassHandle res = cache.get(this);

    if (res != null)
      return res;

    Class<?> c = resolve.apply(version, version.minor >= 17);

    if (c == null)
      throw new ClassNotFoundException("Could not resolve the target class");

    res = new ClassHandle(c, version);
    cache.put(this, res);

    return res;
  }
}
