package keystrokesmod.client.event.impl;

import keystrokesmod.client.event.types.Event;

public class ForgeEvent extends Event {

    private final cpw.mods.fml.common.eventhandler.Event event;

    public ForgeEvent(cpw.mods.fml.common.eventhandler.Event event) {
        this.event = event;
    }

    public cpw.mods.fml.common.eventhandler.Event getEvent() {
        return event;
    }

}
