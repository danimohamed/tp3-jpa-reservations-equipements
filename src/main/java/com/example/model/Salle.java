package com.example.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Salle de reunion / cours.
 *
 * <p>Relations :
 * <ul>
 *   <li>{@code @ManyToMany} vers {@link Equipement} : relation porteuse, donc c'est
 *       ici que l'on declare la table de jointure {@code salle_equipement}.</li>
 *   <li>{@code @OneToMany} vers {@link Reservation} : cote inverse (mappedBy), la
 *       colonne de jointure vit dans {@code reservation.salle_id}. Aucune cascade ici :
 *       le proprietaire du cycle de vie d'une reservation est l'{@link Utilisateur},
 *       ce qui evite deux parents "orphanRemoval" pour la meme entite.</li>
 * </ul>
 */
@Entity
@Table(name = "salle")
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false)
    private int capacite;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "salle_equipement",
            joinColumns = @JoinColumn(name = "salle_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id")
    )
    private Set<Equipement> equipements = new HashSet<>();

    @OneToMany(mappedBy = "salle")
    private Set<Reservation> reservations = new HashSet<>();

    public Salle() {
    }

    public Salle(String nom, int capacite) {
        this.nom = nom;
        this.capacite = capacite;
    }

    /** Ajoute un equipement en synchronisant les deux cotes de la relation. */
    public void addEquipement(Equipement equipement) {
        this.equipements.add(equipement);
        equipement.getSalles().add(this);
    }

    /** Retire un equipement en synchronisant les deux cotes de la relation. */
    public void removeEquipement(Equipement equipement) {
        this.equipements.remove(equipement);
        equipement.getSalles().remove(this);
    }

    /**
     * Indique si la salle est libre sur un intervalle donne.
     * Deux intervalles se chevauchent si {@code debut < autreFin} et {@code autreDebut < fin}.
     */
    public boolean estDisponible(java.time.LocalDate debut, java.time.LocalDate fin) {
        if (debut == null || fin == null || !debut.isBefore(fin)) {
            throw new IllegalArgumentException("Intervalle invalide");
        }
        for (Reservation r : reservations) {
            if (r.getDateDebut().isBefore(fin) && debut.isBefore(r.getDateFin())) {
                return false;
            }
        }
        return true;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public Set<Equipement> getEquipements() {
        return equipements;
    }

    public void setEquipements(Set<Equipement> equipements) {
        this.equipements = equipements;
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
        if (!(o instanceof Salle)) {
            return false;
        }
        Salle other = (Salle) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Salle.class);
    }

    @Override
    public String toString() {
        return "Salle{id=" + id + ", nom='" + nom + "', capacite=" + capacite
                + ", equipements=" + equipements.size() + "}";
    }
}
