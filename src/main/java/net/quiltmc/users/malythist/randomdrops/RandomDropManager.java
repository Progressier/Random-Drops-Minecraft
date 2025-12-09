package net.quiltmc.users.malythist.randomdrops;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class RandomDropManager {

	private boolean active = false;

	private final Map<Identifier, LootMapping> blockMappings = new HashMap<>();
	private final Set<Item> usedItems = new HashSet<>();

	private List<Item> itemPool = Collections.emptyList();
	private final Random random = new Random();

	private int minCount = 1;
	private int maxCount = 100;

	public void setActive(boolean active) {
		this.active = active;
	}

	public void initItemPool() {
		this.itemPool = Registries.ITEM.stream()
			.filter(item -> item != Items.AIR)
			.filter(this::isAllowedItem)
			.toList();

		RandomDropsMod.LOGGER.info("[{}] item pool size: {}", RandomDropsMod.MOD_ID, itemPool.size());
	}

	private boolean isAllowedItem(Item item) {
		if (item instanceof SpawnEggItem) return false;

		if (item == Items.DEBUG_STICK) return false;

		Block block = Block.getBlockFromItem(item);
		if (block != Blocks.AIR) {

			if (block == Blocks.COMMAND_BLOCK
				|| block == Blocks.CHAIN_COMMAND_BLOCK
				|| block == Blocks.REPEATING_COMMAND_BLOCK) {
				return false;
			}

			if (block == Blocks.BARRIER
				|| block == Blocks.STRUCTURE_BLOCK
				|| block == Blocks.BEDROCK
				|| block == Blocks.JIGSAW
				|| block == Blocks.STRUCTURE_VOID) {
				return false;
			}
		}

		return true;
	}

	public boolean onBlockBreak(
		ServerWorld world,
		BlockPos pos,
		BlockState state,
		PlayerEntity player
	) {
		if (!active) {
			return true;
		}

		if (state.isAir()) {
			return true;
		}

		Block block = state.getBlock();
		if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
			return true;
		}

		Identifier blockId = Registries.BLOCK.getId(block);
		LootMapping mapping = blockMappings.computeIfAbsent(blockId, id -> createRandomLoot());
		world.breakBlock(pos, false, player);
		dropCustomLoot(world, pos, mapping);

		return false;
	}

	private void dropCustomLoot(ServerWorld world, BlockPos pos, LootMapping mapping) {
		if (mapping.item == null || mapping.count <= 0) return;

		ItemStack stack = new ItemStack(mapping.item, mapping.count);

		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;

		ItemEntity drop = new ItemEntity(world, x, y, z, stack);
		world.spawnEntity(drop);
	}

	private LootMapping createRandomLoot() {
		if (itemPool.isEmpty()) {
			initItemPool();
		}

		List<Item> available = itemPool.stream()
			.filter(item -> !usedItems.contains(item))
			.toList();

		if (available.isEmpty()) {
			available = itemPool;
		}

		Item item = available.get(random.nextInt(available.size()));
		usedItems.add(item);

		int range = Math.max(1, maxCount - minCount + 1);
		int count = minCount + random.nextInt(range);

		return new LootMapping(item, count);
	}

	public void setMinCount(int min) {
		if (min < 1) min = 1;
		this.minCount = min;
		if (this.maxCount < this.minCount) {
			this.maxCount = this.minCount;
		}
	}

	public void setMaxCount(int max) {
		if (max < 1) max = 1;
		this.maxCount = max;
		if (this.minCount > this.maxCount) {
			this.minCount = this.maxCount;
		}
	}

	public int getMinCount() {
		return minCount;
	}

	public int getMaxCount() {
		return maxCount;
	}

	private static class LootMapping {
		final Item item;
		final int count;

		LootMapping(Item item, int count) {
			this.item = item;
			this.count = count;
		}
	}
}
