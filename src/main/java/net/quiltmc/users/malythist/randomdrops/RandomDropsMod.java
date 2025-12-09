package net.quiltmc.users.malythist.randomdrops;


import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.command.CommandBuildContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomDropsMod implements ModInitializer {

	public static final String MOD_ID = "randomdrops";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final RandomDropManager DROP_MANAGER = new RandomDropManager();

	@Override
	public void onInitialize(ModContainer mod) {
		LOGGER.info("[{}] init", MOD_ID);

		DROP_MANAGER.initItemPool();

		registerCommands();
		registerEvents();
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register(this::registerRandomDropsCommand);
	}

	private void registerRandomDropsCommand(
		CommandDispatcher<ServerCommandSource> dispatcher,
		CommandBuildContext commandBuildContext,
		CommandManager.RegistrationEnvironment registrationEnvironment
	) {
		dispatcher.register(
			CommandManager.literal("randomdrops")
				.then(CommandManager.literal("start")
					.executes(ctx -> {
						DROP_MANAGER.setActive(true);
						ctx.getSource().sendFeedback(
							() -> Text.literal("Random Drops: случайный дроп включен"),
							false
						);
						return 1;
					}))
				.then(CommandManager.literal("stop")
					.executes(ctx -> {
						DROP_MANAGER.setActive(false);
						ctx.getSource().sendFeedback(
							() -> Text.literal("Random Drops: случайный дроп выключен"),
							false
						);
						return 1;
					}))
				.then(CommandManager.literal("set")
					.then(CommandManager.literal("min")
						.then(CommandManager.argument("value", IntegerArgumentType.integer(1))
							.executes(ctx -> {
								int value = IntegerArgumentType.getInteger(ctx, "value");
								RandomDropsMod.DROP_MANAGER.setMinCount(value);
								int min = RandomDropsMod.DROP_MANAGER.getMinCount();
								int max = RandomDropsMod.DROP_MANAGER.getMaxCount();
								ctx.getSource().sendFeedback(
									() -> Text.literal("RandomDrops: min=" + min + ", max=" + max),
									false
								);
								return 1;
							})))
					.then(CommandManager.literal("max")
						.then(CommandManager.argument("value", IntegerArgumentType.integer(1))
							.executes(ctx -> {
								int value = IntegerArgumentType.getInteger(ctx, "value");
								RandomDropsMod.DROP_MANAGER.setMaxCount(value);
								int min = RandomDropsMod.DROP_MANAGER.getMinCount();
								int max = RandomDropsMod.DROP_MANAGER.getMaxCount();
								ctx.getSource().sendFeedback(
									() -> Text.literal("RandomDrops: min=" + min + ", max=" + max),
									false
								);
								return 1;
							})))
				)
		);
	}

	private void registerEvents() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerWorld serverWorld)) {
				return true;
			}
			return DROP_MANAGER.onBlockBreak(serverWorld, pos, state, player);
		});
	}
}
