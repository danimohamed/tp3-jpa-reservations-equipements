package com.example;

import com.example.util.JpaUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/** Utilitaires partages par les tests : remise a zero de la base et comptages JPQL. */
final class TestSupport {

    private TestSupport() {
    }

    /**
     * Vide toutes les tables. L'ordre est impose par les cles etrangeres :
     * la table de jointure d'abord, puis les entites filles, puis les parents.
     */
    static void resetDatabase() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createNativeQuery("DELETE FROM salle_equipement").executeUpdate();
            em.createQuery("DELETE FROM Reservation").executeUpdate();
            em.createQuery("DELETE FROM Utilisateur").executeUpdate();
            em.createQuery("DELETE FROM Salle").executeUpdate();
            em.createQuery("DELETE FROM Equipement").executeUpdate();
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    static long compter(String jpql) {
        EntityManager em = JpaUtil.createEntityManager();
        try {
            return em.createQuery(jpql, Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    static long compterJointures() {
        EntityManager em = JpaUtil.createEntityManager();
        try {
            Object resultat = em.createNativeQuery("SELECT COUNT(*) FROM salle_equipement")
                    .getSingleResult();
            return ((Number) resultat).longValue();
        } finally {
            em.close();
        }
    }
}
