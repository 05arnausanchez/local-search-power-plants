package energia;

import aima.search.framework.GoalTest;

/** En búsqueda local no hay estado objetivo: la búsqueda termina cuando el algoritmo lo decide. */
public class EnergiaGoalTest implements GoalTest {
    public boolean isGoalState(Object estado) {
        return false;
    }
}
