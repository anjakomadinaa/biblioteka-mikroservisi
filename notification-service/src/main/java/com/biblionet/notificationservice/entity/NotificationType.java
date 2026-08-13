package com.biblionet.notificationservice.entity;

import java.util.Arrays;

public enum NotificationType {

    LOAN_CREATED,
    LOAN_RETURNED;

    /**
     * Mapira eventType iz LoanEvent poruke na tip notifikacije.
     * Baca IllegalArgumentException za nepoznat tip, da listener moze da odbaci poruku.
     */
    public static NotificationType fromEventType(String eventType) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(eventType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nepoznat tip dogadjaja: " + eventType));
    }

}
