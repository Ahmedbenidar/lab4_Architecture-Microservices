package dao;

import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import entities.Salle;
import util.HibernateUtil;

public class SalleDao implements IDao<Salle> {

    @Override
    public boolean create(Salle salle) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(salle);
            transaction.commit();
            return true;
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la création de " + salle, e);
            return false;
        }
    }

    @Override
    public boolean update(Salle salle) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(salle);
            transaction.commit();
            return true;
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la mise à jour de " + salle, e);
            return false;
        }
    }

    @Override
    public boolean delete(Salle salle) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            // Rechargement indispensable : sur une instance detachee la collection "machines"
            // peut etre vide, et la cascade ne supprimerait alors aucune machine (erreur de
            // cle etrangere a la suppression de la salle).
            Salle aSupprimer = salle.getId() == 0 ? null : session.get(Salle.class, salle.getId());
            if (aSupprimer == null) {
                transaction.rollback();
                return false;
            }
            session.delete(aSupprimer);
            transaction.commit();
            return true;
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la suppression de " + salle, e);
            return false;
        }
    }

    @Override
    public Salle getById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Salle.class, id);
        }
    }

    @Override
    public List<Salle> getAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Salle order by code", Salle.class).list();
        }
    }

    public Salle findByCode(String code) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Salle where code = :code", Salle.class)
                    .setParameter("code", code)
                    .uniqueResult();
        }
    }

    /**
     * Charge les salles et leurs machines en une seule requete (join fetch) :
     * evite le probleme N+1 et les LazyInitializationException hors session.
     */
    public List<Salle> getAllAvecMachines() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select distinct s from Salle s left join fetch s.machines order by s.code",
                    Salle.class).list();
        } catch (Exception e) {
            DaoSupport.signalerEchec("la lecture des salles avec leurs machines", e);
            return Collections.emptyList();
        }
    }

    /** Salle qui concentre le plus de machines (aggregation + tri cote base). */
    public Salle salleLaPlusEquipee() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select s from Salle s left join s.machines m group by s.id, s.code, s.libelle "
                            + "order by count(m) desc, s.code asc",
                    Salle.class)
                    .setMaxResults(1)
                    .uniqueResult();
        } catch (Exception e) {
            DaoSupport.signalerEchec("la recherche de la salle la plus équipée", e);
            return null;
        }
    }

    public int supprimerTout() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            int supprimees = session.createQuery("delete from Salle").executeUpdate();
            transaction.commit();
            return supprimees;
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la purge des salles", e);
            return -1;
        }
    }

    /** Remet le compteur d'auto-increment a 1 pour obtenir des identifiants previsibles. */
    public void reinitialiserCompteur() {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.createNativeQuery("ALTER TABLE salles AUTO_INCREMENT = 1").executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            DaoSupport.rollback(transaction);
            DaoSupport.signalerEchec("la remise à zéro du compteur de salles", e);
        }
    }
}
