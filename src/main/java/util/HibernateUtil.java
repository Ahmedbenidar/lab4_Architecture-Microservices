package util;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Point d'acces unique a la SessionFactory : construction paresseuse, une seule
 * instance pour toute la JVM, et fermeture explicite en fin de vie de l'application.
 */
public final class HibernateUtil {

    private static final String CLE_URL = "hibernate.connection.url";
    private static final String CLE_UTILISATEUR = "hibernate.connection.username";
    private static final String CLE_MOT_DE_PASSE = "hibernate.connection.password";

    static {
        // Hibernate journalise en INFO via java.util.logging : trop bavard pour une
        // sortie de demonstration. Niveau surchargeable avec -DHIBERNATE_LOG=INFO.
        Level niveau = Level.WARNING;
        String demande = System.getProperty("HIBERNATE_LOG");
        if (demande == null || demande.isBlank()) {
            demande = System.getenv("HIBERNATE_LOG");
        }
        if (demande != null && !demande.isBlank()) {
            try {
                niveau = Level.parse(demande.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                System.err.println("Niveau de journal inconnu (" + demande + "), WARNING utilisé");
            }
        }
        Logger.getLogger("org.hibernate").setLevel(niveau);
        Logger.getLogger("com.mysql").setLevel(niveau);
    }

    private static final SessionFactory SESSION_FACTORY = construireSessionFactory();

    private HibernateUtil() {
    }

    private static SessionFactory construireSessionFactory() {
        try {
            Configuration configuration = new Configuration().configure();
            appliquerSurcharges(configuration);
            return configuration.buildSessionFactory();
        } catch (Throwable e) {
            System.err.println("Impossible d'initialiser Hibernate : " + e.getMessage());
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Permet de changer de base sans toucher au fichier de configuration :
     * priorite aux variables d'environnement, puis aux proprietes systeme.
     */
    private static void appliquerSurcharges(Configuration configuration) {
        surcharger(configuration, CLE_URL, "DB_URL");
        surcharger(configuration, CLE_UTILISATEUR, "DB_USER");
        surcharger(configuration, CLE_MOT_DE_PASSE, "DB_PASSWORD");
    }

    private static void surcharger(Configuration configuration, String propriete, String variable) {
        String valeur = System.getenv(variable);
        if (valeur == null || valeur.isBlank()) {
            valeur = System.getProperty(variable);
        }
        if (valeur != null && !valeur.isBlank()) {
            configuration.setProperty(propriete, valeur);
        }
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static String urlCourante() {
        Object url = SESSION_FACTORY.getProperties().get(CLE_URL);
        return url == null ? "(non definie)" : url.toString();
    }

    public static String versionHibernate() {
        return org.hibernate.Version.getVersionString();
    }

    public static void fermer() {
        if (!SESSION_FACTORY.isClosed()) {
            SESSION_FACTORY.close();
        }
    }
}
