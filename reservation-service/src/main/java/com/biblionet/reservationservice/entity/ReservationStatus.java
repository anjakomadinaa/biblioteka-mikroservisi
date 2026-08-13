package com.biblionet.reservationservice.entity;

public enum ReservationStatus {

    /** Clan ceka u redu za knjigu koja je trenutno pozajmljena. */
    WAITING,

    /** Knjiga je vracena i ovaj clan je bio prvi na redu. */
    NOTIFIED,

    /** Clan je odustao od rezervacije. */
    CANCELLED

}
