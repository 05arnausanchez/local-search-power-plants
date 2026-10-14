package energia;

import aima.search.framework.HeuristicFunction;

/**
 * H2: -beneficio + alfa · MW perdidos en transporte.
 * Con H1, mover un cliente entre dos centrales encendidas no cambia el beneficio, así que
 * el Hill Climbing nunca lo hace (solo acepta mejoras estrictas). H2 premia acercar clientes
 * a su central, lo que libera capacida dpara servir a más clientes después.
 * alfa debe ser pequeño frente a las tarifas para que servir a un cliente siga mandando.
 */
public class HeuristicaPerdidas implements HeuristicFunction {
    private final double alfa, penalGarantizados;

    public HeuristicaPerdidas(double alfa) { this(alfa, 0); }
    public HeuristicaPerdidas(double alfa, double penalGarantizados) {
        this.alfa = alfa; this.penalGarantizados = penalGarantizados;
    }

    public double getHeuristicValue(Object o) {
        EstadoEnergia s = (EstadoEnergia) o;
        return -s.getBeneficio() + alfa * s.getPerdidas() + penalGarantizados * s.getMwGarantizadosSinServir();
    }
}
