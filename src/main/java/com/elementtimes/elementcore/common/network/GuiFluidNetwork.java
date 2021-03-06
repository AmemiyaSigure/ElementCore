package com.elementtimes.elementcore.common.network;

import com.elementtimes.elementcore.api.annotation.ModNetwork;
import com.elementtimes.elementcore.api.template.tileentity.SideHandlerType;
import com.elementtimes.elementcore.api.template.tileentity.lifecycle.HandlerInfoMachineLifecycle;
import com.elementtimes.elementcore.api.utils.FluidUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于流体参与的机器的通信
 * @author luqin2007
 */
@ModNetwork(handlerClass = "com.elementtimes.elementcore.common.network.GuiFluidNetwork$Handler", side = Side.CLIENT)
public class GuiFluidNetwork implements IMessage {

    private Map<SideHandlerType, Int2ObjectMap<FluidStack>> fluids = new HashMap<>();
    private Map<SideHandlerType, Int2IntMap> capabilities = new HashMap<>();
    private int guiType;
    private boolean isValid = false;

    public GuiFluidNetwork() { }

    public void put(int gui, HandlerInfoMachineLifecycle.FluidInfo infos) {
        guiType = gui;
        for (Map.Entry<SideHandlerType, IFluidHandler> entry : infos.fluids.entrySet()) {
            IFluidTankProperties[] properties = entry.getValue().getTankProperties();
            Int2ObjectMap<FluidStack> rFluids = new Int2ObjectArrayMap<>(properties.length);
            Int2IntMap rCapabilities = new Int2IntArrayMap(properties.length);
            for (int i = 0; i < properties.length; i++) {
                rFluids.put(i, properties[i].getContents());
                rCapabilities.put(i, properties[i].getCapacity());
            }
            fluids.put(entry.getKey(), rFluids);
            capabilities.put(entry.getKey(), rCapabilities);
        }
    }

    /**
     * int typeCount
     * [
     *  int gui
     *  String type
     *  int count
     *  [
     *      NBT fluid
     *      int capability
     *  ]
     * ]
     * @param buf bug
     */
    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            guiType = buf.readInt();
            int typeCount = buf.readInt();
            for (int t = 0; t < typeCount; t++) {
                SideHandlerType type = SideHandlerType.get(buf.readInt());
                int count = buf.readInt();
                Int2ObjectMap<FluidStack> rFluids = new Int2ObjectArrayMap<>(count);
                Int2IntMap rCapabilities = new Int2IntArrayMap(count);
                for (int i = 0; i < count; i++) {
                    int slot = buf.readInt();
                    FluidStack fluid = FluidStack.loadFluidStackFromNBT(ByteBufUtils.readTag(buf));
                    rFluids.put(slot, fluid);
                    rCapabilities.put(slot, buf.readInt());
                }
                fluids.put(type, rFluids);
                capabilities.put(type, rCapabilities);
            }
            isValid = true;
        } catch (Exception ignore) {}
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(guiType);
        buf.writeInt(fluids.size());
        fluids.keySet().forEach(type -> {
            buf.writeInt(type.id);
            Int2ObjectMap<FluidStack> rFluids = fluids.get(type);
            Int2IntMap rCapabilities = capabilities.get(type);
            buf.writeInt(rFluids.size());
            rFluids.keySet().forEach(slot -> {
                buf.writeInt(slot);
                FluidStack fluid = rFluids.get(slot);
                if (fluid == null) {
                    fluid = FluidUtils.EMPTY;
                }
                ByteBufUtils.writeTag(buf, fluid.writeToNBT(new NBTTagCompound()));
                buf.writeInt(rCapabilities.get(slot));
            });
        });
    }

    public static class Handler implements IMessageHandler<GuiFluidNetwork, IMessage> {

        public Handler() {}

        @Override
        synchronized public IMessage onMessage(GuiFluidNetwork message, MessageContext ctx) {
            synchronized (this) {
                if (message.isValid) {
                    Map<SideHandlerType, Int2ObjectMap<ImmutablePair<FluidStack, Integer>>> fluids = new HashMap<>();
                    message.fluids.keySet().forEach(type -> {
                        Int2ObjectMap<FluidStack> rFluids = message.fluids.get(type);
                        Int2IntMap rCapabilities = message.capabilities.get(type);
                        fluids.put(type, new Int2ObjectArrayMap<>(rFluids.size()));
                        rFluids.keySet().forEach(slot -> {
                            FluidStack fluidStack = rFluids.get(slot);
                            int capability = rCapabilities.get(slot);
                            fluids.get(type).put(slot, ImmutablePair.of(fluidStack, capability));
                        });
                    });
                    com.elementtimes.elementcore.api.template.gui.client.GuiDataFromServer.FLUIDS.put(message.guiType, fluids);
                }
            }
            return null;
        }
    }
}
