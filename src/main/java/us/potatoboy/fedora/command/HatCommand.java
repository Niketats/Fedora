package us.potatoboy.fedora.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import us.potatoboy.fedora.Fedora;
import us.potatoboy.fedora.Hat;
import us.potatoboy.fedora.HatManager;
import us.potatoboy.fedora.component.EntityHatComponent;
import us.potatoboy.fedora.component.PlayerHatComponent;

import java.util.Collection;

public class HatCommand {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LiteralCommandNode<ServerCommandSource> hatNode = CommandManager
                    .literal("hat")
                    .requires(source -> source.hasPermissionLevel(2))
                    .build();

            LiteralCommandNode<ServerCommandSource> unlockNode = CommandManager
                    .literal("unlock")
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .then(CommandManager.argument("hat", StringArgumentType.greedyString())
                                    .suggests(new HatSuggestionProvider())
                                    .executes(HatCommand::unlock)))
                    .build();

            LiteralCommandNode<ServerCommandSource> removeNode = CommandManager
                    .literal("remove")
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .then(CommandManager.argument("hat", StringArgumentType.greedyString())
                                    .suggests(new HatSuggestionProvider())
                                    .executes(HatCommand::remove)))
                    .build();

            LiteralCommandNode<ServerCommandSource> setNode = CommandManager
                    .literal("set")
                    .then(CommandManager.argument("target", EntityArgumentType.entities())
                            .then(CommandManager.argument("hat", StringArgumentType.greedyString())
                                    .suggests(new HatSuggestionProvider())
                                    .executes(HatCommand::set)))
                    .build();

            dispatcher.getRoot().addChild(hatNode);
            hatNode.addChild(unlockNode);
            hatNode.addChild(removeNode);
            hatNode.addChild(setNode);
        });
    }

    private static int set(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> target = EntityArgumentType.getEntities(ctx, "target");
        String hat = StringArgumentType.getString(ctx, "hat");

        for (Entity entity : target) {
            if (!(entity instanceof LivingEntity)) continue;

            if (!(entity instanceof ServerPlayerEntity)) {
                EntityHatComponent component = Fedora.ENTITY_HAT_COMPONENT.get(entity);

                component.setHat(HatManager.getFromID(hat));
                Fedora.ENTITY_HAT_COMPONENT.sync(entity);
            } else {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                PlayerHatComponent component = Fedora.PLAYER_HAT_COMPONENT.get(player);

                component.setCurrentHat(HatManager.getFromID(hat));
            }
        }

        ctx.getSource().sendFeedback(new TranslatableText("text.fedora.set_hat", target.size()), false);
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "target");
        String hatId = StringArgumentType.getString(context, "hat");

        for (Hat hat : HatManager.getHatRegistry()) {
            if (hat.id.equalsIgnoreCase(hatId)) {
                Fedora.PLAYER_HAT_COMPONENT.get(player).removeHat(hat);

                context.getSource().sendFeedback(new TranslatableText("text.fedora.hatremove", hat.id, player.getDisplayName()), true);
                return 1;
            }
        }

        context.getSource().sendError(new TranslatableText("text.fedora.invalid_hat"));
        return 0;
    }

    private static int unlock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity player = EntityArgumentType.getPlayer(context, "target");
        String hatId = StringArgumentType.getString(context, "hat");

        for (Hat hat : HatManager.getHatRegistry()) {
            if (hat.id.equalsIgnoreCase(hatId)) {
                Fedora.PLAYER_HAT_COMPONENT.get(player).unlockHat(hat);

                context.getSource().sendFeedback(new TranslatableText("text.fedora.hatunlock", hat.id,  player.getDisplayName()), true);
                return 1;
            }
        }

        context.getSource().sendError(new TranslatableText("text.fedora.invalid_hat"));
        return 0;
    }
}
