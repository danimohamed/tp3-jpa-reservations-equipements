package com.example.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Utilisateur de l'application.
 *
 * <p>C'est le <strong>proprietaire du cycle de vie</strong> des reservations :
 * {@code cascade = ALL} + {@code orphanRemoval = true} signifient que
 * <ul>
 *   <li>persister un utilisateur persiste ses reservations,</li>
 *   <li>supprimer un utilisateur supprime ses reservations,</li>
 *   <li>retirer une reservation de la collection la supprime de la base (orphelin).</li>
 * </ul>
 */
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nom;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservations = new HashSet<>();

    public Utilisateur() {
    }

    public Utilisateur(String nom, String email) {
        this.nom = nom;
        this.email = email;
    }

    /**
     * Ajoute une reservation et maintient les deux cotes de la relation :
     * la collection de l'utilisateur ET le champ {@code utilisateur} de la reservation.
     */
    public void addReservation(Reservation reservation) {
        this.reservations.add(reservation);
        reservation.setUtilisateur(this);
    }

    /**
     * Retire une reservation. Comme {@code orphanRemoval = true}, la reservation
     * est supprimee de la base au prochain flush.
     */
    public void removeReservation(Reservation reservation) {
        this.reservations.remove(reservation);
        reservation.setUtilisateur(null);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Set<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(Set<Reservation> reservations) {
        this.reservations = reservations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Utilisateur)) {
            return false;
        }
        Utilisateur other = (Utilisateur) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Utilisateur.class);
    }

    @Override
    public String toString() {
        return "Utilisateur{id=" + id + ", nom='" + nom + "', email='" + email
                + "', reservations=" + reservations.size() + "}";
    }
}
