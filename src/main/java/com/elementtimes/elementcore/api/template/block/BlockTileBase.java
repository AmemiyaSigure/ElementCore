package com.elementtimes.elementcore.api.template.block;

import com.elementtimes.elementcore.api.template.block.interfaces.IDismantleBlock;
import com.elementtimes.elementcore.api.template.tileentity.interfaces.IGuiProvider;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 需要带有 TileEntity 的方块时继承此类
 *
 * @author KSGFK create in 2019/2/17
 */
@SuppressWarnings("WeakerAccess")
public class BlockTileBase<T extends TileEntity> extends BlockContainer implements IDismantleBlock {

    private Class<T> mEntityClass;
    private int mGui;
    private Object mMod;

    private BlockTileBase(Material materialIn, int gui, Object mod) {
        super(materialIn);
        setHardness(15.0F);
        setResistance(25.0F);
        mGui = gui;
        mMod = mod;
    }

    public BlockTileBase(Class<T> entityClass, Object mod) {
        this(Material.IRON, 0, mod);
        this.mEntityClass = entityClass;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(@SuppressWarnings("NullableProblems") World worldIn, int meta) {
        try {
            if (mEntityClass != null) {
                for (Constructor<?> constructor : mEntityClass.getDeclaredConstructors()) {
                    constructor.setAccessible(true);
                    if (constructor.getParameterCount() == 0) {
                        return (TileEntity) constructor.newInstance();
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public EnumBlockRenderType getRenderType(IBlockState state) {//渲染类型设为普通方块
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity e = worldIn.getTileEntity(pos);
            if (e instanceof IGuiProvider) {
                playerIn.openGui(mMod, ((IGuiProvider) e).getGuiId(), worldIn, pos.getX(), pos.getY(), pos.getZ());
            } else {
                playerIn.openGui(mMod, mGui, worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        IDismantleBlock.super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    }

//    @Override
//    @SuppressWarnings("NullableProblems")
//    // 不知道要不要删除。使用这个结果是无法用稿子敲下来
//    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
//        if (worldIn.isRemote) return;
//        worldIn.setBlockToAir(pos);
//        TileEntity tile = worldIn.getTileEntity(pos);
//        if (tile != null) {
//            worldIn.removeTileEntity(pos);
//        }
//    }
}
