package com.example.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Reservation d'une salle par un utilisateur sur une periode donnee.
 *
 * <p>Relations :
 * <ul>
 *   <li>{@code @ManyToOne} vers {@link Utilisateur} : cote proprietaire, colonne
 *       {@code utilisateur_id} non nulle. La cascade est portee par l'utilisateur.</li>
 *   <li>{@code @ManyToOne} vers {@link Salle} : cote proprietaire, colonne
 *       {@code salle_id}. On propage PERSIST/MERGE pour qu'enregistrer une
 *       reservation cree aussi la salle si elle n'existe pas encore.</li>
 * </ul>
 *
 * <p>La methode de rappel {@code @PrePersist}/{@code @PreUpdate} valide la coherence
 * des dates avant tout acces a la base.
 */
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "salle_id")
    private Salle salle;

    public Reservation() {
    }

    public Reservation(LocalDate dateDebut, LocalDate dateFin) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    public Reservation(LocalDate dateDebut, LocalDate dateFin, Salle salle) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        setSalle(salle);
    }

    /** Verifie que la periode est coherente. Leve une exception sinon. */
    public void validerPeriode() {
        if (dateDebut == null || dateFin == null) {
            throw new IllegalStateException("Les dates de debut et de fin sont obligatoires");
        }
        if (!dateDebut.isBefore(dateFin)) {
            throw new IllegalStateException(
                    "La date de debut (" + dateDebut + ") doit etre anterieure a la date de fin (" + dateFin + ")");
        }
    }

    @PrePersist
    @PreUpdate
    private void verifierPeriode() {
        validerPeriode();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Salle getSalle() {
        return salle;
    }

    /**
     * Affecte la salle en maintenant les deux cotes de la relation :
     * {@code Salle.reservations} est le cote inverse ({@code mappedBy}), il ne doit
     * jamais etre oublie, sinon le modele en memoire devient incoherent (par exemple
     * {@link Salle#estDisponible} ne verrait aucune reservation).
     */
    public void setSalle(Salle salle) {
        if (this.salle != null) {
            this.salle.getReservations().remove(this);
        }
        this.salle = salle;
        if (salle != null) {
            salle.getReservations().add(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reservation)) {
            return false;
        }
        Reservation other = (Reservation) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Reservation.class);
    }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", du " + dateDebut + " au " + dateFin
                + ", utilisateur=" + (utilisateur != null ? utilisateur.getNom() : null)
                + ", salle=" + (salle != null ? salle.getNom() : null) + "}";
    }
}
