# TP 3 — Gestion avancée des entités Salle, Réservation, Utilisateur et Équipement avec JPA et Hibernate

Projet Maven autonome illustrant les relations JPA avancées : `@ManyToOne`, `@OneToMany`
bidirectionnelle, `@ManyToMany` avec table de jointure, cascade, `orphanRemoval`,
méthodes utilitaires de synchronisation des deux côtés de la relation, cycle de vie
(`@PrePersist` / `@PreUpdate`) et requêtes JPQL / SQL natives.

Base de données **H2 en mémoire** — aucune installation externe n'est requise.

---

## Prérequis

| Outil | Version testée |
|-------|----------------|
| JDK   | 21 (le code cible Java 11) |
| Maven | 3.9.9 |

---

## Démarrage rapide

```bash
# Compiler et lancer les tests
mvn test

# Construire le jar
mvn package

# Exécuter la démonstration console
mvn exec:java
```

Le jar produit est `target/gestion-reservations-1.0.0.jar`.

---

## Structure du projet

```
tp3-jpa-reservations-equipements/
├── pom.xml
└── src
    ├── main
    │   ├── java/com/example
    │   │   ├── App.java                        # démonstration console (6 sections)
    │   │   ├── model
    │   │   │   ├── Salle.java                  # @ManyToMany vers Equipement, @OneToMany vers Reservation
    │   │   │   ├── Equipement.java             # côté inverse du @ManyToMany
    │   │   │   ├── Utilisateur.java            # @OneToMany avec cascade ALL + orphanRemoval
    │   │   │   └── Reservation.java            # entité de jointure porteuse des dates
    │   │   └── util
    │   │       └── JpaUtil.java                # EntityManagerFactory singleton
    │   └── resources/META-INF
    │       └── persistence.xml                 # unité de persistance "gestion-reservations-pu"
    └── test/java/com/example
        ├── TestSupport.java                    # remise à zéro de la base entre les tests
        ├── RelationsJpaTest.java               # 8 tests : cascade, jointure, orphanRemoval, JPQL
        └── ValidationReservationTest.java      # 3 tests : validation des périodes
```

---

## Dépendances (`pom.xml`)

| Dépendance | Version | Portée | Rôle |
|------------|---------|--------|------|
| `javax.persistence-api` | 2.2 | compile | API JPA (annotations) |
| `hibernate-core` | 5.6.15.Final | compile | Implémentation JPA |
| `com.h2database:h2` | 2.1.214 | runtime | Base en mémoire |
| `junit:junit` | 4.13.2 | test | Tests unitaires |
| `net.bytebuddy:byte-buddy` | 1.14.18 | compile | **Correctif JDK 21** (voir ci-dessous) |

> **Note sur les versions.** L'énoncé prescrit `hibernate-core 5.4.30.Final` et `h2 1.4.200`.
> Ces versions embarquent une version de Byte Buddy incapable de lire les fichiers de classe
> du JDK 21 (`Unsupported class file major version 65`). Hibernate est donc passé en
> `5.6.15.Final`, H2 en `2.1.214`, et `byte-buddy 1.14.18` est épinglé explicitement.
> Toutes les annotations, le nom de l'unité de persistance, le package `com.example.model`
> et l'URL JDBC `jdbc:h2:mem:test;DB_CLOSE_DELAY=-1` restent conformes à l'énoncé.

---

## Configuration JPA (`META-INF/persistence.xml`)

```xml
<persistence-unit name="gestion-reservations-pu" transaction-type="RESOURCE_LOCAL">
  <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
  <class>com.example.model.Salle</class>
  <class>com.example.model.Equipement</class>
  <class>com.example.model.Utilisateur</class>
  <class>com.example.model.Reservation</class>
  <properties>
    <property name="javax.persistence.jdbc.url" value="jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"/>
    <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
    <property name="hibernate.hbm2ddl.auto" value="update"/>
    <property name="hibernate.show_sql" value="true"/>
    <property name="hibernate.format_sql" value="true"/>
  </properties>
</persistence-unit>
```

---

## Points clés du TP

### 1. Le modèle de relations

| Relation | Type | Côté propriétaire | Cascade |
|----------|------|-------------------|---------|
| `Utilisateur` → `Reservation` | `@OneToMany` | `Reservation.utilisateur` | `ALL` + `orphanRemoval` |
| `Salle` → `Reservation` | `@OneToMany(mappedBy)` | `Reservation.salle` | **aucune** |
| `Reservation` → `Salle` | `@ManyToOne` | lui-même | `PERSIST`, `MERGE` |
| `Salle` ↔ `Equipement` | `@ManyToMany` | `Salle.equipements` | `PERSIST`, `MERGE` |

**Décision de conception :** le cycle de vie d'une `Reservation` est porté par **un seul**
parent, `Utilisateur`. `Salle.reservations` est un simple `mappedBy` sans cascade. Sinon deux
parents revendiqueraient le même `orphanRemoval` sur la même ligne, ce qui produit des
suppressions inattendues lorsqu'on retire la réservation d'une seule des deux collections.

### 2. Table de jointure `salle_equipement`

```java
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
@JoinTable(
    name = "salle_equipement",
    joinColumns = @JoinColumn(name = "salle_id"),
    inverseJoinColumns = @JoinColumn(name = "equipement_id"))
private Set<Equipement> equipements = new HashSet<>();
```

Côté `Equipement`, la collection est `@ManyToMany(mappedBy = "equipements")` **sans cascade** :
un équipement est une donnée de référence partagée entre plusieurs salles, il ne doit jamais
être supprimé parce qu'une salle l'a retiré.

### 3. Synchronisation des deux côtés de la relation

`Salle.addEquipement`, `Salle.removeEquipement`, `Utilisateur.addReservation`,
`Utilisateur.removeReservation` et `Reservation.setSalle` mettent à jour les deux collections.
C'est indispensable : JPA ne synchronise que le côté propriétaire en base, mais le modèle en
mémoire reste faux si le côté inverse n'est pas mis à jour. Sans cela,
`Salle.estDisponible()` ne verrait aucune réservation.

### 4. Validation du cycle de vie

`Reservation.validerPeriode()` refuse une période nulle ou dont la date de début n'est pas
strictement antérieure à la date de fin. Elle est appelée automatiquement par :

```java
@PrePersist
@PreUpdate
private void verifierPeriode() {
    validerPeriode();
}
```

Une période invalide fait donc échouer le `persist`/`flush` et non un contrôle applicatif
facultatif.

### 5. Requêtes

```java
// JPQL : salles équipées d'un équipement donné (DISTINCT obligatoire avec la jointure)
SELECT DISTINCT s FROM Salle s JOIN s.equipements e WHERE e.nom = :nom

// SQL natif : contenu brut de la table de jointure
SELECT salle_id, equipement_id FROM salle_equipement

// JPQL : comptages de vérification après cascade / orphanRemoval
SELECT COUNT(u) FROM Utilisateur u
```

### 6. Pièges rencontrés et corrigés

1. **Côté inverse non synchronisé.** Le constructeur `Reservation(début, fin, salle)`
   affectait le champ `salle` directement, sans passer par `setSalle()`. La salle n'était
   donc pas ajoutée à `Salle.reservations` et `estDisponible()` répondait `true` sur une
   période déjà réservée. Le constructeur appelle désormais `setSalle(salle)`.
2. **`orphanRemoval` avec deux parents.** Voir la décision de conception au point 1 : la
   cascade n'est déclarée que sur `Utilisateur`.
3. **Byte Buddy et JDK 21.** Voir la note sur les versions.
4. **Ordre des suppressions dans les tests.** La table de jointure doit être vidée avant
   `salle` et `equipement`, sinon les contraintes de clé étrangère bloquent la remise à zéro.

---

## Tests

```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

**`RelationsJpaTest` (8 tests)**

| Test | Vérifie |
|------|---------|
| `testPersistanceEnCascadeDepuisUtilisateur` | un seul `persist` crée utilisateur, réservation, salle et équipements |
| `testManyToManyEtTableDeJointure` | la table `salle_equipement` contient les bonnes lignes |
| `testEquipementPartageEntrePlusieursSalles` | un équipement partagé ne produit qu'une seule ligne `equipement` |
| `testOrphanRemovalSupprimeLaReservation` | retirer une réservation de l'utilisateur la supprime en base |
| `testCascadeRemoveSupprimeLesReservationsMaisPasLaSalle` | `em.remove(utilisateur)` supprime ses réservations, conserve salle et équipements |
| `testRetirerUnEquipementSupprimeLaLigneDeJointure` | `removeEquipement` nettoie la table de jointure sans supprimer l'équipement |
| `testDisponibiliteDeLaSalle` | périodes identiques, chevauchantes, antérieures et postérieures |
| `testRequeteJpqlSallesEquipees` | la requête `JOIN` ne retourne que les salles équipées |

**`ValidationReservationTest` (3 tests)**

| Test | Vérifie |
|------|---------|
| `testPeriodeValideEstAcceptee` | une période cohérente est persistée |
| `testPeriodeInvalideEstRefusee` | `@PrePersist` rejette la période et la transaction est annulée |
| `testValiderPeriodeDetecteLesDatesManquantes` | dates nulles refusées |

`TestSupport.resetDatabase()` vide les tables avant chaque test (table de jointure d'abord).

---

## Auteur

Mohamed Dani — [@danimohamed](https://github.com/danimohamed)
