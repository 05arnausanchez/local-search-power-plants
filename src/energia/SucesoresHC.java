package energia;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;

import java.util.ArrayList;
import java.util.List;

/** Hill Climbing: genera TODOS los sucesores aplicables con los operadores activos. */
public class SucesoresHC implements SuccessorFunction {

    private final boolean usarMover, usarSwap, usarVaciar;

    /** ops: combinación de "mover", "swap" y "vaciar", p. ej. "mover+swap". */
    public SucesoresHC(String ops) {
        usarMover = ops.contains("mover"); usarSwap = ops.contains("swap"); usarVaciar = ops.contains("vaciar");
    }

    public List getSuccessors(Object o) {

        EstadoEnergia s = (EstadoEnergia) o;
        s.materializar();
        int nCli = EstadoEnergia.getNumClientes(), nCen = EstadoEnergia.getNumCentrales();
        List<Successor> lista = new ArrayList<>();

        if (usarMover)
            for (int i = 0; i < nCli; i++)
                for (int j = EstadoEnergia.NINGUNA; j < nCen; j++)
                    if (s.puedeMover(i, j)) lista.add(new Successor("mover", s.sucesorMover(i, j)));
        if (usarSwap)
            for (int i = 0; i < nCli; i++)
                for (int k = i + 1; k < nCli; k++)
                    if (s.puedeSwap(i, k)) lista.add(new Successor("swap", s.sucesorSwap(i, k)));
        if (usarVaciar)
            for (int c = 0; c < nCen; c++) {
                EstadoEnergia v = s.sucesorVaciar(c);
                if (v != null) lista.add(new Successor("vaciar", v));
            }

        return lista;
        
    }

}
