package com.meosea.meosea_storage;

import com.meosea.meosea_storage.fabric.command.StorageDebugCommand;
import com.meosea.meosea_storage.fabric.registry.ModScreenHandlers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeoSeaStorage implements ModInitializer {
	public static final String MOD_ID = "meosea-storage";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		ModScreenHandlers.register();

		// TODO: package net.fabricmc.fabric.api.command.v2 là suy đoán theo Fabric API các bản
		// gần đây mình biết — CHƯA kiểm chứng khớp bản 26.3. Nếu IDE báo lỗi "cannot resolve",
		// dùng tool decompile (Ctrl+T) tìm "CommandRegistrationCallback" để lấy đúng package.
		// Lệnh /storagetest chỉ để DEBUG — nhớ gỡ bỏ dòng dưới trước khi release bản chính thức.
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			StorageDebugCommand.register(dispatcher)
		);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}