package com.inventage.portal.gateway.proxy.listener;

import com.inventage.portal.gateway.proxy.model.Gateway;

/**
 * To receive announcements about dynamic configuration changes one has to implement this interface.
 */
public interface Listener {

    void listen(Gateway model);
}
