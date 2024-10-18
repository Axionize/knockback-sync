package me.caseload.knockbacksync.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import com.mojang.brigadier.context.CommandContext;
import me.caseload.knockbacksync.command.CommandOperations;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class CommandUtil {

    private static CommandOperations operations;

    public static void setOperations(CommandOperations operations) {
        CommandUtil.operations = operations;
    }

    public static void sendSuccessMessage(CommandContext<CommandSourceStack> context, String message) {
        sendSuccessMessage(context, () -> getComponent(message), false);
    }

    public static void sendSuccessMessage(CommandContext<CommandSourceStack> context, String message, boolean allowLogging) {
        sendSuccessMessage(context, () -> getComponent(message), allowLogging);
    }

    public static void sendSuccessMessage(CommandContext<CommandSourceStack> context, Component message) {
        sendSuccessMessage(context, () -> message, false);
    }

    public static void sendSuccessMessage(CommandContext<CommandSourceStack> context, Component message, boolean allowLogging) {
        sendSuccessMessage(context, () -> message, allowLogging);
    }

    public static void sendSuccessMessage(CommandContext<CommandSourceStack> context, Supplier<Component> messageSupplier) {
        sendSuccessMessage(context, messageSupplier, false);
    }

    public static void sendSuccessMessage(CommandContext<CommandSourceStack> context, Supplier<Component> messageSupplier, boolean allowLogging) {
        operations.sendSuccess(context.getSource(), messageSupplier, allowLogging);
    }

    public static void sendFailureMessage(CommandContext<CommandSourceStack> context, String message) {
        sendFailureMessage(context, getComponent(message));
    }

    public static void sendFailureMessage(CommandContext<CommandSourceStack> context, Component message) {
        operations.sendFailure(context.getSource(), () -> message);
    }

    public static Component getComponent(String message) {
        return operations.createComponent(message);
    }
}