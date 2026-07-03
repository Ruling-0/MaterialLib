package com.ruling_0.materiallib;

import java.lang.reflect.Field;

import net.minecraftforge.common.MinecraftForge;

import com.ruling_0.materiallib.api.MaterialRegistrationEvent;

import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.IEventListener;
import cpw.mods.fml.common.eventhandler.ListenerList;

/// Detects [MaterialRegistrationEvent] handlers subscribed after the event fired.
///
/// A late handler fails silently everywhere else: the bus accepts the subscription, the handler never runs,
/// and the mod's content simply does not exist. Comparing the event's listener list between the post and
/// postInit turns that silence into an error naming each late handler. The bus id needed to read the listener
/// list is private to [EventBus], so it is read reflectively; if that read fails, the check is skipped with a
/// warning rather than affecting startup.
final class LateHandlerCheck {

    private final int busId;
    private final ListenerList listeners;
    private final IEventListener[] subscribed;

    private LateHandlerCheck(int busId, ListenerList listeners) {
        this.busId = busId;
        this.listeners = listeners;
        this.subscribed = listeners.getListeners(busId);
    }

    /// Captures the handlers subscribed to `posted` at the time of the post, or null when the bus id cannot be
    /// read.
    static LateHandlerCheck snapshot(MaterialRegistrationEvent posted) {
        int busId;
        try {
            Field field = EventBus.class.getDeclaredField("busID");
            field.setAccessible(true);
            busId = field.getInt(MinecraftForge.EVENT_BUS);
        } catch (ReflectiveOperationException e) {
            MaterialLib.LOG.warn("Cannot detect late MaterialRegistrationEvent handlers", e);
            return null;
        }
        return new LateHandlerCheck(busId, posted.getListenerList());
    }

    /// Logs an error for every handler subscribed since the snapshot.
    void report() {
        for (IEventListener listener : listeners.getListeners(busId)) {
            if (!contains(subscribed, listener)) {
                MaterialLib.LOG.error(
                    "{} subscribed to MaterialRegistrationEvent after it fired, so its registrations were "
                        + "never collected. Subscribe during FMLConstructionEvent",
                    listener);
            }
        }
    }

    private static boolean contains(IEventListener[] listeners, IEventListener listener) {
        for (IEventListener candidate : listeners) {
            if (candidate == listener) return true;
        }
        return false;
    }
}
