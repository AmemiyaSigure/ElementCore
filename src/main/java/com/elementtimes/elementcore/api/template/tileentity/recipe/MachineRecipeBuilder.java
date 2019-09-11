package com.elementtimes.elementcore.api.template.tileentity.recipe;

import com.elementtimes.elementcore.api.template.interfaces.Function5;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * 用于辅助构建合成配方
 * @author luqin2007
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MachineRecipeBuilder {
    private ToIntFunction<MachineRecipeCapture> cost = (a) -> 0;
    private List<IngredientPart<ItemStack>> inputItems = new LinkedList<>();
    private List<IngredientPart<FluidStack>> inputFluids = new LinkedList<>();
    private List<IngredientPart<ItemStack>> outputItems = new LinkedList<>();
    private List<IngredientPart<FluidStack>> outputFluids = new LinkedList<>();
    private MachineRecipeHandler handler;

    public MachineRecipeBuilder(MachineRecipeHandler handler) {
        this.handler = handler;
    }

    /**
     * 增加耗能
     * @param energyCost 能量消耗
     * @return MachineRecipeBuilder
     */
    public MachineRecipeBuilder addCost(int energyCost) {
        ToIntFunction<MachineRecipeCapture> c = cost;
        cost = (a) -> c.applyAsInt(a) + energyCost;
        return this;
    }

    /**
     * 增加耗能
     * @param energyCost 能量消耗
     * @return MachineRecipeBuilder
     */
    public MachineRecipeBuilder addCost(ToIntFunction<MachineRecipeCapture> energyCost) {
        ToIntFunction<MachineRecipeCapture> c = cost;
        cost = (a) -> c.applyAsInt(a) + energyCost.applyAsInt(a);
        return this;
    }

    /**
     * 按顺序添加输入
     * @param item 输入物品
     * @return MachineRecipeBuilder
     */
    public MachineRecipeBuilder addItemInput(IngredientPart<ItemStack> item) {
        inputItems.add(item);
        return this;
    }

    /**
     * 按顺序添加多个输入
     * @param items 输入物品
     * @return MachineRecipeBuilder
     */
    @SafeVarargs
    public final MachineRecipeBuilder addItemInputs(IngredientPart<ItemStack>... items) {
        Collections.addAll(inputItems, items);
        return this;
    }

    /**
     * 按顺序添加输出
     * @param item 输出物品
     * @return MachineRecipeBuilder
     */
    public MachineRecipeBuilder addItemOutput(IngredientPart<ItemStack> item) {
        outputItems.add(item);
        return this;
    }

    /**
     * 按顺序添加多个输出
     * @param items 输出物品
     * @return MachineRecipeBuilder
     */
    @SafeVarargs
    public final MachineRecipeBuilder addItemOutputs(IngredientPart<ItemStack>... items) {
        Collections.addAll(outputItems, items);
        return this;
    }

    /**
     * 动态匹配物品输入
     * @param inputCheck 检查输入物品是否符合要求
     * @param inputConvert 根据输入物品获取实际输入物品种类及数量
     * @param allInputValues 所有可能的输入，用于jei
     * @return MachineRecipeBuilder
     */
    public MachineRecipeBuilder addItemInput(Predicate<ItemStack> inputCheck, Function<ItemStack, ItemStack> inputConvert, List<ItemStack> allInputValues) {
        return addItemInput(new IngredientPart<>(
                (recipe, slot, inputItems, inputFluids, input) -> inputCheck.test(input),
                (recipe, input, fluids, i, probability) ->
                        IngredientPart.rand.nextFloat() > probability ? ItemStack.EMPTY : inputConvert.apply(input.get(i)),
                () -> allInputValues));
    }

    /**
     * 动态匹配物品输出
     * @param outputGetter 根据合成表获取输出
     * @param allInputValues 所有可能的输出，用于jei
     * @return MachineRecipeBuilder
     */
    public MachineRecipeBuilder addItemOutput(Function5.StackGetter<ItemStack> outputGetter, List<ItemStack> allInputValues) {
        return addItemOutput(new IngredientPart<>(
                (recipe, slot, inputItems, inputFluids, input) -> true,
                outputGetter,
                () -> allInputValues));
    }

    public MachineRecipeBuilder addFluidOutput(IngredientPart<FluidStack> fluid) {
        outputFluids.add(fluid);
        return this;
    }

    public MachineRecipeBuilder addFluidInput(IngredientPart<FluidStack> fluid) {
        inputFluids.add(fluid);
        return this;
    }


    /**
     * 创建并添加配方
     * @return MachineRecipeHandler
     */
    public MachineRecipeHandler endAdd() {
        MachineRecipe recipe = new MachineRecipe();
        recipe.energy = cost;
        recipe.inputs = inputItems;
        recipe.fluidInputs = inputFluids;
        recipe.outputs = outputItems;
        recipe.fluidOutputs = outputFluids;
        handler.getMachineRecipes().add(recipe);
        return handler;
    }
}