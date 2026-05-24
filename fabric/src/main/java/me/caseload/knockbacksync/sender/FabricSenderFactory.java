package me.caseload.knockbacksync.sender;

import me.caseload.knockbacksync.FabricBase;
import me.caseload.knockbacksync.Base;
import me.caseload.knockbacksync.permission.FabricPermissionChecker;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.rcon.RconConsoleSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;

import java.util.UUID;

public class FabricSenderFactory extends SenderFactory<FabricBase, CommandSourceStack> implements SenderMapper<CommandSourceStack, Sender> {
    private final FabricBase plugin;

    public FabricSenderFactory(FabricBase kbSyncFabricBase) {
        super(kbSyncFabricBase);
        this.plugin = kbSyncFabricBase;
    }

    @Override
    protected FabricBase getPlugin() {
        return this.plugin;
    }

    @Override
    protected UUID getUniqueId(CommandSourceStack commandSource) {
        if (commandSource.getEntity() != null) {
            return commandSource.getEntity().getUUID();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(CommandSourceStack commandSource) {
        String name = commandSource.getTextName();
        if (commandSource.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    protected void sendMessage(CommandSourceStack sender, String message) {
        sender.sendSuccess(() -> Component.literal(message), false);
    }

    @Override
    protected boolean hasPermission(CommandSourceStack commandSource, String node) {
        return ((FabricPermissionChecker) Base.INSTANCE.getPermissionChecker()).hasPermission(commandSource, node);
    }

    @Override
    protected boolean hasPermission(CommandSourceStack commandSource, String node, boolean defaultIfUnset) {
        return ((FabricPermissionChecker) Base.INSTANCE.getPermissionChecker()).hasPermission(commandSource, node, defaultIfUnset);
    }

    @Override
    protected void performCommand(CommandSourceStack sender, String command) {
//        sender.getServer().getCommandManager().executeWithPrefix(sender, command);
    }

    @Override
    protected boolean isConsole(CommandSourceStack sender) {
        CommandSource output = sender.source;
        return output == sender.getServer()
                || output.getClass() == RconConsoleSource.class
                || (output == CommandSource.NULL && sender.getTextName().equals(""));
    }

    @Override
    public @NonNull Sender map(@NonNull CommandSourceStack base) {
        return this.wrap(base);
    }

    @Override
    public @NonNull CommandSourceStack reverse(@NonNull Sender mapped) {
        return this.unwrap(mapped);
    }

    @Override
    public void close() throws Exception {

    }
}
