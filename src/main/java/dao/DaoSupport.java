package dao;

import org.hibernate.Transaction;

/**
 * Mutualise la gestion des transactions et la journalisation des echecs
 * pour toutes les implementations de {@link IDao}.
 */
final class DaoSupport {

    private DaoSupport() {
    }

    static void rollback(Transaction transaction) {
        if (transaction == null || !transaction.isActive()) {
            return;
        }
        try {
            transaction.rollback();
        } catch (RuntimeException e) {
            // Cas limite : le fournisseur de connexions a deja rendu la connexion au pool,
            // Hibernate a alors deja annule la transaction de son cote.
            System.err.println("[DAO] Rollback délaissé (" + e.getClass().getSimpleName()
                    + ") : la transaction a déjà été annulée par Hibernate");
        }
    }

    static void signalerEchec(String operation, Exception e) {
        System.err.println("[DAO] Échec pendant " + operation + " : " + causeRacine(e));
    }

    private static String causeRacine(Throwable e) {
        Throwable courant = e;
        while (courant.getCause() != null && courant.getCause() != courant) {
            courant = courant.getCause();
        }
        String message = courant.getMessage();
        if (message == null) {
            return courant.getClass().getSimpleName();
        }
        int retourLigne = message.indexOf('\n');
        return retourLigne > 0 ? message.substring(0, retourLigne) : message;
    }
}
