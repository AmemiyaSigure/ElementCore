package com.elementtimes.elementcore.api.template.tileentity.recipe;

import com.elementtimes.elementcore.api.common.ECUtils;
import com.elementtimes.elementcore.api.template.interfaces.Function5;
import com.elementtimes.elementcore.api.utils.FluidUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreIngredient;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 合成表匹配
 * @author luqin2007
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "WeakerAccess", "unchecked"})
public class IngredientPart<T> {

    private static String SYMBOL_ITEM_ALL = "[all]";
    private static String SYMBOL_ITEM_ORE = "[ore]";
    private static String SYMBOL_ITEM_ID = "[id]";
    protected static final Random RAND = new Random(System.currentTimeMillis());

    /**
     * 测试输入是否匹配
     * 用于机器检索合成表
     */
    public Function5.Match<T> matcher;

    /**
     * 测试输入是否匹配
     * 用于机器验证输入物品
     */
    public Function5.Match<T> accept;

    /**
     * 根据输入获取实际物品及数量
     * 用于获取输入输出物品
     */
    public Function5.StackGetter<T> getter;

    /**
     * 获取所有可用值
     * 用于 Jei 兼容
     */
    public Supplier<List<T>> allViableValues;

    /**
     * 概率
     */
    public float probability = 1.0f;

    public List<String> tooltips = new ArrayList<>();

    public IngredientPart(Function5.Match<T> matcher, Function5.Match<T> accept, Function5.StackGetter<T> getter, Supplier<List<T>> allViableValues) {
        this.getter = getter;
        this.matcher = matcher;
        this.accept = accept;
        this.allViableValues = allViableValues;
    }

    private IngredientPart() {}

    public IngredientPart<T> withProbability(float probability) {
        this.probability = probability;
        return this;
    }

    public IngredientPart<T> withStrings(String... strings) {
        Collections.addAll(tooltips, strings);
        return this;
    }

    public IngredientPart<T> withStrings(Collection<String> strings) {
        tooltips.addAll(strings);
        return this;
    }

    public static IngredientPart<ItemStack> forItem(Ingredient ingredient, int count) {
        Function5.Match<ItemStack> match = (recipe, slot, inputItems, inputFluids, input) -> ingredient.apply(input);
        Function5.StackGetter<ItemStack> get = (recipe, items, fluids, slot, probability) -> {
            if (RAND.nextFloat() > probability) {
                return ItemStack.EMPTY;
            }

            if (items != null && !items.isEmpty() && !items.get(slot).isEmpty()) {
                return ItemHandlerHelper.copyStackWithSize(items.get(slot), count);
            }
			ItemStack[] stacks = ingredient.getMatchingStacks();
			if (stacks.length == 0) {
			    return ItemStack.EMPTY;
			}
			return ItemHandlerHelper.copyStackWithSize(stacks[0], count);
        };
        Supplier<List<ItemStack>> allViableValues = () ->
                Arrays.stream(ingredient.getMatchingStacks())
                        .map(itemStack -> ItemHandlerHelper.copyStackWithSize(itemStack, count))
                        .collect(Collectors.toList());
        Function5.Match<ItemStack> accept = (recipe, slot, inputItems, inputFluids, input) -> {
            for (ItemStack stack : ingredient.getMatchingStacks()) {
                if (ECUtils.item.isItemRawEqual(stack, input)) {
                    return true;
                }
            }
            return false;
        };
        return new IngredientPart<>(match, accept, get, allViableValues);
    }

    public static IngredientPart<ItemStack> forItem(ItemStack itemStack) {
        Function5.Match<ItemStack> match = (recipe, slot, inputItems, inputFluids, input) -> {
            if (itemStack.isItemEqual(input)) {
                return itemStack.getCount() <= input.getCount();
            }
            return false;
        };
        Function5.StackGetter<ItemStack> get = (recipe, items, fluids, slot, probability) ->
                RAND.nextFloat() > probability ? ItemStack.EMPTY : itemStack.copy();
        Supplier<List<ItemStack>> allViableValues = () -> Collections.singletonList(itemStack);
        Function5.Match<ItemStack> accept = (recipe, slot, inputItems, inputFluids, input) -> ECUtils.item.isItemRawEqual(input, itemStack);
        return new IngredientPart<>(match, accept, get, allViableValues);
    }

    public static IngredientPart<ItemStack> forItem(Item item, int count) {
        Function5.Match<ItemStack> match = (recipe, slot, inputItems, inputFluids, input) -> {
            if (input != null && item == input.getItem()) {
                return count <= input.getCount();
            }
            return false;
        };
        Function5.StackGetter<ItemStack> get = (recipe, items, fluids, slot, probability) ->
                RAND.nextFloat() > probability ? ItemStack.EMPTY : new ItemStack(item, count);
        Supplier<List<ItemStack>> allViableValues = () -> Collections.singletonList(new ItemStack(item, count));
        Function5.Match<ItemStack> accept = (recipe, slot, inputItems, inputFluids, input) -> input.getItem() == item;
        return new IngredientPart<>(match, accept, get, allViableValues);
    }

    public static IngredientPart<ItemStack> forItem(Block block, int count) {
        return forItem(Item.getItemFromBlock(block), count);
    }
    
    public static IngredientPart<ItemStack> forItem(String nameOrOreName, int count) {
        if (nameOrOreName.startsWith(SYMBOL_ITEM_ID)) {
            String name = nameOrOreName.substring(4);
            Item item = Item.getByNameOrId(name);
            return IngredientPart.forItem(item, count);
        } else if (nameOrOreName.startsWith(SYMBOL_ITEM_ORE)) {
            String name = nameOrOreName.substring(5);
            Ingredient ingredient = new OreIngredient(name);
            return IngredientPart.forItem(ingredient, count);
        } else if (nameOrOreName.startsWith(SYMBOL_ITEM_ALL)) {
            String name = nameOrOreName.substring(5);
            Item item = Item.getByNameOrId(name);
            Ingredient ingredientOre = new OreIngredient(name);
            Ingredient ingredientItem = Ingredient.fromItem(item == null ? Items.AIR : item);
            return IngredientPart.forItem(Ingredient.merge(Arrays.asList(ingredientItem, ingredientOre)), count);
        } else {
            Item item = Item.getByNameOrId(nameOrOreName);
            if (item == null || item == Items.AIR) {
                return IngredientPart.forItem(new OreIngredient(nameOrOreName), count);
            }
			return IngredientPart.forItem(item, count);
        }
    }

    public static IngredientPart<ItemStack> forItemRandom(ItemStack itemStack, float probability) {
        return forItem(itemStack).withProbability(probability);
    }

    public static IngredientPart<FluidStack> forFluid(FluidStack fluidStack) {
        Function5.Match<FluidStack> match = (recipe, slot, inputItems, inputFluids, input) -> input != null && input.containsFluid(fluidStack);
        Function5.StackGetter<FluidStack> get = (recipe, items, fluids, slot, probability) ->
                RAND.nextFloat() > probability ? null : fluidStack.copy();
        Supplier<List<FluidStack>> allViableValues = () -> Collections.singletonList(fluidStack);
        Function5.Match<FluidStack> accept = (recipe, slot, inputItems, inputFluids, input) -> fluidStack.isFluidEqual(input);
        return new IngredientPart<>(match, accept, get, allViableValues);
    }

    public static IngredientPart<FluidStack> forFluid(Fluid fluid, int amount) {
        Function5.Match<FluidStack> match = (recipe, slot, inputItems, inputFluids, input) -> input != null && fluid == input.getFluid() && amount <= input.amount;
        Function5.StackGetter<FluidStack> get = (recipe, items, fluids, slot, probability) ->
                RAND.nextFloat() > probability ? null : new FluidStack(fluid, amount);
        Supplier<List<FluidStack>> allViableValues = () -> Collections.singletonList(new FluidStack(fluid, amount));
        Function5.Match<FluidStack> accept = (recipe, slot, inputItems, inputFluids, input) -> input != null && fluid == input.getFluid();
        return new IngredientPart<>(match, accept, get, allViableValues);
    }

    public static IngredientPart<FluidStack> forFluidRandom(FluidStack fluidStack, float probability) {
        return forFluid(fluidStack).withProbability(probability);
    }

    public static <T> IngredientPart<T> combine(Predicate<T> isEmpty, T emptyStack, IngredientPart<T>... ingredientParts) {
        return new IngredientPart<T>() {
            private final IngredientPart<T>[] contains = ingredientParts;

            {
                matcher = (recipe, slot, inputItems, inputFluids, input) -> {
                    for (IngredientPart<T> part : contains) {
                        if (!part.matcher.apply(recipe, slot, inputItems, inputFluids, input)) {
                            return false;
                        }
                    }
                    return true;
                };

                getter = (recipe, items, fluids, slot, probability) -> {
                    for (IngredientPart<T> part : contains) {
                        T stack = part.getter.apply(recipe, items, fluids, slot, probability);
                        if (!isEmpty.test(stack)) {
                            return stack;
                        }
                    }
                    return emptyStack;
                };

                accept = (recipe, slot, inputItems, inputFluids, input) -> {
                    for (IngredientPart<T> part : contains) {
                        if (part.accept.apply(recipe, slot, inputItems, inputFluids, input)) {
                            return true;
                        }
                    }
                    return true;
                };

                allViableValues = () -> Arrays.stream(contains)
                        .map(c -> c.allViableValues)
                        .map(Supplier::get)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
            }
        };
    }

    public static <T> IngredientPart<T> combineNull(IngredientPart<T>... ingredientParts) {
        return combine(Objects::isNull, null, ingredientParts);
    }

    public static IngredientPart<ItemStack> combineItem(IngredientPart<ItemStack>... ingredientParts) {
        return combine(IS_EMPTY_ITEM, ItemStack.EMPTY, ingredientParts);
    }

    public static final Predicate<ItemStack> IS_EMPTY_ITEM = (is) -> is == ItemStack.EMPTY || is.isEmpty();

    public static IngredientPart<ItemStack> EMPTY_ITEM = new IngredientPart<>(
            (recipe, slot, inputItems, inputFluids, input) -> true,
            (recipe, slot, inputItems, inputFluids, input) -> false,
            (recipe, items, fluids, slot, probability) -> ItemStack.EMPTY,
            () -> Collections.singletonList(ItemStack.EMPTY));

    public static IngredientPart<FluidStack> EMPTY_FLUID = new IngredientPart<>(
            (recipe, slot, inputItems, inputFluids, input) -> true,
            (recipe, slot, inputItems, inputFluids, input) -> false,
            (recipe, items, fluids, slot, probability) -> FluidUtils.EMPTY,
            () -> Collections.singletonList(FluidUtils.EMPTY));
}
