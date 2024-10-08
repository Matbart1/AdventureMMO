
package me.mrdaniel.adventuremmo.utils;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.data.type.SkullType;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.collect.Lists;

import me.mrdaniel.adventuremmo.AdventureMMO;
import me.mrdaniel.adventuremmo.catalogtypes.tools.ToolType;
import me.mrdaniel.adventuremmo.catalogtypes.tools.ToolTypes;
import me.mrdaniel.adventuremmo.data.manipulators.SuperToolData;

public class ItemUtils {

	@Nonnull
	public static ItemStack build(@Nonnull final ItemType type, final int amount, final int unsafe) {
		return ItemStack.builder()
				.fromContainer(DataContainer.createNew().set(DataQuery.of("ItemType"), type)
				.set(DataQuery.of("Count"), amount)
						.set(DataQuery.of("UnsafeDamage"), unsafe))
				.build();
	}

	@Nonnull
	public static Entity drop(@Nonnull final Location<World> loc, @Nonnull final ItemStackSnapshot item) {
		Entity e = loc.createEntity(EntityTypes.ITEM);
		e.offer(Keys.REPRESENTED_ITEM, item);
		e.offer(Keys.PICKUP_DELAY, 10);
		loc.getExtent().spawnEntity(e);
		// API 6 loc.getExtent().spawnEntity(e, ServerUtils.getSpawnCause(e));
		return e;
	}

	@Nonnull
	public static Optional<ItemStack> getHead(@Nonnull final EntityType type) {
		ItemStack item = ItemStack.builder().itemType(ItemTypes.SKULL).quantity(1).build();

		SkullType skull;
		if (type == EntityTypes.ZOMBIE) {
			skull = SkullTypes.ZOMBIE;
		} else if (type == EntityTypes.SKELETON) {
			skull = SkullTypes.SKELETON;
		} else if (type == EntityTypes.CREEPER) {
			skull = SkullTypes.CREEPER;
		} else if (type == EntityTypes.WITHER_SKELETON) {
			skull = SkullTypes.WITHER_SKELETON;
		} else if (type == EntityTypes.ENDER_DRAGON) {
			skull = SkullTypes.ENDER_DRAGON;
		} else
			return Optional.empty();

		item.offer(Keys.SKULL_TYPE, skull);
		return Optional.of(item);
	}

	@Nonnull
	public static ItemStack getPlayerHead(@Nonnull final Player p) {
		return ItemStack.builder().itemType(ItemTypes.SKULL).add(Keys.SKULL_TYPE, SkullTypes.PLAYER)
				.add(Keys.REPRESENTED_PLAYER, p.getProfile()).build();
	}

	public static void giveSuperTool(@Nonnull final Player p, @Nonnull final ToolType tool) {
		ItemStack item = p.getItemInHand(HandTypes.MAIN_HAND).get();

		item.offer(new SuperToolData(item.get(Keys.ITEM_ENCHANTMENTS).orElse(Lists.newArrayList()),
				TextUtils.toString(item.get(Keys.DISPLAY_NAME).orElse(Text.of(""))),
				item.get(Keys.ITEM_DURABILITY).orElse(0)));
		item.offer(Keys.DISPLAY_NAME, Text.of(TextColors.RED, TextStyles.BOLD, "Super ", tool.getName()));
		item.offer(Keys.UNBREAKABLE, true);

		final boolean rod = tool == ToolTypes.ROD;
		int lvl = rod ? 3 : 5;
		int max_lvl = rod ? 5 : 10;
		EnchantmentType type = rod ? EnchantmentTypes.LURE : EnchantmentTypes.EFFICIENCY;

		List<Enchantment> ench = item.get(Keys.ITEM_ENCHANTMENTS).orElse(Lists.newArrayList());
		for (Enchantment enchant : ench) {
			if (enchant.getType() == type) {
				lvl += enchant.getLevel();
				ench.remove(enchant);
				break;
			}
		}
		ench.add(Enchantment.of(type, MathUtils.between(lvl, 1, max_lvl)));
		item.offer(Keys.ITEM_ENCHANTMENTS, ench);

		p.setItemInHand(HandTypes.MAIN_HAND, item);
	}

	public static void restoreSuperTool(@Nonnull final Player p, @Nonnull final PluginContainer container) {
		p.closeInventory();
		// API 6 p.closeInventory(ServerUtils.getCause(container,
		// NamedCause.of("player", p)));
		p.getInventory().slots().forEach(slot -> slot.peek()
				.ifPresent(item -> item.get(SuperToolData.class).ifPresent(data -> slot.set(data.restore(item)))));
	}

	@Nonnull
	public static ItemStack enchant(@Nonnull final AdventureMMO mmo, @Nonnull final ItemStack item) {
		List<Enchantment> enchants = Lists.newArrayList();
		mmo.getGame().getRegistry().getAllOf(EnchantmentType.class).forEach(ench -> {
			if (Math.random() > 0.9 && ench.canBeAppliedByTable(item)) {
				enchants.add(Enchantment.of(ench, (int) (Math.random() * ench.getMaximumLevel() + 1)));
			}
		});
		item.offer(Keys.ITEM_ENCHANTMENTS, enchants);
		return item;
	}
}
