package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;
import com.example.util.JpaUtil;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.time.LocalDate;

import static org.junit.Assert.*;

/**
 * Tests de la validation de periode portee par les methodes de rappel
 * {@code @PrePersist} / {@code @PreUpdate} de {@link Reservation}.
 */
public class ValidationReservationTest {

    @Before
    public void setUp() {
        TestSupport.resetDatabase();
    }

    @Test
    public void testPeriodeValideEstAcceptee() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Utilisateur eve = new Utilisateur("Eve", "eve@example.com");
        Reservation reservation = new Reservation(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), new Salle("Salle I909", 8));
        eve.addReservation(reservation);

        tx.begin();
        em.persist(eve);
        tx.commit();
        em.close();

        assertNotNull(reservation.getId());
        assertEquals(1L, TestSupport.compter("SELECT COUNT(r) FROM Reservation r"));
    }

    @Test
    public void testPeriodeInvalideEstRefusee() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Utilisateur frank = new Utilisateur("Frank", "frank@example.com");
        frank.addReservation(new Reservation(
                LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 1), new Salle("Salle J010", 8)));

        boolean exceptionLevee = false;
        try {
            tx.begin();
            em.persist(frank);
            tx.commit();
        } catch (RuntimeException e) {
            exceptionLevee = true;
            if (tx.isActive()) {
                tx.rollback();
            }
        } finally {
            em.close();
        }

        assertTrue("Une periode incoherente doit lever une exception", exceptionLevee);
        assertEquals("Rien ne doit avoir ete enregistre", 0L,
                TestSupport.compter("SELECT COUNT(r) FROM Reservation r"));
    }

    @Test
    public void testValiderPeriodeDetecteLesDatesManquantes() {
        Reservation incomplete = new Reservation();
        try {
            incomplete.validerPeriode();
            fail("Une reservation sans dates doit etre refusee");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("obligatoires"));
        }
    }
}
