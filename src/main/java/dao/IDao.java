package dao;

import java.util.List;

/**
 * Contrat de persistance generique : tout objet metier persistable doit pouvoir
 * etre cree, modifie, supprime et relu sans que les couches superieures
 * connaissent Hibernate.
 *
 * @param <T> type de l'entite geree
 */
public interface IDao<T> {

    boolean create(T objet);

    boolean update(T objet);

    boolean delete(T objet);

    T getById(int id);

    List<T> getAll();
}
