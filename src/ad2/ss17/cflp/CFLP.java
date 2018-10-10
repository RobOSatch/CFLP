package ad2.ss17.cflp;

import java.util.ArrayList;

/**
 * Klasse zum Berechnen der L&ouml;sung mittels Branch-and-Bound.
 * Hier sollen Sie Ihre L&ouml;sung implementieren.
 */
public class CFLP extends AbstractCFLP {
    // Global upper bound, which must not be exceeded by any possible solution.
    private int globalUpperBound;

    // An array holding the minimal distance to the closest facility for each customer index.
    private int[] minimalDistancesForCustomers;

    private Facility[] facilities;

    // Solution variables
    private int totalCosts;
    private int[] solution;

    private CFLPInstance instance;

    public CFLP(CFLPInstance instance) {
        this.instance = instance;

        this.globalUpperBound = Integer.MAX_VALUE;
        this.minimalDistancesForCustomers = new int[instance.getNumCustomers()];

        // Find nearest facility for each customer. Is needed for lower bound calculation.
        for (int i=0; i < instance.getNumCustomers(); i++) {
            int minDistance = Integer.MAX_VALUE;

            for (int j=0; j < instance.getNumFacilities(); j++) {
                if (instance.distance(j,i) < minDistance) {
                    minDistance = instance.distance(j,i);
                }
            }

            minimalDistancesForCustomers[i] = minDistance;
        }

        // Initiate facilities array to help us with managing per facility stuff.
        this.facilities = new Facility[instance.getNumFacilities()];
        for (int facilityIdx=0; facilityIdx < instance.getNumFacilities(); facilityIdx++) {
            facilities[facilityIdx] = new Facility(facilityIdx);
        }

        this.solution = new int[instance.getNumCustomers()];
    }

    /**
     * Diese Methode bekommt vom Framework maximal 30 Sekunden Zeit zur
     * Verf&uuml;gung gestellt um eine g&uuml;ltige L&ouml;sung
     * zu finden.
     * <p>
     * <p>
     * F&uuml;gen Sie hier Ihre Implementierung des Branch-and-Bound-Algorithmus
     * ein.
     * </p>
     */
    @Override
    public void run() {
        //long startTime = System.currentTimeMillis();

        this.globalUpperBound = calcGlobalUpperBound(0);
        branchAndBound(0);

        //long stopTime = System.currentTimeMillis();
        //long elapsedTime = stopTime - startTime;
        //System.out.println("Finished within " + elapsedTime);
    }

    public void branchAndBound(int customerIdx) {
        if (customerIdx < instance.getNumCustomers()) {
            for (Facility f : facilities) {
                f.addCustomer(customerIdx);

                if (this.calcLocalLowerBound(customerIdx + 1) < globalUpperBound) {
                    branchAndBound(customerIdx + 1);
                }

                f.removeCustomer(customerIdx);
            }
        } else {
            globalUpperBound = totalCosts;

            // Construct solution array and setting the solution.
            for (Facility f : facilities) {
                for (int i : f.allottedCustomers) {
                    solution[i] = f.facilityIdx;
                }
            }

            setSolution(globalUpperBound, solution);
        }
    }

    /**
     * A helping class managing a single facility. Only the index is used in the initializer as the rest is taken from the instances arrays.
     */
    public class Facility {
        private int facilityIdx;

        private int k;
        private int usedBandwidth;

        private ArrayList<Integer> allottedCustomers;
        private ArrayList<Integer> calculatedStageCosts;

        private Facility previousState;
        private int previousCosts;

        public Facility(int facilityIdx) {
            this.facilityIdx = facilityIdx;

            this.k = 0;
            this.usedBandwidth = 0;

            this.allottedCustomers = new ArrayList<>();
            initiateBasicStageCosts();
        }

        public Facility(Facility other) {
            this.facilityIdx = other.facilityIdx;

            this.k = other.k;
            this.usedBandwidth = other.usedBandwidth;

            this.allottedCustomers = other.allottedCustomers;
            this.calculatedStageCosts = other.calculatedStageCosts;
        }

        private void resetTo(Facility state) {
            this.usedBandwidth = state.usedBandwidth;
            this.k = state.k;
            this.allottedCustomers = state.allottedCustomers;
            totalCosts = previousCosts;
        }

        private void addCustomer(int customerIdx) {
            previousState = new Facility(this);
            previousCosts = totalCosts;

            usedBandwidth += instance.bandwidthOf(customerIdx);

            while (usedBandwidth > calcMaxBandwidthAt(this.k)) {
                this.changeStage(CHANGE_TYPE.UPGRADE);
            }

            this.allottedCustomers.add(customerIdx);
            totalCosts += instance.distance(this.facilityIdx, customerIdx) * instance.distanceCosts;
        }

        private void removeCustomer(int customerIdx) {
            //resetTo(previousState);

            usedBandwidth -= instance.bandwidthOf(customerIdx);

            while (usedBandwidth <=  calcMaxBandwidthAt(this.k - 1)) {
                this.changeStage(CHANGE_TYPE.DOWNGRADE);
            }

            
            this.allottedCustomers.remove(this.allottedCustomers.indexOf(customerIdx));

            totalCosts -= instance.distance(this.facilityIdx, customerIdx) * instance.distanceCosts;
        }

        private int calcOpeningCostsAt(int k) {
            if (k < calculatedStageCosts.size()) {
                return calculatedStageCosts.get(k);
            }

            int openingCosts = Math.addExact(Math.addExact(calcOpeningCostsAt(k-1), calcOpeningCostsAt(k-2)), (4-k) * instance.baseOpeningCostsOf(facilityIdx));
            calculatedStageCosts.add(openingCosts);
            return openingCosts;
        }

        private int calcMaxBandwidthAt(int k) {
            return instance.maxBandwidthOf(facilityIdx) * k;
        }

        private boolean hasSpaceForCustomer(int customerIdx) {
            return (usedBandwidth + instance.bandwidthOf(customerIdx)) <= calcMaxBandwidthAt(this.k);
        }

        private void changeStage(CHANGE_TYPE changeType) {
            totalCosts -= calcOpeningCostsAt(k);

            if (changeType.equals(CHANGE_TYPE.UPGRADE)) {
                this.k++;
            } else {
                this.k--;
            }

            totalCosts += calcOpeningCostsAt(k);
        }

        private void initiateBasicStageCosts() {
            this.calculatedStageCosts = new ArrayList<>();
            this.calculatedStageCosts.add(0);
            this.calculatedStageCosts.add(instance.baseOpeningCostsOf(facilityIdx));
            this.calculatedStageCosts.add((int) Math.ceil(1.5f * instance.baseOpeningCostsOf(facilityIdx)));
        }
    }

    private enum CHANGE_TYPE {
        UPGRADE,
        DOWNGRADE
    }

    // Calculates the global upper bound by calculating the costs of a valid solution.
    public int calcGlobalUpperBound(int customerIdx) {
        if (customerIdx < instance.getNumCustomers()) {
            Facility cheapestFacility = new Facility(0);
            int minimalCosts = Integer.MAX_VALUE;

            for (Facility f : facilities) {
                int costsForNextFacilityStage = f.calcOpeningCostsAt(f.k + 1) - f.calcOpeningCostsAt(f.k) + instance.distance(f.facilityIdx, customerIdx) * instance.distanceCosts;

                if (costsForNextFacilityStage < minimalCosts) {
                    cheapestFacility = f;
                    minimalCosts = costsForNextFacilityStage;
                }
            }

            cheapestFacility.addCustomer(customerIdx);
            int newGlobalUpperBound = calcGlobalUpperBound(customerIdx+1);
            cheapestFacility.removeCustomer(customerIdx);

            return newGlobalUpperBound;
        } else {
            return totalCosts;
        }
    }

    // Calculates the local lower bound by adding every not allotted customer to the cheapest facility. The distance costs are kept fictionally minimal, by selecting the closest facility's ones.
    public int calcLocalLowerBound(int customerIdx) {
        if (customerIdx < instance.getNumCustomers()) {
            int minimalCosts = Integer.MAX_VALUE;
            Facility cheapestFacility = new Facility(0);

            for (Facility f : facilities) {
                /**if (f.hasSpaceForCustomer(customerIdx)) {
                    if (f.calcOpeningCostsAt(f.k) < minimalCosts) {
                        minimalCosts = f.calcOpeningCostsAt(f.k);
                        cheapestFacility = f;
                    }
                } else {*/
                    int costsForNextFacilityStage = f.calcOpeningCostsAt(f.k + 1) - f.calcOpeningCostsAt(f.k);

                    if (costsForNextFacilityStage < minimalCosts) {
                        minimalCosts = costsForNextFacilityStage;
                        cheapestFacility = f;
                    }
                //}
            }

            // Add customer (if not yet allotted) to cheapest possible facility with a minimal possible distance. Reverts the cost change caused by addCustomer.
            cheapestFacility.addCustomer(customerIdx);
            totalCosts -= instance.distance(cheapestFacility.facilityIdx, customerIdx) * instance.distanceCosts;
            totalCosts += minimalDistancesForCustomers[customerIdx] * instance.distanceCosts;

            int localLowerBound = calcLocalLowerBound(customerIdx+1);

            // Remove customer (if not yet allotted) from cheapest possible facility with a minimal possible distance. Reverts the cost change caused by removeCustomer.
            totalCosts -= minimalDistancesForCustomers[customerIdx] * instance.distanceCosts;
            cheapestFacility.removeCustomer(customerIdx);
            totalCosts += instance.distance(cheapestFacility.facilityIdx, customerIdx) * instance.distanceCosts;

            return localLowerBound;
        } else {
            return totalCosts;
        }
    }
}
