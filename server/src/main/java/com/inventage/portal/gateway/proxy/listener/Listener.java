package com.inventage.portal.gateway.proxy.listener;

import com.inventage.portal.gateway.proxy.config.model.DynamicModel;

/**
 * To receive announcements about dynamic configuration changes one has to
 * implement this interface.
 */
public interface Listener {

    void listen(DynamicModel model);
}
