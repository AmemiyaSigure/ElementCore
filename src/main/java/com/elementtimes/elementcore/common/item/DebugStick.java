package com.elementtimes.elementcore.common.item;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * 大小锤子
 * 唯一指定调试工具
 * @author luqin2007
 */
public class DebugStick extends Item {

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        super.getSubItems(tab, items);
        if (items.removeIf(is -> is.getItem() == this)) {
            items.add(new ItemStack(this, 1, 0b0000));
            items.add(new ItemStack(this, 1, 0b0001));
        }
    }

    @Override
    @Nonnull
    @SuppressWarnings({"deprecation"})
        public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        if (stack.getMetadata() == 0b0000) {
            return TextFormatting.RED + I18n.translateToLocal("item.elementcore.debugstick.server.name");
        } else {
            return TextFormatting.BLUE + I18n.translateToLocal("item.elementcore.debugstick.client.name");
        }
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        final ItemStack heldItem = player.getHeldItem(hand);
        switch (heldItem.getMetadata()) {
            case 0b0000:
                if (!worldIn.isRemote) {
                    debug(worldIn, pos, player);
                }
                break;
            case 0b0001:
                if (worldIn.isRemote) {
                    debug(worldIn, pos, player);
                }
                break;
            default:
        }
        return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    }

    private void debug(World worldIn, BlockPos pos, EntityPlayer player) {
        Block block = worldIn.getBlockState(pos).getBlock();
        TileEntity te = worldIn.getTileEntity(pos);
        if (block == Blocks.AIR) {
            // 空气
            player.sendMessage(new TextComponentTranslation("chat.elementcore.debug.noblock", pos.getX(), pos.getY(), pos.getZ()));
        } else {
            debugTe(te, block, player);
        }
    }

    private void debugTe(TileEntity te, Block block, EntityPlayer player) {
        if (te == null) {
            // 无 te
            player.sendMessage(new TextComponentTranslation("chat.elementcore.debug.noblockte", new ItemStack(block).getDisplayName()));
        } else {
            NBTTagCompound nbt = te.writeToNBT(new NBTTagCompound());
            if (nbt.getKeySet().isEmpty()) {
                // 无 nbt
                player.sendMessage(new TextComponentTranslation("chat.elementcore.debug.nonbt", new ItemStack(block).getDisplayName()));
            } else {
                player.sendMessage(new TextComponentString(block.getLocalizedName()));
                sendDebugChat(player, "", nbt, 0);
                player.sendMessage(new TextComponentString("============================================================"));
            }
        }
    }

    private void sendDebugChat(EntityPlayer player, String lastKey, NBTBase nbt, int level) {
        StringBuilder space = new StringBuilder();
        for (int i = 1; i < level; i++) {
            space.append("    ");
        }
        if (nbt instanceof NBTTagCompound) {
            player.sendMessage(new TextComponentString(space.toString() + lastKey));
            ((NBTTagCompound) nbt).getKeySet().forEach(key -> sendDebugChat(player, key, ((NBTTagCompound) nbt).getTag(key), level + 1));
        } else {
            if (nbt instanceof NBTTagList) {
                for (int i = 0; i < ((NBTTagList) nbt).tagCount(); i++) {
                    sendDebugChat(player, lastKey + "[" + i + "]", ((NBTTagList) nbt).get(i), level);
                }
            } else if (nbt instanceof NBTTagByte) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + ((NBTTagByte) nbt).getByte()));
            } else if (nbt instanceof NBTTagDouble) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + ((NBTTagDouble) nbt).getDouble()));
            } else if (nbt instanceof NBTTagFloat) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + ((NBTTagFloat) nbt).getDouble()));
            } else if (nbt instanceof NBTTagInt) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + ((NBTTagInt) nbt).getDouble()));
            } else if (nbt instanceof NBTTagLong) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + ((NBTTagLong) nbt).getDouble()));
            } else if (nbt instanceof NBTTagShort) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + ((NBTTagShort) nbt).getDouble()));
            } else if (nbt instanceof NBTTagByteArray) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + Arrays.toString(((NBTTagByteArray) nbt).getByteArray())));
            } else if (nbt instanceof NBTTagIntArray) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + Arrays.toString(((NBTTagIntArray) nbt).getIntArray())));
            } else if (nbt instanceof NBTTagString) {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + ((NBTTagString) nbt).getString()));
            } else {
                player.sendMessage(new TextComponentString(space.toString() + lastKey + " = " + nbt.toString()));
            }
        }
    }
}
