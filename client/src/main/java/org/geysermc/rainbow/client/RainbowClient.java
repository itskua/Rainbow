package org.geysermc.rainbow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.client.command.CommandSuggestionsArgumentType;
import org.geysermc.rainbow.client.command.PackGeneratorCommand;
import org.geysermc.rainbow.client.editor.ConverterCalibrationManager;
import org.geysermc.rainbow.client.editor.TransformOverrideManager;
import org.geysermc.rainbow.client.mapper.PackMapper;

public class RainbowClient implements ClientModInitializer {
    private static RainbowClient instance;

    private final PackManager packManager = new PackManager();
    private final PackMapper packMapper = new PackMapper(packManager);

    // TODO export language overrides
    @Override
    public void onInitializeClient() {
        instance = this;
        ConverterCalibrationManager.load();
        TransformOverrideManager.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> PackGeneratorCommand.register(dispatcher, packManager, packMapper));
        ClientTickEvents.START_CLIENT_TICK.register(packMapper::tick);

        ArgumentTypeRegistry.registerArgumentType(Rainbow.getModdedIdentifier("command_suggestions"),
                CommandSuggestionsArgumentType.class, SingletonArgumentInfo.contextFree(CommandSuggestionsArgumentType::new));

        RainbowIO.registerExceptionListener(new RainbowClientIOHandler());
    }

    public static RainbowClient getInstance() {
        return instance;
    }

    public PackManager getPackManager() {
        return packManager;
    }

    public PackMapper getPackMapper() {
        return packMapper;
    }
}
