package org.samo_lego.clientstorage.fabric_client.casts;

import org.jetbrains.annotations.Nullable;
import org.samo_lego.clientstorage.fabric_client.storage.InteractableContainer;

import java.util.Optional;

public interface ICSPlayer {

    /**
     * Gets last container that player
     * has interacted with.
     *
     * @return last interacted container
     */
    Optional<InteractableContainer> cs_getLastInteractedContainer();

    /**
     * Sets last container that player has interacted with.
     * If player has interacted with a non-container, this should be set to null.
     *
     * @param container last interacted container.
     */

    void cs_setLastInteractedContainer(@Nullable InteractableContainer container);
}
