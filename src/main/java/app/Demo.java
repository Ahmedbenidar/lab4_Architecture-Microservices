package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dao.MachineDao;
import dao.SalleDao;
import entities.Machine;
import entities.Salle;
import services.MachineService;
import services.SalleService;
import util.HibernateUtil;

/**
 * Scenario de demonstration du TP4 : verifie de bout en bout la couche de
 * persistance Hibernate contre une base MySQL/MariaDB reelle.
 *
 * Chaque verification affiche le resultat attendu, la valeur reellement obtenue,
 * puis un bilan final. Le code de sortie vaut 1 si au moins une verification echoue,
 * ce qui rend le scenario utilisable en integration continue.
 */
public final class Demo {

    private static final String SEPARATEUR =
            "==========================================================================================";

    private static int verifications;
    private static int conformes;
    private static final List<String> ecarts = new ArrayList<>();

    private Demo() {
    }

    public static void main(String[] args) {
        SalleService salleService = new SalleService();
        MachineService machineService = new MachineService();
        int codeSortie = 0;

        entete();
        try {
            purge(salleService, machineService);

            JeuDeDonnees jeu = creation(salleService, machineService);
            lectureEtMiseAJour(salleService, machineService, jeu);
            requetesHql(salleService, machineService);
            contraintesEtRollback(salleService, machineService);
            suppressionEnCascade(salleService, machineService, jeu);
        } catch (RuntimeException e) {
            System.out.println();
            System.out.println("  [!] Scénario interrompu : " + e);
            ecarts.add("interruption : " + e.getMessage());
        } finally {
            codeSortie = bilan();
            HibernateUtil.fermer();
        }
        System.exit(codeSortie);
    }

    // ------------------------------------------------------------------ etapes

    private static void purge(SalleService salleService, MachineService machineService) {
        titre("0", "Purge des données précédentes");
        salleService.purgerTout();
        verifier("Table salles vidée", salleService.nombreDeSalles() == 0,
                salleService.nombreDeSalles() + " salle(s)");
        verifier("Table machines vidée", machineService.getAll().isEmpty(),
                machineService.getAll().size() + " machine(s)");
    }

    private static JeuDeDonnees creation(SalleService salleService, MachineService machineService) {
        titre("1", "Création des salles puis des machines (clé étrangère salle_id)");

        JeuDeDonnees jeu = new JeuDeDonnees();
        jeu.s1 = new Salle("S1", "Salle Informatique");
        jeu.s2 = new Salle("S2", "Salle Réseau");
        jeu.s3 = new Salle("S3", "Salle Électronique");

        for (Salle salle : new Salle[] {jeu.s1, jeu.s2, jeu.s3}) {
            boolean creee = salleService.create(salle);
            System.out.printf("  -> %s créée=%s%n", salle, creee);
        }

        jeu.dell1 = new Machine("PC-001", "Dell", jeu.s1);
        jeu.hp1 = new Machine("PC-002", "HP", jeu.s1);
        jeu.dell2 = new Machine("PC-003", "Dell", jeu.s2);
        jeu.lenovo1 = new Machine("PC-004", "Lenovo", jeu.s3);
        for (Machine machine : new Machine[] {jeu.dell1, jeu.hp1, jeu.dell2, jeu.lenovo1}) {
            machineService.create(machine);
            System.out.printf("  -> %s%n", machine);
        }

        verifier("Identifiants générés par la base", jeu.s1.getId() > 0 && jeu.lenovo1.getId() > 0,
                "S1=" + jeu.s1.getId() + ", PC-004=" + jeu.lenovo1.getId());
        verifier("3 salles persistées", salleService.nombreDeSalles() == 3,
                salleService.nombreDeSalles() + " salle(s)");
        verifier("4 machines persistées", machineService.getAll().size() == 4,
                machineService.getAll().size() + " machine(s)");
        verifier("Machine reliée à sa salle en base", machineService.nombreDansSalle(jeu.s1.getId()) == 2,
                "S1 contient " + machineService.nombreDansSalle(jeu.s1.getId()) + " machine(s)");
        return jeu;
    }

    private static void lectureEtMiseAJour(SalleService salleService, MachineService machineService,
                                           JeuDeDonnees jeu) {
        titre("2", "Lecture par identifiant, mise à jour et changement d'affectation");

        Salle relue = salleService.getById(jeu.s1.getId());
        System.out.printf("  getById(%d)   -> %s%n", jeu.s1.getId(), relue);

        relue.setLibelle("Salle Informatique - rénovée 2025");
        boolean modifiee = salleService.update(relue);
        Salle apresMaj = salleService.getById(jeu.s1.getId());
        System.out.printf("  update        -> %s (libellé relu : \"%s\")%n", modifiee, apresMaj.getLibelle());

        boolean deplacee = machineService.deplacer(jeu.hp1, jeu.s3.getId());
        System.out.printf("  deplacer PC-002 vers S3 -> %s%n", deplacee);

        verifier("Lecture par clé primaire", "S1".equals(relue.getCode()),
                "code lu = " + relue.getCode());
        verifier("Mise à jour du libellé", "Salle Informatique - rénovée 2025".equals(apresMaj.getLibelle()),
                "\"" + apresMaj.getLibelle() + "\"");
        verifier("Changement de salle pris en compte",
                machineService.nombreDansSalle(jeu.s1.getId()) == 1
                        && machineService.nombreDansSalle(jeu.s3.getId()) == 2,
                "S1=" + machineService.nombreDansSalle(jeu.s1.getId())
                        + ", S3=" + machineService.nombreDansSalle(jeu.s3.getId()));
    }

    private static void requetesHql(SalleService salleService, MachineService machineService) {
        titre("3", "Requêtes HQL : recherche paramétrée, agrégations, jointure");

        List<Machine> dell = machineService.parMarque("dell");
        StringBuilder references = new StringBuilder();
        for (Machine machine : dell) {
            if (references.length() > 0) {
                references.append(", ");
            }
            references.append(machine.getRef());
        }
        System.out.printf("  findByMarque(\"dell\") -> %s%n", references);

        Map<String, Long> repartition = machineService.repartitionParSalle();
        StringBuilder details = new StringBuilder();
        for (Map.Entry<String, Long> entree : repartition.entrySet()) {
            if (details.length() > 0) {
                details.append(" | ");
            }
            details.append(entree.getKey()).append('=').append(entree.getValue());
        }
        System.out.printf("  repartition par salle -> %s%n", details);

        List<Salle> inventaire = salleService.inventaireAvecMachines();
        for (Salle salle : inventaire) {
            System.out.printf("  %-3s %-40s %s%n", salle.getCode(),
                    "\"" + salle.getLibelle() + "\"",
                    salle.getMachines().size() + " machine(s) chargée(s) par join fetch");
        }

        Salle plusEquipee = salleService.salleLaPlusEquipee();
        long chargePlusEquipee = plusEquipee == null ? 0 : machineService.nombreDansSalle(plusEquipee.getId());
        System.out.printf("  salleLaPlusEquipee() -> %s (%d machines)%n",
                plusEquipee == null ? "aucune" : plusEquipee.getCode(), chargePlusEquipee);

        verifier("Recherche HQL insensible à la casse", dell.size() == 2,
                dell.size() + " machine(s) de marque Dell");
        verifier("Agrégation group by", "2".equals(String.valueOf(repartition.get("S3"))),
                "S3 = " + repartition.get("S3") + " machine(s)");
        verifier("Jointure chargée en une requête", inventaire.size() == 3,
                inventaire.size() + " salle(s) avec collection initialisée");
        verifier("Tri par agrégat", plusEquipee != null && "S3".equals(plusEquipee.getCode()),
                plusEquipee == null ? "aucune salle" : plusEquipee.getCode());
    }

    private static void contraintesEtRollback(SalleService salleService, MachineService machineService) {
        titre("4", "Validation métier et rollback sur violation de contrainte");

        try {
            salleService.create(new Salle("S1", "Doublon refuse par le service"));
            verifier("Code de salle unique (service)", false, "aucune exception levee");
        } catch (IllegalArgumentException e) {
            System.out.printf("  service.create(\"S1\") -> rejet : %s%n", e.getMessage());
            verifier("Code de salle unique (service)", true, e.getMessage());
        }

        SalleDao salleDao = new SalleDao();
        boolean inseree = salleDao.create(new Salle("S1", "Doublon tenté en direct"));
        verifier("Contrainte SQL uk_salle_code + rollback", !inseree,
                "insertion=" + inseree + ", salles=" + salleService.nombreDeSalles());

        try {
            machineService.create(new Machine("PC-999", "Asus"));
            verifier("Machine toujours rattachée à une salle", false, "aucune exception levee");
        } catch (IllegalArgumentException e) {
            System.out.printf("  machine sans salle -> rejet : %s%n", e.getMessage());
            verifier("Machine toujours rattachée à une salle", true, e.getMessage());
        }

        MachineDao machineDao = new MachineDao();
        Salle salleFantome = new Salle("SX", "Salle inexistante");
        salleFantome.setId(9999);
        boolean inseree2 = machineDao.create(new Machine("PC-999", "Asus", salleFantome));
        verifier("Clé étrangère salle_id appliquée", !inseree2,
                "insertion=" + inseree2 + ", machines=" + machineService.getAll().size());
    }

    private static void suppressionEnCascade(SalleService salleService, MachineService machineService,
                                             JeuDeDonnees jeu) {
        titre("5", "Suppression d'une salle et cascade sur ses machines");

        long avant = machineService.getAll().size();
        Salle aSupprimer = salleService.getById(jeu.s3.getId());
        boolean supprimee = salleService.delete(aSupprimer);
        long apres = machineService.getAll().size();
        System.out.printf("  delete(S3) -> %s ; machines avant=%d, après=%d%n", supprimee, avant, apres);

        verifier("Salle supprimée", salleService.parCode("S3") == null, "parCode(\"S3\") = null");
        verifier("Machines de S3 supprimées en cascade", apres == avant - 2,
                avant + " -> " + apres + " machine(s)");
        verifier("Machines restantes rattachées aux bonnes salles",
                machineService.nombreDansSalle(jeu.s1.getId()) == 1
                        && machineService.nombreDansSalle(jeu.s2.getId()) == 1,
                "S1=1, S2=1");
        verifier("Nombre final de salles", salleService.nombreDeSalles() == 2,
                salleService.nombreDeSalles() + " salle(s)");
    }

    // ------------------------------------------------------------- restitution

    private static void entete() {
        // Initialisation avant l'affichage pour que les journaux Hibernate
        // n'intersectent pas le bandeau de presentation.
        HibernateUtil.getSessionFactory();
        System.out.println(SEPARATEUR);
        System.out.println(" TP 4 - Hibernate et persistance des données");
        System.out.println(" Mapping objet-relationnel, DAO générique et requêtes HQL");
        System.out.println(SEPARATEUR);
        System.out.printf(" JDK          : %s (%s)%n",
                System.getProperty("java.version"), System.getProperty("java.vendor"));
        System.out.printf(" Hibernate    : %s%n", HibernateUtil.versionHibernate());
        System.out.printf(" Base         : %s%n", HibernateUtil.urlCourante());
        System.out.println(SEPARATEUR);
        System.out.println();
    }

    private static void titre(String numero, String libelle) {
        System.out.println();
        System.out.println("[" + numero + "] " + libelle);
        System.out.println("--------------------------------------------------------------------------------");
    }

    private static void verifier(String libelle, boolean condition, Object observe) {
        verifications++;
        if (condition) {
            conformes++;
        } else {
            ecarts.add(libelle);
        }
        System.out.printf("  %-7s %-46s | %s%n", condition ? "[OK]" : "[ECHEC]", libelle, observe);
    }

    private static int bilan() {
        System.out.println();
        System.out.println(SEPARATEUR);
        System.out.printf(" BILAN : %d vérification(s) conforme(s) sur %d%n", conformes, verifications);
        if (!ecarts.isEmpty()) {
            System.out.println(" Écarts détectés :");
            for (String ecart : ecarts) {
                System.out.println("   - " + ecart);
            }
        }
        System.out.println(ecarts.isEmpty()
                ? " État du projet : toutes les couches (entités, DAO, services) fonctionnent."
                : " État du projet : au moins une couche est a corriger.");
        System.out.println(SEPARATEUR);
        return ecarts.isEmpty() ? 0 : 1;
    }

    /** References conservees entre les etapes du scenario. */
    private static final class JeuDeDonnees {
        private Salle s1;
        private Salle s2;
        private Salle s3;
        private Machine dell1;
        private Machine hp1;
        private Machine dell2;
        private Machine lenovo1;
    }
}
