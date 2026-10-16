package com.example;

import com.example.model.Equipement;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;
import com.example.util.JpaUtil;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests des relations avancees : cascades, orphanRemoval et ManyToMany.
 */
public class RelationsJpaTest {

    private static final LocalDate D1 = LocalDate.of(2026, 5, 10);
    private static final LocalDate D2 = LocalDate.of(2026, 5, 12);

    @Before
    public void setUp() {
        TestSupport.resetDatabase();
    }

    @Test
    public void testPersistanceEnCascadeDepuisUtilisateur() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Salle salle = new Salle("Salle A101", 30);
        salle.addEquipement(new Equipement("Projecteur HD"));
        Utilisateur alice = new Utilisateur("Alice", "alice@example.com");
        alice.addReservation(new Reservation(D1, D2, salle));

        tx.begin();
        em.persist(alice);
        tx.commit();
        em.close();

        assertNotNull("L'identifiant de l'utilisateur doit etre genere", alice.getId());
        assertEquals(1L, TestSupport.compter("SELECT COUNT(u) FROM Utilisateur u"));
        assertEquals("La reservation doit etre persistee par cascade", 1L,
                TestSupport.compter("SELECT COUNT(r) FROM Reservation r"));
        assertEquals("La salle doit etre persistee par cascade depuis la reservation", 1L,
                TestSupport.compter("SELECT COUNT(s) FROM Salle s"));
        assertEquals("L'equipement doit etre persiste par cascade depuis la salle", 1L,
                TestSupport.compter("SELECT COUNT(e) FROM Equipement e"));
    }

    @Test
    public void testManyToManyEtTableDeJointure() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Salle salle = new Salle("Salle B203", 20);
        Equipement projecteur = new Equipement("Projecteur HD");
        Equipement tableau = new Equipement("Tableau blanc");
        salle.addEquipement(projecteur);
        salle.addEquipement(tableau);

        tx.begin();
        em.persist(salle);
        tx.commit();

        // Les deux cotes de la relation sont synchronises en memoire
        assertTrue(salle.getEquipements().contains(projecteur));
        assertTrue(projecteur.getSalles().contains(salle));

        // Et la table de jointure contient bien deux lignes
        assertEquals(2L, TestSupport.compterJointures());
        assertEquals(2L, TestSupport.compter("SELECT COUNT(e) FROM Equipement e"));

        em.close();
    }

    @Test
    public void testEquipementPartageEntrePlusieursSalles() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Equipement projecteur = new Equipement("Projecteur HD");
        Salle salle1 = new Salle("Salle A101", 30);
        Salle salle2 = new Salle("Salle B203", 20);
        salle1.addEquipement(projecteur);
        salle2.addEquipement(projecteur);

        tx.begin();
        em.persist(salle1);
        em.persist(salle2);
        tx.commit();
        em.close();

        assertEquals("L'equipement ne doit exister qu'une seule fois", 1L,
                TestSupport.compter("SELECT COUNT(e) FROM Equipement e"));
        assertEquals("Il doit etre lie aux deux salles", 2L, TestSupport.compterJointures());
    }

    @Test
    public void testOrphanRemovalSupprimeLaReservation() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Salle salle = new Salle("Salle C305", 15);
        Utilisateur bob = new Utilisateur("Bob", "bob@example.com");
        bob.addReservation(new Reservation(D1, D2, salle));
        bob.addReservation(new Reservation(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), salle));

        tx.begin();
        em.persist(bob);
        tx.commit();
        em.close();

        assertEquals(2L, TestSupport.compter("SELECT COUNT(r) FROM Reservation r"));

        em = JpaUtil.createEntityManager();
        tx = em.getTransaction();
        tx.begin();
        Utilisateur utilisateurGere = em.find(Utilisateur.class, bob.getId());
        assertEquals(2, utilisateurGere.getReservations().size());
        Reservation retiree = utilisateurGere.getReservations().iterator().next();
        Long idRetiree = retiree.getId();
        utilisateurGere.removeReservation(retiree);
        tx.commit();
        em.close();

        assertEquals("La reservation orpheline doit etre supprimee", 1L,
                TestSupport.compter("SELECT COUNT(r) FROM Reservation r"));

        em = JpaUtil.createEntityManager();
        assertNull("La reservation retiree ne doit plus exister",
                em.find(Reservation.class, idRetiree));
        em.close();
    }

    @Test
    public void testCascadeRemoveSupprimeLesReservationsMaisPasLaSalle() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Salle salle = new Salle("Salle D404", 40);
        salle.addEquipement(new Equipement("Videoprojecteur 4K"));
        Utilisateur carol = new Utilisateur("Carol", "carol@example.com");
        carol.addReservation(new Reservation(D1, D2, salle));

        tx.begin();
        em.persist(carol);
        tx.commit();
        em.close();

        em = JpaUtil.createEntityManager();
        tx = em.getTransaction();
        tx.begin();
        em.remove(em.find(Utilisateur.class, carol.getId()));
        tx.commit();
        em.close();

        assertEquals(0L, TestSupport.compter("SELECT COUNT(u) FROM Utilisateur u"));
        assertEquals("Les reservations partent avec l'utilisateur (cascade REMOVE)", 0L,
                TestSupport.compter("SELECT COUNT(r) FROM Reservation r"));
        assertEquals("La salle est un agregat independant", 1L,
                TestSupport.compter("SELECT COUNT(s) FROM Salle s"));
        assertEquals(1L, TestSupport.compter("SELECT COUNT(e) FROM Equipement e"));
    }

    @Test
    public void testRetirerUnEquipementSupprimeLaLigneDeJointure() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Salle salle = new Salle("Salle E505", 25);
        Equipement projecteur = new Equipement("Projecteur HD");
        salle.addEquipement(projecteur);
        salle.addEquipement(new Equipement("Tableau blanc"));

        tx.begin();
        em.persist(salle);
        tx.commit();
        em.close();

        assertEquals(2L, TestSupport.compterJointures());

        em = JpaUtil.createEntityManager();
        tx = em.getTransaction();
        tx.begin();
        Salle salleGeree = em.find(Salle.class, salle.getId());
        Equipement equipementGere = em.find(Equipement.class, projecteur.getId());
        salleGeree.removeEquipement(equipementGere);
        tx.commit();
        em.close();

        assertEquals(1L, TestSupport.compterJointures());
        assertEquals("L'equipement lui-meme est conserve", 2L,
                TestSupport.compter("SELECT COUNT(e) FROM Equipement e"));
    }

    @Test
    public void testDisponibiliteDeLaSalle() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Salle salle = new Salle("Salle F606", 12);
        Utilisateur dave = new Utilisateur("Dave", "dave@example.com");
        dave.addReservation(new Reservation(D1, D2, salle));

        tx.begin();
        em.persist(dave);
        tx.commit();

        assertFalse("Periode identique : salle occupee",
                salle.estDisponible(D1, D2));
        assertFalse("Chevauchement partiel : salle occupee",
                salle.estDisponible(LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 15)));
        assertTrue("Periode posterieure : salle libre",
                salle.estDisponible(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 22)));
        assertTrue("Periode anterieure : salle libre",
                salle.estDisponible(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 9)));

        em.close();
    }

    @Test
    public void testRequeteJpqlSallesEquipees() {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        Salle avecProjecteur = new Salle("Salle G707", 30);
        avecProjecteur.addEquipement(new Equipement("Projecteur HD"));
        Salle sansProjecteur = new Salle("Salle H808", 10);
        sansProjecteur.addEquipement(new Equipement("Tableau blanc"));

        tx.begin();
        em.persist(avecProjecteur);
        em.persist(sansProjecteur);
        tx.commit();

        TypedQuery<Salle> requete = em.createQuery(
                "SELECT DISTINCT s FROM Salle s JOIN s.equipements e WHERE e.nom = :nom", Salle.class);
        requete.setParameter("nom", "Projecteur HD");
        List<Salle> resultat = requete.getResultList();

        assertEquals(1, resultat.size());
        assertEquals("Salle G707", resultat.get(0).getNom());

        em.close();
    }
}
