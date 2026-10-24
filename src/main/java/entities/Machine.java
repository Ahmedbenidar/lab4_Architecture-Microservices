package entities;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Machine affectee a une salle. Unites signees : la cle etrangere salle_id porte la relation.
 */
@Entity
@Table(name = "machines", uniqueConstraints = @UniqueConstraint(name = "uk_machine_ref", columnNames = "ref"))
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "ref", nullable = false, length = 30)
    private String ref;

    @Column(name = "marque", nullable = false, length = 60)
    private String marque;

    // EAGER volontaire : une machine n'a de sens qu'avec sa salle.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    public Machine() {
    }

    public Machine(String ref, String marque) {
        this.ref = ref;
        this.marque = marque;
    }

    public Machine(String ref, String marque, Salle salle) {
        this(ref, marque);
        this.salle = salle;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public Salle getSalle() {
        return salle;
    }

    public void setSalle(Salle salle) {
        this.salle = salle;
    }

    @Override
    public boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (!(autre instanceof Machine)) {
            return false;
        }
        Machine machine = (Machine) autre;
        return id != 0 && id == machine.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        String salleCode = salle == null ? "-" : salle.getCode();
        return String.format("Machine#%d %s (%s) -> salle %s", id, ref, marque, salleCode);
    }
}
