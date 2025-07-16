package ch.uniport.gateway.proxy.listener;

import ch.uniport.gateway.proxy.config.model.DynamicModel;

/**
 * To receive announcements about dynamic configuration changes one has to
 * implement this interface.
 */
public interface Listener {

    void listen(DynamicModel model);
}
