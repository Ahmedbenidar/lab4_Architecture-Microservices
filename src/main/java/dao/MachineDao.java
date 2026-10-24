package dao;

import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import entities.Machine;
import util.HibernateUtil;

public class MachineDao implements IDao<Machine> {

    @Override
    public boolean create(Machine machine) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(machine);
            transaction.commit();
            return true;
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la création de " + machine, e);
            return false;
        }
    }

    @Override
    public boolean update(Machine machine) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(machine);
            transaction.commit();
            return true;
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la mise à jour de " + machine, e);
            return false;
        }
    }

    @Override
    public boolean delete(Machine machine) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Machine aSupprimer = machine.getId() == 0 ? null : session.get(Machine.class, machine.getId());
            if (aSupprimer == null) {
                transaction.rollback();
                return false;
            }
            session.delete(aSupprimer);
            transaction.commit();
            return true;
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la suppression de " + machine, e);
            return false;
        }
    }

    @Override
    public Machine getById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Machine.class, id);
        }
    }

    @Override
    public List<Machine> getAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Machine order by ref", Machine.class).list();
        }
    }

    /** Requete HQL parametree : recherche insensible a la casse sur la marque. */
    public List<Machine> findByMarque(String marque) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "from Machine m where lower(m.marque) = lower(:marque) order by m.ref",
                    Machine.class)
                    .setParameter("marque", marque)
                    .list();
        } catch (Exception e) {
            DaoSupport.signalerEchec("la recherche par marque", e);
            return Collections.emptyList();
        }
    }

    public List<Machine> findBySalle(int salleId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "from Machine m where m.salle.id = :salleId order by m.ref",
                    Machine.class)
                    .setParameter("salleId", salleId)
                    .list();
        } catch (Exception e) {
            DaoSupport.signalerEchec("la recherche par salle", e);
            return Collections.emptyList();
        }
    }

    public long compterParSalle(int salleId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long total = session.createQuery(
                    "select count(m) from Machine m where m.salle.id = :salleId", Long.class)
                    .setParameter("salleId", salleId)
                    .uniqueResult();
            return total == null ? 0L : total;
        }
    }

    /** Agregation HQL : nombre de machines par code de salle. */
    @SuppressWarnings("unchecked")
    public List<Object[]> repartitionParSalle() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select s.code, count(m) from Salle s left join s.machines m "
                            + "group by s.code order by s.code asc").list();
        } catch (Exception e) {
            DaoSupport.signalerEchec("le calcul de la répartition par salle", e);
            return Collections.emptyList();
        }
    }

    public int supprimerTout() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            int supprimees = session.createQuery("delete from Machine").executeUpdate();
            transaction.commit();
            return supprimees;
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la purge des machines", e);
            return -1;
        }
    }

    /** Remet le compteur d'auto-increment a 1 pour obtenir des identifiants previsibles. */
    public void reinitialiserCompteur() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.createNativeQuery("ALTER TABLE machines AUTO_INCREMENT = 1").executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la remise à zéro du compteur de machines", e);
        }
    }
}
