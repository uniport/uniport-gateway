package com.inventage.portal.gateway.proxy.middleware.controlapi;

public enum ControlApiAction {

    SESSION_TERMINATE(ControlApiMiddlewareFactory.ACTION_SESSION_TERMINATE),

    SESSION_RESET(ControlApiMiddlewareFactory.ACTION_SESSION_RESET);

    private final String name;

    ControlApiAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
