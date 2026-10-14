package energia;

import IA.Energia.Centrales;
import IA.Energia.Clientes;
import aima.search.framework.HeuristicFunction;
import aima.search.framework.Problem;
import aima.search.framework.Search;
import aima.search.framework.SearchAgent;
import aima.search.informed.HillClimbingSearch;
import aima.search.informed.SimulatedAnnealingSearch;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ejecuta una búsqueda y escribe una linea CSV con el resultado (a no ser que le pongams legible=true).
 *
 * Uso: java -cp "bin;lib/*" energia.Main clave=valor ...   (en Linux/Mac, ":" en vez de ";")
 *   exp=etiqueta      nombre del experimento (se copia en la salida)
 *   seed=1            semilla del escenario (centrales y clientes) y de los sucesores SA
 *   cen=5,10,25       número de centrales A,B,C
 *   cli=1000          número de clientes
 *   tipos=0.25,0.30,0.45  proporción XG,MG,G
 *   propg=0.75        proporción de garantizados
 *   alg=HC|SA
 *   ops=mover | mover+swap | mover+vaciar | mover+swap+vaciar ...
 *   init=greedy | orden | aleatoria | vacia
 *   heur=h1 | h2      h1 = -beneficio, h2 = -beneficio + alfa·MW perdidos
 *   alfa=10           peso de las pérdidas en h2 (€/MW)
 *   penal=0           penalización por MW garantizado sin servir (experimento 5)
 *   iter=100000 stiter=100 k=1000 lambda=0.001   parámetros del Simulated Annealing
 *   out=fichero.txt   guarda la asignación final (una línea por cliente: central o -1)
 *   header=true       solo imprime la cabecera del CSV
 *   legible=true      en vez de la línea CSV, imprime el resultado ordenado en secciones
 */
public class Main {

    static final String CABECERA = "exp,seed,nA,nB,nC,cli,propg,alg,ops,init,heur,alfa,penal,iter,stiter,k,lambda,"
            + "beneficio_inicial,beneficio,h,tiempo_ms,pasos,garServ,garTot,noGarServ,noGarTot,"
            + "mwPerdidos,onA,onB,onC,mwA,mwB,mwC,mwGarSinServir,valida";

    public static void main(String[] args) throws Exception {

        Locale.setDefault(Locale.US);   // punto decimal en vezd e coma para no romper el CSV

        Map<String, String> p = leerParametros(args);

        if (Boolean.parseBoolean(p.getOrDefault("header", "false"))) {
            System.out.println(CABECERA);
            return;
        }

        String exp = p.getOrDefault("exp", "prueba");
        int seed = Integer.parseInt(p.getOrDefault("seed", "1"));
        int[] cen = ints(p.getOrDefault("cen", "5,10,25"));
        int cli = Integer.parseInt(p.getOrDefault("cli", "1000"));
        double[] tipos = doubles(p.getOrDefault("tipos", "0.25,0.30,0.45"));
        double propg = Double.parseDouble(p.getOrDefault("propg", "0.75"));
        String alg = p.getOrDefault("alg", "HC").toUpperCase();
        String ops = p.getOrDefault("ops", "mover").toLowerCase();
        String init = p.getOrDefault("init", "greedy").toLowerCase();
        String heur = p.getOrDefault("heur", "h1").toLowerCase();
        double alfa = Double.parseDouble(p.getOrDefault("alfa", "10"));
        double penal = Double.parseDouble(p.getOrDefault("penal", "0"));
        int iter = Integer.parseInt(p.getOrDefault("iter", "100000"));
        int stiter = Integer.parseInt(p.getOrDefault("stiter", "100"));
        int k = Integer.parseInt(p.getOrDefault("k", "1000"));
        double lambda = Double.parseDouble(p.getOrDefault("lambda", "0.001"));

        // ------------------------------------------------------------------ escenario

        Centrales centrales = new Centrales(cen, seed);
        Clientes clientes = new Clientes(cli, tipos, propg, seed);
        EstadoEnergia.inicializar(centrales, clientes);

        // ------------------------------------------------------------------ solución inicial

        EstadoEnergia.permitirQuitarGarantizados = init.equals("vacia");
        EstadoEnergia inicial = solucionInicial(init, seed);
        double beneficioInicial = inicial.getBeneficio();

        // ------------------------------------------------------------------ problema AIMA

        HeuristicFunction h;
        if (heur.equals("h2")) h = new HeuristicaPerdidas(alfa, penal);
        else h = new HeuristicaBeneficio(penal);

        boolean sa = alg.equals("SA");
        Search busqueda;
        Problem problema;

        if (sa) {

            busqueda = new SimulatedAnnealingSearch(iter, stiter, k, lambda);
            problema = new Problem(inicial, new SucesoresSA(ops, seed), new EnergiaGoalTest(), h);

        } else {

            busqueda = new HillClimbingSearch();
            problema = new Problem(inicial, new SucesoresHC(ops), new EnergiaGoalTest(), h);

        }

        long t0 = System.nanoTime();
        SearchAgent agente = new SearchAgent(problema, busqueda);
        long ms = (System.nanoTime() - t0) / 1_000_000;

        EstadoEnergia fin;
        if (sa) fin = (EstadoEnergia) ((SimulatedAnnealingSearch) busqueda).getGoalState();
        else fin = (EstadoEnergia) ((HillClimbingSearch) busqueda).getGoalState();

        String error = fin.esValida();
        if (error != null) System.err.println("ESTADO NO VÁLIDO (seed " + seed + "): " + error);

        if (p.containsKey("out")) guardarAsignacion(p.get("out"), fin);

        // ------------------------------------------------------------------ resultado

        int[] serv = fin.getServidos();
        int[] on = fin.getEncendidasPorTipo();
        double[] mw = fin.getMwPorTipo();

        String pasos = "";
        if (!sa) pasos = "" + agente.getActions().size();   // en SA "pasos" no tiene sentido

        if (Boolean.parseBoolean(p.getOrDefault("legible", "false"))) {

            System.out.printf("%n=== Escenario ===%n");
            System.out.printf("  Experimento : %s   Semilla: %d%n", exp, seed);
            System.out.printf("  Centrales   : A=%d  B=%d  C=%d%n", cen[0], cen[1], cen[2]);
            System.out.printf("  Clientes    : %d (garantizados: %s)%n", cli, f(propg * 100) + "%");
            System.out.printf("%n=== Configuración ===%n");
            System.out.printf("  Algoritmo   : %s   Operadores: %s   Inicial: %s%n", alg, ops, init);
            System.out.printf("  Heurística  : %s   alfa=%s   penal=%s%n", heur, alfa, penal);
            if (sa) System.out.printf("  SA          : iter=%d  stiter=%d  k=%d  lambda=%s%n", iter, stiter, k, lambda);
            System.out.printf("%n=== Resultado ===%n");
            System.out.printf("  Beneficio inicial : %,15.2f €%n", beneficioInicial);
            System.out.printf("  Beneficio final   : %,15.2f €%n", fin.getBeneficio());
            System.out.printf("  Heurística (h)    : %,15.2f%n", h.getHeuristicValue(fin));
            System.out.printf("  Tiempo            : %d ms%n", ms);
            if (!sa) System.out.printf("  Pasos             : %s%n", pasos);
            System.out.printf("  Garantizados      : %d/%d servidos%n", serv[0], serv[2]);
            System.out.printf("  No garantizados   : %d/%d servidos%n", serv[1], serv[3]);
            System.out.printf("  MW perdidos       : %.2f%n", fin.getPerdidas());
            System.out.printf("  MW garantizados sin servir: %.2f%n", fin.getMwGarantizadosSinServir());
            System.out.printf("%n=== Centrales en marcha ===%n");
            System.out.printf("  %-5s %8s %12s%n", "Tipo", "Encend.", "MW usados");
            String[] tipoCen = {"A", "B", "C"};
            for (int i = 0; i < 3; i++)
                System.out.printf("  %-5s %8d %12.2f%n", tipoCen[i], on[i], mw[i]);
            System.out.printf("%n  Solución válida: %s%n", error == null ? "SÍ" : "NO -> " + error);
            return;

        }

        System.out.println(String.join(",",
                exp, "" + seed, "" + cen[0], "" + cen[1], "" + cen[2], "" + cli, "" + propg,
                alg, ops, init, heur, "" + alfa, "" + penal,
                "" + iter, "" + stiter, "" + k, "" + lambda,
                f(beneficioInicial), f(fin.getBeneficio()), f(h.getHeuristicValue(fin)),
                "" + ms, pasos,
                "" + serv[0], "" + serv[2], "" + serv[1], "" + serv[3],
                f(fin.getPerdidas()), "" + on[0], "" + on[1], "" + on[2],
                f(mw[0]), f(mw[1]), f(mw[2]), f(fin.getMwGarantizadosSinServir()),
                error == null ? "1" : "0"));

    }

    // ------------------------------------------------------------------ auxiliares

    /** Lee los argumentos clave=valor (la clave en minúsculas). */
    private static Map<String, String> leerParametros(String[] args) {

        Map<String, String> p = new HashMap<>();

        for (String a : args) {

            int eq = a.indexOf('=');
            if (eq > 0) p.put(a.substring(0, eq).toLowerCase(), a.substring(eq + 1));

        }

        return p;

    }

    private static EstadoEnergia solucionInicial(String init, int seed) {

        switch (init) {
            case "orden":     return EstadoEnergia.solucionOrden();
            case "aleatoria": return EstadoEnergia.solucionAleatoria(seed);
            case "vacia":     return EstadoEnergia.vacia();
            default:          return EstadoEnergia.solucionGreedy();
        }

    }

    /** Una línea por cliente: índice de su central o -1 si no está servido. */
    private static void guardarAsignacion(String fichero, EstadoEnergia s) throws Exception {

        try (PrintWriter w = new PrintWriter(fichero, "UTF-8")) {

            for (int i = 0; i < EstadoEnergia.getNumClientes(); i++) w.println(s.getAsignacion(i));

        }

    }

    private static String f(double x) { return String.format("%.2f", x); }

    private static int[] ints(String s) {

        String[] t = s.split(",");
        int[] r = new int[t.length];

        for (int i = 0; i < t.length; i++) r[i] = Integer.parseInt(t[i].trim());

        return r;

    }

    private static double[] doubles(String s) {

        String[] t = s.split(",");
        double[] r = new double[t.length];

        for (int i = 0; i < t.length; i++) r[i] = Double.parseDouble(t[i].trim());

        return r;

    }

}
