package com.elementtimes.elementcore.api.template.tileentity.lifecycle;

import com.elementtimes.elementcore.api.template.capability.EnergyHandler;
import com.elementtimes.elementcore.api.template.capability.fluid.ITankHandler;
import com.elementtimes.elementcore.api.template.capability.item.IItemHandler;
import com.elementtimes.elementcore.api.template.capability.item.ItemHandler;
import com.elementtimes.elementcore.api.template.tileentity.BaseTileEntity;
import com.elementtimes.elementcore.api.template.tileentity.SideHandlerType;
import com.elementtimes.elementcore.api.template.tileentity.interfaces.IMachineLifecycle;
import com.elementtimes.elementcore.api.template.tileentity.recipe.MachineRecipeCapture;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Map;

/**
 * 机器生命周期方法，用于配合 MachineRecipe 进行机器合成
 * @author luqin2007
 */
public class RecipeMachineLifecycle implements IMachineLifecycle {

    private final BaseTileEntity machine;
    private boolean needBind = true;

    private MachineRecipeCapture recipe;
    private final IItemHandler inputItems;
    private final ITankHandler inputTanks;
    private final IItemHandler outputItems;
    private final ITankHandler outputTanks;
    private final Int2IntMap mBindInputToOutputMap = new Int2IntOpenHashMap();

    public RecipeMachineLifecycle(final BaseTileEntity machine) {
        this.machine = machine;
        inputItems = machine.getItemHandler(SideHandlerType.INPUT);
        inputTanks = machine.getTanks(SideHandlerType.INPUT);
        outputItems = machine.getItemHandler(SideHandlerType.OUTPUT);
        outputTanks = machine.getTanks(SideHandlerType.OUTPUT);
        if (inputItems instanceof ItemHandler) {
            ((ItemHandler) inputItems).onItemChangeListener.add(slot -> {
                if (mBindInputToOutputMap.containsKey(slot)) {
                    ItemStack input = inputItems.getStackInSlot(slot);
                    outputItems.setStackInSlot(mBindInputToOutputMap.get(slot), input.copy());
                }
            });
        }
        if (outputItems instanceof ItemHandler) {
            ((ItemHandler) outputItems).onItemChangeListener.add(slot -> {
                if (mBindInputToOutputMap.containsValue(slot)) {
                    ItemStack output = outputItems.getStackInSlot(slot);
                    mBindInputToOutputMap.entrySet().stream()
                            .filter(kv -> kv.getValue().equals(slot))
                            .findFirst()
                            .map(Map.Entry::getKey)
                            .ifPresent(slotInput -> {
                                if (output.isEmpty() || inputItems.isItemValid(slotInput, output)) {
                                    mBindInputToOutputMap.remove(slotInput);
                                    inputItems.setSlotIgnoreChangeListener(slotInput, output);
                                }
                            });
                }
            });
            ((ItemHandler) outputItems).onUnbindAllListener.add(itemStacks -> {
                mBindInputToOutputMap.clear();
                needBind = true;
            });
        }
    }

    @Override
    public void onTickStart() {
        if (needBind) {
            for (int i = 0; i < inputItems.getSlots(); i++) {
                ItemStack is = inputItems.getStackInSlot(i);
                if (!is.isEmpty() && !inputItems.isItemValid(i, is)) {
                    bind(i);
                }
            }
            needBind = false;
        }
    }

    @Override
    public boolean onCheckStart() {
        // 合成表
        recipe = machine.getNextRecipe(inputItems, inputTanks);
        if (machine.isRecipeCanWork(recipe, inputItems, inputTanks)) {
            // 能量
            assert recipe != null;
            int change = recipe.energy;
            if (change > 0) {
                change = Math.min(change, machine.getEnergyTick());
                return machine.getEnergyHandler().extractEnergy(change, true) >= change;
            } else if (change < 0) {
                change = Math.min(-change, machine.getEnergyTick());
                return machine.getEnergyHandler().receiveEnergy(change, true) > 0;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        outputItems.unbindAll();
        needBind = false;
        assert recipe != null;
        machine.setWorkingRecipe(recipe);
        machine.setEnergyUnprocessed(recipe.energy);
        // items
        for (int i = recipe.inputs.size() - 1; i >= 0; i--) {
            ItemStack input = recipe.inputs.get(i);
            inputItems.extractItem(i, input.getCount(), false);
            if (input.getItem().hasContainerItem(input)) {
                inputItems.insertItemIgnoreValid(i, input.getItem().getContainerItem(input), false);
                bind(i);
            }
        }
    }

    @Override
    public boolean onLoop() {
        assert recipe != null;
        int unprocessed = machine.getEnergyUnprocessed();

        int change = 0;
        EnergyHandler handler = machine.getEnergyHandler();
        if (unprocessed > 0) {
            // 耗能过程
            change = Math.min(machine.getEnergyTick(), unprocessed);
            if (handler.extractEnergy(change, true) < change) {
                return false;
            }
            handler.extractEnergy(change, false);
            machine.processEnergy(change);
        } else if (unprocessed < 0) {
            // 产能过程
            change = Math.min(machine.getEnergyTick(), -unprocessed);
            change = handler.receiveEnergy(change, false);
            machine.processEnergy(change);
        }

        // 流体消耗/产出
        float a = (float) change / (float) Math.abs(machine.getEnergyProcessed() + machine.getEnergyUnprocessed());
        if (fluid(true, a)) {
            fluid(false, a);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean onCheckFinish() {
        return machine.getEnergyUnprocessed() == 0;
    }

    @Override
    public boolean onCheckResume() {
        recipe = machine.getWorkingRecipe();
        assert recipe != null;
        int change = Math.min(machine.getEnergyTick(), Math.abs(machine.getEnergyUnprocessed()));

        float a = (float) (Math.abs(machine.getEnergyProcessed()) + change) / (float) Math.abs(machine.getEnergyProcessed() + machine.getEnergyUnprocessed());
        if (machine.getWorkingRecipe().energy > 0) {
            return machine.getEnergyHandler().extractEnergy(change, true) >= change && fluid(true, a);
        }
		return machine.getEnergyHandler().receiveEnergy(change, true) > 0 && fluid(true, a);
    }

    @Override
    public boolean onFinish() {
        assert recipe != null;
        boolean output = output(true);
        if (output) {
            output(false);
        }
        return output;
    }

    private boolean output(boolean simulate) {
        int itemCount = recipe.outputs.size();
        boolean pushAll = true;
        // item
        for (int i = 0; pushAll && i < itemCount; i++) {
            ItemStack left = recipe.outputs.get(i);
            // 已有物品
            for (int j = 0; j < outputItems.getSlots(); j++) {
                ItemStack slot = outputItems.getStackInSlot(j);
                if (!slot.isEmpty() && slot.isItemEqual(left)) {
                    left = outputItems.insertItemIgnoreValid(j, left, simulate);
                }
            }
            // 空槽位
            for (int j = 0; j < outputItems.getSlots(); j++) {
                ItemStack slot = outputItems.getStackInSlot(j);
                if (slot.isEmpty()) {
                    left = outputItems.insertItemIgnoreValid(j, left, simulate);
                }
            }
            if (simulate) {
                pushAll = left.isEmpty();
            }
        }
        return pushAll;
    }

    private boolean fluid(boolean simulate, float a) {
        int max = Math.max(recipe.fluidInputs.size(), recipe.fluidOutputs.size());
        for (int i = 0; i < max; i++) {
            if (recipe.fluidInputs.size() > i) {
                final FluidStack fluid = recipe.fluidInputs.get(i);
                int amount = (int) (fluid.amount * a);
                if (simulate) {
                    FluidStack drain = inputTanks.drainIgnoreCheck(i, new FluidStack(fluid, amount), false);
                    if (drain == null || drain.amount < amount) {
                        return false;
                    }
                } else {
                    inputTanks.drainIgnoreCheck(i, new FluidStack(fluid, amount), true);
                }
            }

            if (recipe.fluidOutputs.size() > i) {
                final FluidStack fluid = recipe.fluidOutputs.get(i);
                int amount = (int) (fluid.amount * a);
                final FluidStack fillStack = new FluidStack(fluid, amount);
                if (simulate) {
                    int fill = outputTanks.fillIgnoreCheck(i, fillStack, false);
                    if (fill < amount) {
                        return false;
                    }
                } else {
                    outputTanks.fillIgnoreCheck(i, fillStack, true);
                }
            }
        }
        return true;
    }

    private void bind(int slotInput) {
        ItemStack input = inputItems.getStackInSlot(slotInput).copy();
        int bind = outputItems.bind(input);
        mBindInputToOutputMap.put(slotInput, bind);
    }
}