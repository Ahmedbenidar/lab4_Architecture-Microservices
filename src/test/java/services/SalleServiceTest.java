package services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import entities.Salle;

class SalleServiceTest {

    private final SalleService salleService = new SalleService();

    @BeforeEach
    void viderLaTable() {
        salleService.purgerTout();
    }

    @Test
    @DisplayName("create persiste la salle et fait generer la cle primaire")
    void createGenereLaCle() {
        Salle salle = new Salle("T1", "Salle de test");

        assertTrue(salleService.create(salle));
        assertTrue(salle.getId() > 0, "L'identifiant doit etre genere par la base");
        assertEquals("T1", salleService.getById(salle.getId()).getCode());
    }

    @Test
    @DisplayName("getAll renvoie les salles triees par code")
    void getAllTrieParCode() {
        salleService.create(new Salle("B2", "Deuxieme"));
        salleService.create(new Salle("A1", "Premiere"));

        List<Salle> salles = salleService.getAll();

        assertEquals(2, salles.size());
        assertEquals("A1", salles.get(0).getCode());
    }

    @Test
    @DisplayName("update modifie le libelle en base")
    void updateModifieLeLibelle() {
        Salle salle = new Salle("T2", "Ancien libelle");
        salleService.create(salle);

        salle.setLibelle("Nouveau libelle");
        assertTrue(salleService.update(salle));
        assertEquals("Nouveau libelle", salleService.getById(salle.getId()).getLibelle());
    }

    @Test
    @DisplayName("delete retire la ligne de la table")
    void deleteRetireLaLigne() {
        Salle salle = new Salle("T3", "A supprimer");
        salleService.create(salle);

        assertTrue(salleService.delete(salle));
        assertNull(salleService.getById(salle.getId()));
        assertEquals(0, salleService.nombreDeSalles());
    }

    @Test
    @DisplayName("un code de salle en double est refuse par la couche service")
    void codeEnDoubleRefuse() {
        salleService.create(new Salle("T4", "Originale"));

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> salleService.create(new Salle("T4", "Doublon")));

        assertTrue(erreur.getMessage().contains("T4"));
        assertEquals(1, salleService.nombreDeSalles());
    }

    @Test
    @DisplayName("un libelle vide est refuse")
    void libelleObligatoire() {
        Salle incomplete = new Salle("T5", "   ");

        assertThrows(IllegalArgumentException.class, () -> salleService.create(incomplete));
        assertEquals(0, salleService.nombreDeSalles());
    }

    @Test
    @DisplayName("parCode retrouve une salle existante et null sinon")
    void parCode() {
        salleService.create(new Salle("T6", "Recherchee"));

        assertNotNull(salleService.parCode("T6"));
        assertNull(salleService.parCode("INCONNU"));
        assertFalse(salleService.getAll().isEmpty());
    }
}
