package dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import entities.Machine;
import entities.Salle;
import services.MachineService;
import services.SalleService;

/**
 * Verifie le comportement de la couche DAO face aux contraintes relationnelles
 * et le bon chargement des associations.
 */
class DaoPersistanceTest {

    private final SalleDao salleDao = new SalleDao();
    private final MachineDao machineDao = new MachineDao();
    private final SalleService salleService = new SalleService();
    private final MachineService machineService = new MachineService();

    @BeforeEach
    void viderLaBase() {
        salleService.purgerTout();
    }

    @Test
    @DisplayName("une violation de contrainte annule l'insertion (rollback)")
    void violationDeContrainteAnnuleLInsertion() {
        assertTrue(salleDao.create(new Salle("D1", "Salle valide")));

        assertFalse(salleDao.create(new Salle("D1", "Salle en doublon")), "L'insertion doit echouer");

        List<Salle> salles = salleDao.getAll();
        assertEquals(1, salles.size());
        assertEquals("Salle valide", salles.get(0).getLibelle());
    }

    @Test
    @DisplayName("la cle etrangere salle_id empeche une machine orpheline")
    void cleEtrangereObligatoire() {
        Salle inexistante = new Salle("D2", "Jamais persistee");
        inexistante.setId(9999);

        assertFalse(machineDao.create(new Machine("M-D2", "Dell", inexistante)));
        assertTrue(machineDao.getAll().isEmpty());
    }

    @Test
    @DisplayName("getById renvoie null pour un identifiant inconnu")
    void getByIdInconnu() {
        assertNull(salleDao.getById(424242));
        assertNull(machineDao.getById(424242));
    }

    @Test
    @DisplayName("getAllAvecMachines charge les collections hors session")
    void jointureFetchChargeLesMachines() {
        Salle salle = new Salle("D3", "Salle chargee");
        salleService.create(salle);
        machineService.create(new Machine("M-D3-A", "Dell", salle));
        machineService.create(new Machine("M-D3-B", "HP", salle));

        List<Salle> inventaire = salleDao.getAllAvecMachines();

        assertEquals(1, inventaire.size());
        // La collection est initialisee par le join fetch : aucun acces base ici.
        assertEquals(2, inventaire.get(0).getMachines().size());
    }

    @Test
    @DisplayName("salleLaPlusEquipee renvoie la salle la plus chargee")
    void salleLaPlusEquipee() {
        Salle petite = new Salle("D4", "Petite salle");
        Salle grande = new Salle("D5", "Grande salle");
        salleService.create(petite);
        salleService.create(grande);
        machineService.create(new Machine("M-D4", "HP", petite));
        machineService.create(new Machine("M-D5-A", "Dell", grande));
        machineService.create(new Machine("M-D5-B", "Dell", grande));

        Salle resultat = salleDao.salleLaPlusEquipee();

        assertNotNull(resultat);
        assertEquals("D5", resultat.getCode());
    }
}
