package com.example.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Equipement partageable entre plusieurs salles (projecteur, tableau blanc...).
 *
 * <p>Cote inverse de la relation {@code @ManyToMany} : la table de jointure est
 * declaree dans {@link Salle}, donc ici on utilise {@code mappedBy = "equipements"}.
 * Aucune cascade n'est declaree de ce cote : c'est toujours la salle qui pilote
 * la persistance de ses equipements.
 */
@Entity
@Table(name = "equipement")
public class Equipement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @ManyToMany(mappedBy = "equipements")
    private Set<Salle> salles = new HashSet<>();

    public Equipement() {
    }

    public Equipement(String nom) {
        this.nom = nom;
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

    public Set<Salle> getSalles() {
        return salles;
    }

    public void setSalles(Set<Salle> salles) {
        this.salles = salles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Equipement)) {
            return false;
        }
        Equipement other = (Equipement) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Equipement.class);
    }

    @Override
    public String toString() {
        return "Equipement{id=" + id + ", nom='" + nom + "'}";
    }
}
