package com.example;

import com.example.model.Equipement;
import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.model.Utilisateur;
import com.example.util.JpaUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;

/**
 * Demonstration de la gestion avancee des relations JPA :
 * cascades, orphanRemoval, ManyToMany avec table de jointure.
 *
 * <p>Lancement : {@code mvn clean compile exec:java}
 */
public class App {

    public static void main(String[] args) {
        EntityManager em = JpaUtil.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            // 1) Une salle et ses equipements
            Salle salleA = new Salle("Salle A101", 30);
            Equipement projecteur = new Equipement("Projecteur HD");
            Equipement tableau = new Equipement("Tableau blanc");
            salleA.addEquipement(projecteur);
            salleA.addEquipement(tableau);

            // 2) Un utilisateur et ses reservations
            Utilisateur alice = new Utilisateur("Alice Dupont", "alice@example.com");

            Reservation r1 = new Reservation(LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12), salleA);
            Reservation r2 = new Reservation(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3), salleA);
            alice.addReservation(r1);
            alice.addReservation(r2);

            // Un seul persist : les cascades entrainent Reservation -> Salle -> Equipement
            em.persist(alice);
            tx.commit();

            System.out.println("=== 1. Persistance en cascade (un seul persist) ===");
            System.out.println("Utilisateur  : " + alice);
            System.out.println("Reservations : " + alice.getReservations().size()
                    + " (persistees sans appel explicite a persist)");
            System.out.println("Salle        : " + salleA);
            System.out.println("Equipements  : " + salleA.getEquipements());
            System.out.println();

            System.out.println("=== 2. Table de jointure salle_equipement ===");
            List<Object[]> jointure = em.createNativeQuery(
                    "SELECT salle_id, equipement_id FROM salle_equipement").getResultList();
            for (Object[] ligne : jointure) {
                System.out.println("salle_id=" + ligne[0] + " -> equipement_id=" + ligne[1]);
            }
            System.out.println();

            System.out.println("=== 3. Disponibilite de la salle ===");
            System.out.println("Du 10/05 au 12/05 (deja reservee) : "
                    + salleA.estDisponible(LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12)));
            System.out.println("Du 20/05 au 22/05 (libre)        : "
                    + salleA.estDisponible(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 22)));
            System.out.println();

            // 3) orphanRemoval : retirer une reservation de la collection la supprime en base
            tx.begin();
            Utilisateur utilisateurGere = em.find(Utilisateur.class, alice.getId());
            Reservation aSupprimer = utilisateurGere.getReservations().iterator().next();
            System.out.println("=== 4. orphanRemoval : suppression de " + aSupprimer + " ===");
            utilisateurGere.removeReservation(aSupprimer);
            tx.commit();

            long restantes = em.createQuery("SELECT COUNT(r) FROM Reservation r", Long.class)
                    .getSingleResult();
            System.out.println("Reservations restantes : " + restantes);
            System.out.println();

            // 4) cascade REMOVE : supprimer l'utilisateur supprime ses reservations
            tx.begin();
            Utilisateur aSupprimerUtilisateur = em.find(Utilisateur.class, alice.getId());
            System.out.println("=== 5. cascade REMOVE : suppression de l'utilisateur ===");
            em.remove(aSupprimerUtilisateur);
            tx.commit();

            long utilisateurs = em.createQuery("SELECT COUNT(u) FROM Utilisateur u", Long.class)
                    .getSingleResult();
            long reservations = em.createQuery("SELECT COUNT(r) FROM Reservation r", Long.class)
                    .getSingleResult();
            long salles = em.createQuery("SELECT COUNT(s) FROM Salle s", Long.class)
                    .getSingleResult();
            long equipements = em.createQuery("SELECT COUNT(e) FROM Equipement e", Long.class)
                    .getSingleResult();
            System.out.println("Utilisateurs : " + utilisateurs + " | Reservations : " + reservations
                    + " | Salles : " + salles + " | Equipements : " + equipements);
            System.out.println("-> Les reservations ont disparu avec l'utilisateur,"
                    + " la salle et les equipements sont conserves.");
            System.out.println();

            // 5) Requete JPQL : salles equipees d'un projecteur
            TypedQuery<Salle> requete = em.createQuery(
                    "SELECT DISTINCT s FROM Salle s JOIN s.equipements e WHERE e.nom = :nom",
                    Salle.class);
            requete.setParameter("nom", "Projecteur HD");
            System.out.println("=== 6. JPQL : salles equipees d'un projecteur ===");
            System.out.println(requete.getResultList());

        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
            JpaUtil.shutdown();
        }
    }
}
