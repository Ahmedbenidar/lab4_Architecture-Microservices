package services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dao.IDao;
import dao.MachineDao;
import dao.SalleDao;
import entities.Machine;
import entities.Salle;

/**
 * Regles metier liees aux machines : toute machine appartient obligatoirement a
 * une salle deja enregistree.
 */
public class MachineService implements IDao<Machine> {

    private final MachineDao machineDao = new MachineDao();
    private final SalleDao salleDao = new SalleDao();

    @Override
    public boolean create(Machine machine) {
        valider(machine);
        if (salleDao.getById(machine.getSalle().getId()) == null) {
            throw new IllegalArgumentException("La salle d'affectation n'existe pas en base");
        }
        return machineDao.create(machine);
    }

    @Override
    public boolean update(Machine machine) {
        if (machine == null || machine.getId() == 0) {
            throw new IllegalArgumentException("Seule une machine déjà persistée peut être mise à jour");
        }
        valider(machine);
        return machineDao.update(machine);
    }

    @Override
    public boolean delete(Machine machine) {
        if (machine == null || machine.getId() == 0) {
            throw new IllegalArgumentException("Seule une machine déjà persistée peut être supprimée");
        }
        return machineDao.delete(machine);
    }

    @Override
    public Machine getById(int id) {
        return machineDao.getById(id);
    }

    @Override
    public List<Machine> getAll() {
        return machineDao.getAll();
    }

    /** Recherche HQL insensible a la casse. */
    public List<Machine> parMarque(String marque) {
        if (marque == null || marque.isBlank()) {
            throw new IllegalArgumentException("La marque recherchée est obligatoire");
        }
        return machineDao.findByMarque(marque);
    }

    public List<Machine> parSalle(int salleId) {
        return machineDao.findBySalle(salleId);
    }

    public long nombreDansSalle(int salleId) {
        return machineDao.compterParSalle(salleId);
    }

    /** Repartition {code salle -> nombre de machines}. */
    public Map<String, Long> repartitionParSalle() {
        Map<String, Long> repartition = new LinkedHashMap<>();
        for (Object[] ligne : machineDao.repartitionParSalle()) {
            repartition.put((String) ligne[0], ((Number) ligne[1]).longValue());
        }
        return repartition;
    }

    /** Deplace une machine vers une autre salle existante. */
    public boolean deplacer(Machine machine, int salleIdCible) {
        if (machine == null || machine.getId() == 0) {
            throw new IllegalArgumentException("Seule une machine persistée peut être déplacée");
        }
        Salle cible = salleDao.getById(salleIdCible);
        if (cible == null) {
            throw new IllegalArgumentException("La salle cible " + salleIdCible + " n'existe pas");
        }
        machine.setSalle(cible);
        return machineDao.update(machine);
    }

    private void valider(Machine machine) {
        if (machine == null) {
            throw new IllegalArgumentException("La machine ne peut pas etre nulle");
        }
        if (machine.getRef() == null || machine.getRef().isBlank()) {
            throw new IllegalArgumentException("La référence de la machine est obligatoire");
        }
        if (machine.getMarque() == null || machine.getMarque().isBlank()) {
            throw new IllegalArgumentException("La marque de la machine est obligatoire");
        }
        if (machine.getSalle() == null || machine.getSalle().getId() == 0) {
            throw new IllegalArgumentException("Une machine doit être rattachée à une salle persistée");
        }
    }
}
