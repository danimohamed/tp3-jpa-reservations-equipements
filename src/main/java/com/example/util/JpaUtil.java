package com.example.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Point d'acces unique a l'{@link EntityManagerFactory}.
 *
 * <p>L'{@code EntityManagerFactory} est un objet lourd et thread-safe : on n'en cree
 * qu'un seul pour toute l'application (patron singleton). Les {@link EntityManager},
 * eux, sont legers et ne doivent jamais etre partages entre threads : on en cree un
 * par unite de travail puis on le ferme.
 */
public final class JpaUtil {

    private static final String PERSISTENCE_UNIT = "gestion-reservations-pu";

    private static final EntityManagerFactory EMF =
            Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);

    private JpaUtil() {
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return EMF;
    }

    public static EntityManager createEntityManager() {
        return EMF.createEntityManager();
    }

    /** A appeler une seule fois, a l'arret de l'application. */
    public static void shutdown() {
        if (EMF.isOpen()) {
            EMF.close();
        }
    }
}
