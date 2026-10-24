package services;

import java.util.List;

import dao.IDao;
import dao.MachineDao;
import dao.SalleDao;
import entities.Salle;

/**
 * Regles metier liees aux salles. La couche service valide les donnees avant de
 * deleguer la persistance au DAO : les deux niveaux se protegent mutuellement.
 */
public class SalleService implements IDao<Salle> {

    private final SalleDao salleDao = new SalleDao();
    private final MachineDao machineDao = new MachineDao();

    @Override
    public boolean create(Salle salle) {
        valider(salle);
        if (salleDao.findByCode(salle.getCode()) != null) {
            throw new IllegalArgumentException("Le code de salle " + salle.getCode() + " est déjà utilisé");
        }
        return salleDao.create(salle);
    }

    @Override
    public boolean update(Salle salle) {
        if (salle == null || salle.getId() == 0) {
            throw new IllegalArgumentException("Seule une salle déjà persistée peut être mise à jour");
        }
        valider(salle);
        return salleDao.update(salle);
    }

    @Override
    public boolean delete(Salle salle) {
        if (salle == null || salle.getId() == 0) {
            throw new IllegalArgumentException("Seule une salle déjà persistée peut être supprimée");
        }
        return salleDao.delete(salle);
    }

    @Override
    public Salle getById(int id) {
        return salleDao.getById(id);
    }

    @Override
    public List<Salle> getAll() {
        return salleDao.getAll();
    }

    public Salle parCode(String code) {
        return salleDao.findByCode(code);
    }

    /** Inventaire complet en une requete : salles + machines associees. */
    public List<Salle> inventaireAvecMachines() {
        return salleDao.getAllAvecMachines();
    }

    public Salle salleLaPlusEquipee() {
        return salleDao.salleLaPlusEquipee();
    }

    public long nombreDeSalles() {
        return salleDao.getAll().size();
    }

    public long nombreDeMachines() {
        return machineDao.getAll().size();
    }

    public int purgerTout() {
        machineDao.supprimerTout();
        int supprimees = salleDao.supprimerTout();
        machineDao.reinitialiserCompteur();
        salleDao.reinitialiserCompteur();
        return supprimees;
    }

    private void valider(Salle salle) {
        if (salle == null) {
            throw new IllegalArgumentException("La salle ne peut pas être nulle");
        }
        if (salle.getCode() == null || salle.getCode().isBlank()) {
            throw new IllegalArgumentException("Le code de la salle est obligatoire");
        }
        if (salle.getLibelle() == null || salle.getLibelle().isBlank()) {
            throw new IllegalArgumentException("Le libellé de la salle est obligatoire");
        }
    }
}
