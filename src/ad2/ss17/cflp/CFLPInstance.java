package ad2.ss17.cflp;

import java.util.Arrays;

/**
 * Speichert Instanzdaten des Capacitated Facility Location Problems
 */
public class CFLPInstance {
    public final int distanceCosts;

    public int[] openingCosts;
    public int[] maxBandwidths;
    public int[] bandwidths;
    public int[][] distances;

    public CFLPInstance(int[] maxBandwidths, int distanceCosts, int[] openingCosts, int[] bandwidths, int[][] distances) {
        this.maxBandwidths = maxBandwidths;

        this.distanceCosts = distanceCosts;
        this.openingCosts = openingCosts;

        this.bandwidths = bandwidths;
        this.distances = distances;
    }

    public CFLPInstance(CFLPInstance other) {
        maxBandwidths = other.maxBandwidths;
        distanceCosts = other.distanceCosts;

        openingCosts = other.openingCosts.clone();
        bandwidths = other.bandwidths.clone();
        distances = new int[openingCosts.length][bandwidths.length];
        for (int i = 0; i < openingCosts.length; ++i) {
            distances[i] = other.distances[i].clone();
        }
    }

    public int getNumCustomers() {
        return bandwidths.length;
    }

    public int getNumFacilities() {
        return openingCosts.length;
    }


    /**
     * @param facilityIdx Der Index der Facility
     * @return Die Basisbandbreite der Facility
     */
    public int maxBandwidthOf(int facilityIdx) {
        return maxBandwidths[facilityIdx];
    }

    /**
     * @param customerIdx Der Index des Kunden
     * @return Die vom Kunden geforderte Bandbreite
     */
    public int bandwidthOf(int customerIdx) {
        return bandwidths[customerIdx];
    }

    /**
     * @param facilityIdx Der Index der Facility
     * @param customerIdx Der Index des Kunden
     * @return Die Distanz zwischen Facility und Kunde
     */
    public int distance(int facilityIdx, int customerIdx) {
        return distances[facilityIdx][customerIdx];
    }

    /**
     * @param facilityIdx Der Index der Facility
     * @return Die einmalignen Kosten zur Eröffnung der Facility
     */
    public int baseOpeningCostsOf(int facilityIdx) {
        return openingCosts[facilityIdx];
    }

    /**
     * @param solution Eine (Teil-)L&ouml;sung für das CFLP.
     *                 Der Index des Array gibt den Kunden an, der Wert an dieser Position die zugeordnete Facility.
     *                 Ist der Wert kleiner 0 dann wird der Kunde als noch nicht zugeordnet angesehen.
     * @return Gibt den Zielfunktionswert der aktuellen (Teil-)L&ouml;sung zur&uuml;ck.; ignoriert Arraywerte kleiner 0
     */
    public int calcObjectiveValue(int[] solution) {
        boolean[] openedFacilities = new boolean[getNumFacilities()];
        Arrays.fill(openedFacilities, false);

        if (solution.length != getNumCustomers())
            throw new RuntimeException("Problem beim Ermitteln des Zielfunktionswertes (zu wenige/zu viele Kunden)");

        int[] accBandwidths = new int[getNumFacilities()];
        for (int i = 0; i < solution.length; ++i) {
            if (solution[i] < 0) continue;
            accBandwidths[solution[i]] += bandwidths[i];
        }

        int sumCosts = 0;
        for (int i = 0; i < solution.length; ++i) {
            if (solution[i] < 0) continue;

            if (!openedFacilities[solution[i]]) {
                sumCosts = Math.addExact(sumCosts, factor((int) Math.ceil(accBandwidths[solution[i]] / (double) maxBandwidths[solution[i]]), openingCosts[solution[i]]));
                openedFacilities[solution[i]] = true;
            }
            sumCosts = Math.addExact(sumCosts, distanceCosts * distance(solution[i], i));
        }

        return sumCosts;
    }

    public int factor(int k, int baseCosts) {
        switch (k) {
            case 0:
                return 0;
            case 1:
                return baseCosts;
            case 2:
                return (int) Math.ceil(1.5 * baseCosts);
            default:
                return Math.addExact(Math.addExact(factor(k - 1, baseCosts), factor(k - 2, baseCosts)), (4 - k) * baseCosts);
        }
    }
}
