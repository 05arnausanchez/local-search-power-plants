package energia;

import aima.search.framework.HeuristicFunction;

/**
 * H1: menos el beneficio (AIMA minimiza).
 * penalGarantizados (€/MW) solo se usa en el experimento 5, donde se permite dejar
 * garantizados sin servir y la restricción se impone con una penalización.
 */
public class HeuristicaBeneficio implements HeuristicFunction {
    private final double penalGarantizados;

    public HeuristicaBeneficio() { this(0); }
    public HeuristicaBeneficio(double penalGarantizados) { this.penalGarantizados = penalGarantizados; }

    public double getHeuristicValue(Object o) {
        EstadoEnergia s = (EstadoEnergia) o;
        return -s.getBeneficio() + penalGarantizados * s.getMwGarantizadosSinServir();
    }
}
