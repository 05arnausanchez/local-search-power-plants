package energia;

import IA.Energia.Central;
import IA.Energia.Centrales;
import IA.Energia.Cliente;
import IA.Energia.Clientes;
import IA.Energia.VEnergia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Estado del problema: A qué central está asignado cada cliente (-1 = sin servir).
 *
 * Parte estática (compartida por todos los estados, se calcula una vez)
 * 
 * Parte dinámica
 *
 * Sucesores perezosos
 */
public class EstadoEnergia {

    // ------------------------------------------------------------------ parte estática

    static int nCen, nCli;
    static int[] tipoCentral;        // tipo de cada central: 0 = A, 1 = B, 2 = C
    static double[] produccion;      // MW que produce cada central
    static double[] costeOn;         // coste diario en marcha
    static double[] costeOff;        // coste diario en parada
    static double[] consumo;         // MW contratados por cliente
    static boolean[] garantizado;    // ¿es garantizado?
    static double[] ingreso;         // consumo · tarifa (según tipo y contrato)
    static double[] indemnizacion;   // lo que le pagamos al cliente si no le servimos
    static double[] valorServir;     // ingreso + indemnización
    static double[][] consumoReal;   // [cliente][central] MW que gasta un cliente desde una central
    static double beneficioBase;     // beneficio con todas las centrales paradas y nadie servido

    /** Si es true, los operadores pueden dejar sin servir a clientes garantizados */
    public static boolean permitirQuitarGarantizados = false;

    static final double EPS = 1e-9; // margen para los errores de redondeo
    public static final int NINGUNA = -1;

    /** Precalcula todo lo que no cambia durante la búsqueda. */
    public static void inicializar(Centrales centrales, Clientes clientes) throws Exception {

        nCen = centrales.size();
        nCli = clientes.size();

        // Centrales

        tipoCentral = new int[nCen];
        produccion = new double[nCen];
        costeOn = new double[nCen];
        costeOff = new double[nCen];

        for (int c = 0; c < nCen; c++) {

            Central ce = centrales.get(c);
            int t = ce.getTipo();
            tipoCentral[c] = t;
            produccion[c] = ce.getProduccion();
            costeOn[c] = ce.getProduccion() * VEnergia.getCosteProduccionMW(t) + VEnergia.getCosteMarcha(t);
            costeOff[c] = VEnergia.getCosteParada(t);

        }

        // Clientes

        consumo = new double[nCli];
        garantizado = new boolean[nCli];
        ingreso = new double[nCli];
        indemnizacion = new double[nCli];
        valorServir = new double[nCli];
        consumoReal = new double[nCli][nCen];
        beneficioBase = 0;

        for (int c = 0; c < nCen; c++) beneficioBase -= costeOff[c];

        for (int i = 0; i < nCli; i++) {

            Cliente cl = clientes.get(i);
            int t = cl.getTipo();
            consumo[i] = cl.getConsumo();
            garantizado[i] = cl.getContrato() == Cliente.GARANTIZADO;

            double tarifa;
            if (garantizado[i]) tarifa = VEnergia.getTarifaClienteGarantizada(t); 
            else tarifa = VEnergia.getTarifaClienteNoGarantizada(t);
            ingreso[i] = consumo[i] * tarifa;
            if (garantizado[i]) indemnizacion[i] = 0;
            else indemnizacion[i] = consumo[i] * VEnergia.getTarifaClientePenalizacion(t);

            valorServir[i] = ingreso[i] + indemnizacion[i];
            beneficioBase -= indemnizacion[i];

            // calcula el consumo real para cada central
            for (int c = 0; c < nCen; c++) {

                Central ce = centrales.get(c);
                double d = Math.hypot(ce.getCoordX() - cl.getCoordX(), ce.getCoordY() - cl.getCoordY());
                consumoReal[i][c] = consumo[i] * (1 + VEnergia.getPerdida(d));

            }

        }

    }

    // ------------------------------------------------------------------ parte dinámica

    private int[] asignacion;      // null mientras el estado no está materializado
    private double[] ocupacion;    // MW reales que usa cada central
    private int[] nClientes;       // nº de clientes de cada central

    private double beneficio;      // beneficio de esta solución
    private double perdidas;       // MW perdidos en transporte
    private double mwGarNoServ;    // MW de clientes garantizados sin servir
    private int nGarNoServ;        // cuántos garantizados sin servir

    private EstadoEnergia padre;   // el estado del que sale este
    private int op, pa, pb;        // qué operador se aplicó y con qué parámetros
    private static final int MOVER = 1, SWAP = 2, VACIAR = 3;

    /** CONSTRUCTOR 1: Estado vacío, con los arrays reservados y nadie servido. */
    private EstadoEnergia() { }

    /** CONSTRUCTOR 2: Sucesor perezoso de mover o swap: calcula los agregados sin copiar arrays. */
    private EstadoEnergia(EstadoEnergia p, int op, int a, int b) {

        padre = p;
        this.op = op;
        pa = a;
        pb = b;

        beneficio = p.beneficio;
        perdidas = p.perdidas;
        mwGarNoServ = p.mwGarNoServ;
        nGarNoServ = p.nGarNoServ;

        if (op == MOVER) acumularMover(p.asignacion, p.nClientes, a, b);
        else if (op == SWAP) acumularSwap(p.asignacion, a, b);

    }

    /** CONSTRUCTOR 3: Sucesor perezoso de vaciar: el plan de reubicación ya calculado da los incrementos. */
    private EstadoEnergia(EstadoEnergia p, int c, int[][] plan) {

        this(p, VACIAR, c, 0);

        int[] nCl = p.nClientes.clone();
        for (int t = 0; t < plan[0].length; t++) {

            int i = plan[0][t];
            int j = plan[1][t];

            acumularMover(p.asignacion, nCl, i, j);
            nCl[c]--;
            if (j != NINGUNA) nCl[j]++;

        }

    }

    /** Copia los arrays del padre y aplica el operador. Solo se hace con el estado elegido. */
    public void materializar() {

        if (asignacion != null) return;
        padre.materializar();

        double b = beneficio;
        double pe = perdidas;
        double mg = mwGarNoServ;
        int ng = nGarNoServ;

        asignacion = padre.asignacion.clone();
        ocupacion = padre.ocupacion.clone();
        nClientes = padre.nClientes.clone();

        beneficio = padre.beneficio;
        perdidas = padre.perdidas;
        mwGarNoServ = padre.mwGarNoServ;
        nGarNoServ = padre.nGarNoServ;

        if (op == MOVER) mover(pa, pb);
        else if (op == SWAP) swap(pa, pb);
        else vaciar(pa);

        // los agregados recalculados deben coincidir con los del sucesor perezoso
        beneficio = b;
        perdidas = pe;
        mwGarNoServ = mg;
        nGarNoServ = ng;

        padre = null;

    }

    // ------------------------------------------------------------------ incrementos

    private static double perdida(int i, int c) {

        if (c == NINGUNA) return 0;
        return consumoReal[i][c] - consumo[i];

    }

    private void servir(int i, int signo) {

        beneficio += signo * valorServir[i];
        if (garantizado[i]) {
            mwGarNoServ -= signo * consumo[i];
            nGarNoServ -= signo;
        }

    }

    private void acumularMover(int[] asig, int[] nCl, int i, int j) {

        int o = asig[i];

        if (o == NINGUNA && j != NINGUNA) servir(i, +1);    // pasa a estar servido
        if (o != NINGUNA && j == NINGUNA) servir(i, -1);    // se queda sin servir
        if (o != NINGUNA && nCl[o] == 1) beneficio += costeOn[o] - costeOff[o];   // se apaga
        if (j != NINGUNA && nCl[j] == 0) beneficio -= costeOn[j] - costeOff[j];   // se enciende

        perdidas += perdida(i, j) - perdida(i, o);

    }

    private void acumularSwap(int[] asig, int i, int k) {

        int o = asig[i];
        int d = asig[k];

        if (o == NINGUNA) {
            servir(i, +1);
            servir(k, -1);
        }
        if (d == NINGUNA) {
            servir(i, -1);
            servir(k, +1);
        }

        // ninguna central cambia su número de clientes: no se enciende ni se apaga nada
        perdidas += perdida(i, d) + perdida(k, o) - perdida(i, o) - perdida(k, d);

    }

    // ------------------------------------------------------------------ operadores

    /** mover(i, j): asignar el cliente i a la central j, o dejarlo sin servir si j = -1. */
    public boolean puedeMover(int i, int j) {

        int o = asignacion[i];

        if (j == o) return false;
        if (j == NINGUNA) return !garantizado[i] || permitirQuitarGarantizados;

        return ocupacion[j] + consumoReal[i][j] <= produccion[j] + EPS;

    }

    /** swap(i, k): intercambiar las centrales de los clientes i y k. */
    public boolean puedeSwap(int i, int k) {

        int o = asignacion[i];
        int d = asignacion[k];

        if (o == d) return false;

        if (o != NINGUNA && ocupacion[o] - consumoReal[i][o] + consumoReal[k][o] > produccion[o] + EPS) return false;
        if (d != NINGUNA && ocupacion[d] - consumoReal[k][d] + consumoReal[i][d] > produccion[d] + EPS) return false;

        if (o == NINGUNA && garantizado[k] && !permitirQuitarGarantizados) return false;
        if (d == NINGUNA && garantizado[i] && !permitirQuitarGarantizados) return false;

        return true;

    }

    /** Aplica mover sobre este estado (se usa en la inicialización y al materializar). */
    void mover(int i, int j) {

        acumularMover(asignacion, nClientes, i, j);

        int o = asignacion[i];

        if (o != NINGUNA) {
            ocupacion[o] -= consumoReal[i][o];
            nClientes[o]--;
        }
        if (j != NINGUNA) {
            ocupacion[j] += consumoReal[i][j];
            nClientes[j]++;
        }

        asignacion[i] = j;

    }

    void swap(int i, int k) {

        acumularSwap(asignacion, i, k);

        int o = asignacion[i];
        int d = asignacion[k];

        if (o != NINGUNA) ocupacion[o] += consumoReal[k][o] - consumoReal[i][o];
        if (d != NINGUNA) ocupacion[d] += consumoReal[i][d] - consumoReal[k][d];

        asignacion[i] = d;
        asignacion[k] = o;

    }

    /** Plan para vaciar c: cada cliente va a la central encendida más cercana con hueco. null si un garantizado no cabe. */
    private int[][] planVaciar(int c) {

        if (nClientes[c] == 0) return null;

        List<Integer> cl = new ArrayList<>();
        for (int i = 0; i < nCli; i++) if (asignacion[i] == c) cl.add(i);

        cl.sort(Comparator.comparingDouble((Integer i) -> -consumo[i]));

        double[] oc = ocupacion.clone();
        int[][] plan = new int[2][cl.size()];

        for (int t = 0; t < cl.size(); t++) {

            int i = cl.get(t);
            int mejor = NINGUNA;

            for (int j = 0; j < nCen; j++) {

                if (j == c || nClientes[j] == 0) continue;                                      // descartamos si es la propia c o está apagada
                if (oc[j] + consumoReal[i][j] > produccion[j] + EPS) continue;                  // descartamos si no cabe
                if (mejor == NINGUNA || consumoReal[i][j] < consumoReal[i][mejor]) mejor = j;   // se queda con la de menor consumo real (la más cercana)

            }

            if (mejor == NINGUNA && garantizado[i] && !permitirQuitarGarantizados) return null; // no podemos dejar sins ervir a un garantizado (descartamos)
            if (mejor != NINGUNA) oc[mejor] += consumoReal[i][mejor];

            plan[0][t] = i;
            plan[1][t] = mejor;

        }

        return plan;

    }

    void vaciar(int c) {

        int[][] plan = planVaciar(c);
        for (int t = 0; t < plan[0].length; t++) mover(plan[0][t], plan[1][t]);

    }

    /** Sucesores. */
    public EstadoEnergia sucesorVaciar(int c) {

        int[][] plan = planVaciar(c);
        if (plan == null) return null;
        return new EstadoEnergia(this, c, plan);

    }
    public EstadoEnergia sucesorMover(int i, int j) { return new EstadoEnergia(this, MOVER, i, j); }
    public EstadoEnergia sucesorSwap(int i, int k)  { return new EstadoEnergia(this, SWAP, i, k); }

    // ------------------------------------------------------------------ soluciones iniciales

    /** Nadie servido y todas las centrales paradas (punto de partida del experimento 5). */
    public static EstadoEnergia vacia() {

        EstadoEnergia s = new EstadoEnergia();

        s.asignacion = new int[nCli];
        Arrays.fill(s.asignacion, NINGUNA);
        s.ocupacion = new double[nCen];
        s.nClientes = new int[nCen];
        s.beneficio = beneficioBase;

        for (int i = 0; i < nCli; i++) {

            if (garantizado[i]) {

                s.mwGarNoServ += consumo[i];
                s.nGarNoServ++;

            }

        }

        return s;

    }

    /**
     * Solución "mala": cada garantizado, en el orden de la lista, a la primera central
     * con hueco (sin mirar distancias). Los no garantizados quedan sin servir. O(N·C).
     */
    public static EstadoEnergia solucionOrden() {

        EstadoEnergia s = vacia();

        for (int i = 0; i < nCli; i++) {

            if (!garantizado[i]) continue;

            int elegida = NINGUNA;

            for (int c = 0; c < nCen && elegida == NINGUNA; c++) if (s.puedeMover(i, c)) elegida = c;

            if (elegida == NINGUNA) throw new IllegalStateException("solucionOrden: el cliente garantizado " + i + " no cabe");

            s.mover(i, elegida);

        }

        return s;

    }

    /**
     * Solución "buena" (greedy):
     *  1. garantizados de mayor a menor consumo, cada uno a la central con menor consumo real
     *     (la más cercana) que tenga hueco;
     *  2. no garantizados de mayor a menor tarifa por MW, a la central más cercana con hueco,
     *     solo si eso aumenta el beneficio (si la central está parada, encenderla debe compensar).
     * O(N·log N + N·C·log C).
     */
    public static EstadoEnergia solucionGreedy() {

        EstadoEnergia s = vacia();
        List<Integer> gar = new ArrayList<>();
        List<Integer> noGar = new ArrayList<>();

        for (int i = 0; i < nCli; i++) {

            if (garantizado[i]) gar.add(i);
            else noGar.add(i);

        }

        gar.sort(Comparator.comparingDouble((Integer i) -> -consumo[i]));
        noGar.sort(Comparator.comparingDouble((Integer i) -> -ingreso[i] / consumo[i])
                             .thenComparingDouble(i -> -consumo[i]));

        for (int i : gar) {

            int c = centralMasCercanaConHueco(s, i, false);

            if (c == NINGUNA) throw new IllegalStateException("solucionGreedy: el cliente garantizado " + i + " no cabe");

            s.mover(i, c);

        }

        for (int i : noGar) {

            int c = centralMasCercanaConHueco(s, i, true);

            if (c != NINGUNA) s.mover(i, c);

        }

        return s;

    }

    /** Solución aleatoria válida: garantizados en orden aleatorio a una central aleatoria con hueco. */
    public static EstadoEnergia solucionAleatoria(long semilla) {

        Random r = new Random(semilla);
        EstadoEnergia s = vacia();
        List<Integer> orden = new ArrayList<>();
        List<Integer> cs = new ArrayList<>();

        for (int i = 0; i < nCli; i++) if (garantizado[i]) orden.add(i);

        Collections.shuffle(orden, r);

        for (int c = 0; c < nCen; c++) cs.add(c);

        for (int i : orden) {

            Collections.shuffle(cs, r);

            int elegida = NINGUNA;
            for (int c : cs) {
                if (s.puedeMover(i, c)) {
                    elegida = c;
                    break;
                }
            }

            if (elegida == NINGUNA) throw new IllegalStateException("solucionAleatoria: el cliente garantizado " + i + " no cabe");
            s.mover(i, elegida);

        }

        return s;

    }

    private static int centralMasCercanaConHueco(EstadoEnergia s, int i, boolean soloSiGana) {

        int mejor = NINGUNA;

        for (int c = 0; c < nCen; c++) {

            if (!s.puedeMover(i, c)) continue;
            if (soloSiGana) {

                double costeEncender = 0;
                if (s.nClientes[c] == 0) costeEncender = costeOn[c] - costeOff[c];
                double ganancia = valorServir[i] - costeEncender;
                if (ganancia <= 0) continue;

            }

            if (mejor == NINGUNA || consumoReal[i][c] < consumoReal[i][mejor]) mejor = c;

        }
        return mejor;

    }

    // ------------------------------------------------------------------ validación

    /**
     * Recalcula todo desde cero y lo compara con los valores incrementales.
     * Devuelve null si el estado es correcto, o un mensaje con el primer error encontrado.
     */
    public String esValida() {

        materializar();
        double[] oc = new double[nCen];
        int[] nc = new int[nCen];
        double ben = 0, per = 0, mg = 0;

        for (int i = 0; i < nCli; i++) {

            int c = asignacion[i];
            if (c == NINGUNA) {
                if (garantizado[i]) {

                    if (!permitirQuitarGarantizados) return "garantizado " + i + " sin servir";
                    mg += consumo[i];

                } else ben -= indemnizacion[i];

            } else {

                oc[c] += consumoReal[i][c];
                nc[c]++;
                ben += ingreso[i];
                per += consumoReal[i][c] - consumo[i];

            }

        }

        for (int c = 0; c < nCen; c++) {

            if (oc[c] > produccion[c] + 1e-6) return "central " + c + " sobrecargada";
            if (Math.abs(oc[c] - ocupacion[c]) > 1e-6) return "ocupación de " + c + " desincronizada";
            if (nc[c] != nClientes[c]) return "nº de clientes de " + c + " desincronizado";

            if (nc[c] > 0) ben -= costeOn[c];
            else ben -= costeOff[c];

        }

        if (Math.abs(ben - beneficio) > 1e-3) return "beneficio desincronizado: " + ben + " vs " + beneficio;
        if (Math.abs(per - perdidas) > 1e-6) return "pérdidas desincronizadas";
        if (Math.abs(mg - mwGarNoServ) > 1e-6) return "MW garantizados sin servir desincronizados";

        return null;

    }

    // ------------------------------------------------------------------ consultas

    public static int getNumCentrales() { return nCen; }
    public static int getNumClientes() { return nCli; }
    public static boolean esGarantizado(int i) { return garantizado[i]; }

    public double getBeneficio() { return beneficio; }
    public double getPerdidas() { return perdidas; }
    public double getMwGarantizadosSinServir() { return mwGarNoServ; }
    public int getGarantizadosSinServir() { return nGarNoServ; }

    public int getAsignacion(int i) {

        materializar();
        return asignacion[i];

    }

    /** [garantizados servidos, no garantizados servidos, garantizados totales, no garantizados totales] */
    public int[] getServidos() {

        materializar();
        int[] r = new int[4];
        for (int i = 0; i < nCli; i++) {

            int k = 1;
            if (garantizado[i]) k = 0;
            r[k + 2]++;
            if (asignacion[i] != NINGUNA) r[k]++;

        }
        return r;

    }

    /** Centrales en marcha por tipo (A, B, C). */
    public int[] getEncendidasPorTipo() {

        materializar();
        int[] r = new int[3];
        for (int c = 0; c < nCen; c++) if (nClientes[c] > 0) r[tipoCentral[c]]++;
        return r;

    }

    /** MW entregados (con pérdidas) por cada tipo de central. */
    public double[] getMwPorTipo() {

        materializar();
        double[] r = new double[3];
        for (int c = 0; c < nCen; c++) r[tipoCentral[c]] += ocupacion[c];
        return r;

    }

    /** Resumen del estado (beneficio, MW perdidos y garantizados sin servir) para depurar. */
    @Override
    public String toString() {
        return String.format("beneficio=%.2f perdidas=%.2fMW garantizadosSinServir=%d", beneficio, perdidas, nGarNoServ);
    }

}
