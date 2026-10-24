package entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Salle de travaux pratiques. Une salle regroupe plusieurs machines.
 */
@Entity
@Table(name = "salles", uniqueConstraints = @UniqueConstraint(name = "uk_salle_code", columnNames = "code"))
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "libelle", nullable = false, length = 120)
    private String libelle;

    // Cote inverse de l'association : la colonne de jointure vit dans "machines".
    @OneToMany(mappedBy = "salle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Machine> machines = new ArrayList<>();

    public Salle() {
    }

    public Salle(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    /** Vue non modifiable pour eviter de casser le suivi des collections par Hibernate. */
    public List<Machine> getMachines() {
        return Collections.unmodifiableList(machines);
    }

    public void ajouterMachine(Machine machine) {
        machines.add(machine);
        machine.setSalle(this);
    }

    public void retirerMachine(Machine machine) {
        machines.remove(machine);
        machine.setSalle(null);
    }

    public int getNombreMachines() {
        return machines.size();
    }

    @Override
    public boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (!(autre instanceof Salle)) {
            return false;
        }
        Salle salle = (Salle) autre;
        return id != 0 && id == salle.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Salle#%d %s \"%s\"", id, code, libelle);
    }
}
