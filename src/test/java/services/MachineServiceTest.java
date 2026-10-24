package services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import entities.Machine;
import entities.Salle;

class MachineServiceTest {

    private final SalleService salleService = new SalleService();
    private final MachineService machineService = new MachineService();

    private Salle atelier;
    private Salle laboratoire;

    @BeforeEach
    void preparerLeContexte() {
        salleService.purgerTout();
        atelier = new Salle("A1", "Atelier");
        laboratoire = new Salle("L1", "Laboratoire");
        salleService.create(atelier);
        salleService.create(laboratoire);
    }

    @Test
    @DisplayName("create rattache la machine a sa salle")
    void createRattacheLaMachine() {
        Machine machine = new Machine("M-01", "Dell", atelier);

        assertTrue(machineService.create(machine));
        assertTrue(machine.getId() > 0);
        assertEquals("Atelier", machineService.getById(machine.getId()).getSalle().getLibelle());
        assertEquals(1, machineService.nombreDansSalle(atelier.getId()));
    }

    @Test
    @DisplayName("une machine sans salle est refusee")
    void machineSansSalleRefusee() {
        Machine orpheline = new Machine("M-02", "HP");

        assertThrows(IllegalArgumentException.class, () -> machineService.create(orpheline));
        assertTrue(machineService.getAll().isEmpty());
    }

    @Test
    @DisplayName("une reference de machine en double est refusee par la base")
    void referenceEnDoubleRefusee() {
        machineService.create(new Machine("M-03", "Lenovo", atelier));

        Machine doublon = new Machine("M-03", "Asus", laboratoire);
        assertFalse(machineService.create(doublon));
        assertEquals(1, machineService.getAll().size());
    }

    @Test
    @DisplayName("findByMarque ignore la casse et trie par reference")
    void findByMarqueInsensibleALaCasse() {
        machineService.create(new Machine("M-10", "Dell", atelier));
        machineService.create(new Machine("M-11", "DELL", laboratoire));
        machineService.create(new Machine("M-12", "HP", atelier));

        List<Machine> dell = machineService.parMarque("dell");

        assertEquals(2, dell.size());
        assertEquals("M-10", dell.get(0).getRef());
        assertEquals("M-11", dell.get(1).getRef());
    }

    @Test
    @DisplayName("repartitionParSalle agrege les compteurs par code de salle")
    void repartitionParSalle() {
        machineService.create(new Machine("M-20", "Dell", atelier));
        machineService.create(new Machine("M-21", "HP", atelier));
        machineService.create(new Machine("M-22", "Lenovo", laboratoire));

        Map<String, Long> repartition = machineService.repartitionParSalle();

        assertEquals(Long.valueOf(2L), repartition.get("A1"));
        assertEquals(Long.valueOf(1L), repartition.get("L1"));
    }

    @Test
    @DisplayName("deplacer change la salle d'affectation")
    void deplacerChangeLaSalle() {
        Machine machine = new Machine("M-30", "Dell", atelier);
        machineService.create(machine);

        assertTrue(machineService.deplacer(machine, laboratoire.getId()));

        assertEquals(0, machineService.nombreDansSalle(atelier.getId()));
        assertEquals(1, machineService.nombreDansSalle(laboratoire.getId()));
    }

    @Test
    @DisplayName("supprimer une salle supprime ses machines en cascade")
    void suppressionEnCascade() {
        machineService.create(new Machine("M-40", "Dell", atelier));
        machineService.create(new Machine("M-41", "HP", atelier));
        machineService.create(new Machine("M-42", "HP", laboratoire));

        assertTrue(salleService.delete(atelier));

        assertEquals(1, machineService.getAll().size());
        assertEquals(0, machineService.nombreDansSalle(atelier.getId()));
    }
}
