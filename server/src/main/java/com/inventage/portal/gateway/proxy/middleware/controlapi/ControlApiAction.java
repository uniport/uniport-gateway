package com.inventage.portal.gateway.proxy.middleware.controlapi;

import com.inventage.portal.gateway.proxy.config.dynamic.DynamicConfiguration;

public enum ControlApiAction {
    SESSION_TERMINATE(DynamicConfiguration.MIDDLEWARE_CONTROL_API_ACTION_SESSION_TERMINATE), SESSION_RESET(DynamicConfiguration.MIDDLEWARE_CONTROL_API_ACTION_SESSION_RESET);

    private final String name;

    ControlApiAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
