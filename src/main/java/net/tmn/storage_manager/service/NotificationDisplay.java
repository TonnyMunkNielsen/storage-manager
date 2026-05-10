package net.tmn.storage_manager.service;

import net.tmn.storage_manager.database.jpa.Notification;

public record NotificationDisplay(Notification notification, String targetDisplay) {}
