package energia;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulated Annealing: genera UN sucesor, eligiendo al azar el operador y sus parámetros.
 * Si los parámetros elegidos no son aplicables se vuelve a sortear (hasta MAX_INTENTOS veces).
 */
public class SucesoresSA implements SuccessorFunction {

    private static final int MAX_INTENTOS = 1000;
    private final int[] operadores;   // 0 = mover, 1 = swap, 2 = vaciar
    private final Random rnd;

    /** ops: combinación de "mover", "swap" y "vaciar". Cada operador activo se elige con la misma probabilidad. */
    public SucesoresSA(String ops, long semilla) {

        List<Integer> l = new ArrayList<>();

        if (ops.contains("mover")) l.add(0);
        if (ops.contains("swap")) l.add(1);
        if (ops.contains("vaciar")) l.add(2);

        operadores = l.stream().mapToInt(Integer::intValue).toArray();
        rnd = new Random(semilla);

    }

    public List getSuccessors(Object o) {

        EstadoEnergia s = (EstadoEnergia) o;
        s.materializar();
        int nCli = EstadoEnergia.getNumClientes(), nCen = EstadoEnergia.getNumCentrales();
        List<Successor> lista = new ArrayList<>(1);

        for (int t = 0; t < MAX_INTENTOS; t++) {

            int op = operadores[rnd.nextInt(operadores.length)];
            if (op == 0) {
                int i = rnd.nextInt(nCli), j = rnd.nextInt(nCen + 1) - 1;   // j = -1: dejar sin servir
                if (s.puedeMover(i, j)) { lista.add(new Successor("mover", s.sucesorMover(i, j))); break; }
            } else if (op == 1) {
                int i = rnd.nextInt(nCli), k = rnd.nextInt(nCli);
                if (s.puedeSwap(i, k)) { lista.add(new Successor("swap", s.sucesorSwap(i, k))); break; }
            } else {
                EstadoEnergia v = s.sucesorVaciar(rnd.nextInt(nCen));
                if (v != null) { lista.add(new Successor("vaciar", v)); break; }
            }
            
        }

        return lista;
    }
}
