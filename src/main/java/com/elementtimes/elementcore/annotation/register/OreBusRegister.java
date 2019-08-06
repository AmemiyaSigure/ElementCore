package com.elementtimes.elementcore.annotation.register;

import com.elementtimes.elementcore.ElementContainer;
import com.elementtimes.elementcore.annotation.enums.GenType;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * 处理世界生成事件
 * @author luqin2007
 */
public class OreBusRegister {

    private ElementContainer mInitializer;

    public OreBusRegister(ElementContainer initializer) {
        mInitializer = initializer;
    }

    @SubscribeEvent
    public void onGenerateOre(OreGenEvent.Post event) {
        if (!event.getWorld().isRemote) {
            final List<WorldGenerator> worldGenerators = mInitializer.blockWorldGen.get(GenType.Ore);
            if (worldGenerators != null) {
                for (WorldGenerator generator: worldGenerators) {
                    if (TerrainGen.generateOre(event.getWorld(), event.getRand(), generator, event.getPos(), OreGenEvent.GenerateMinable.EventType.CUSTOM)) {
                        generator.generate(event.getWorld(), event.getRand(), event.getPos());
                    }
                }
            }
        }
    }
}