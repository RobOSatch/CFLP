package ad2.ss17.cflp;

import java.util.*;

/**
 * Klasse zum Berechnen der L&ouml;sung mittels Branch-and-Bound.
 * Hier sollen Sie Ihre L&ouml;sung implementieren.
 */
public class CFLPJonas extends AbstractCFLP {

    private int costs;
    private int upperBound;
    private final int distanceCosts;
    private final int numCustomers;
    private final int numFacilities;
    private final int[] baseOpeningCosts;
    private final int[] maxBandWidths;
    private final int[] bandwidthOfCustomers;
    private final int[][] distances; //[facility][customer]
    private final int[] minDistances;
    private final Facility[] facilities;
    private final int[] solution;



    public CFLPJonas(CFLPInstance cflpInstance) {

        this.costs = 0;
        this.distanceCosts = cflpInstance.distanceCosts;
        this.numCustomers = cflpInstance.getNumCustomers();
        this.numFacilities = cflpInstance.getNumFacilities();
        this.baseOpeningCosts = cflpInstance.openingCosts;
        this.maxBandWidths = cflpInstance.maxBandwidths;
        this.bandwidthOfCustomers = cflpInstance.bandwidths;
        this.distances = cflpInstance.distances;
        this.minDistances = new int[numCustomers];
        for(int c = 0; c < numCustomers; c++) {
            Integer min = null;
            for (int f = 0; f < numFacilities; f++) {
                if(min == null || distances[f][c] < min) {
                    min = distances[f][c];
                }
            }
            minDistances[c] = min;
        }

        this.solution = new int[numCustomers];
        //facilities
        this.facilities = new Facility[cflpInstance.getNumFacilities()];
        for(int facility = 0; facility < numFacilities; facility++){
            facilities[facility] = new Facility(facility);
        }
    }

    /**
     * Calculates the best solution for the problem.
     * calculates the upper bound and than starts to build the tree.
     */
    @Override
    public void run() {
        upperBound = calcUpperBound(0);
        solveCFLP(0);
    }

    /**
     * Recursive method that calls itself all the way to the bottom of the tree.
     * If the lowest calculated bound is not enough to beat the upper bound, it stops the calculation
     * and goes on with the next possible combination.
     */
    private void solveCFLP(int customer){

        if(customer < numCustomers){

            for(Facility f : facilities){

                f.addCustomer(customer, true);
                if(costs < upperBound && calcLowerBound(customer+1) < upperBound)
                    solveCFLP(customer+1);
                f.removeCustomer(true);
            }
        }else if(costs < upperBound){

            upperBound = costs;
            for(Facility f : facilities)
                for(Integer c : f.customers)
                    solution[c] = f.facility;
            setSolution(costs, solution);
        }
    }

    private int calcLowerBound(int customer){

        if(customer < numCustomers) {
            Facility minCostFacility = facilities[0];
            int minCost = facilities[0].calcOpeningCostsForNextStage();
            for (Facility f : facilities) {
                if (f.calcOpeningCostsForNextStage() < minCost) {
                    minCostFacility = f;
                    minCost = f.calcOpeningCostsForNextStage();
                }
            }
            minCostFacility.addCustomer(customer, false);
            costs += minDistances[customer] * distanceCosts;
            int lowerBound = calcLowerBound(customer+1);
            costs -= minDistances[customer] * distanceCosts;
            minCostFacility.removeCustomer(false);
            return lowerBound;
        } else {
            return costs;
        }
    }

    private int calcUpperBound(int customer){

        if(customer < numCustomers) {
            Facility minCostFacility = facilities[0];
            int minCost = facilities[0].calcOpeningCostsForNextStage() + distances[0][customer] * distanceCosts;
            for (int facility = 0; facility < numFacilities; facility++) {
                Facility f = facilities[facility];
                int costs = f.calcOpeningCostsForNextStage() + distances[facility][customer] * distanceCosts;
                if (costs < minCost) {
                    minCostFacility = f;
                    minCost = costs;
                }
            }
            minCostFacility.addCustomer(customer, true);
            int upperBound = calcUpperBound(customer+1);
            minCostFacility.removeCustomer(true);
            return upperBound;
        }else {
            return costs;
        }
    }

    private class Facility {

        private final int facility;
        private int stage;
        private int sumBandwidths;
        private final Stack<Integer> customers;
        private final List<Integer> openingCosts;

        private Facility(int facility){
            this.facility = facility;
            this.stage = 0;
            this.sumBandwidths = 0;
            this.customers = new Stack<Integer>();
            //opening-costs init
            this.openingCosts = new ArrayList<>();
            this.openingCosts.add(0);
            this.openingCosts.add(baseOpeningCosts[facility]);
            this.openingCosts.add((int) Math.ceil(1.5f * baseOpeningCosts[facility]));
        }

        private void addCustomer(int customer, boolean addDistanceCosts){
            if(addDistanceCosts)
                costs += distances[facility][customer] * distanceCosts;

            sumBandwidths += bandwidthOfCustomers[customer];

            //could be necessary more than one time
            while(sumBandwidths > getBandwidth())
                increaseStage();
            customers.push(customer);
        }

        private void removeCustomer(boolean subDistanceCosts){
            int customer = customers.pop();
            if(subDistanceCosts)
                costs -= distances[facility][customer] * distanceCosts;

            sumBandwidths -= bandwidthOfCustomers[customer];

            //could be necessary more than one time
            while(sumBandwidths <= (stage-1)*maxBandWidths[facility])
                decreaseStage();
        }

        private void increaseStage(){
            costs -= calcOpeningCost(stage);
            this.stage = this.stage + 1;
            costs += calcOpeningCost(stage);
        }

        private void decreaseStage(){
            costs -= calcOpeningCost(stage);
            this.stage = this.stage-1;
            costs += calcOpeningCost(stage);
        }

        private int getBandwidth(){
            return stage * maxBandWidths[facility];
        }

        private int calcOpeningCost(int stage){
            if(stage < openingCosts.size()) {
                return openingCosts.get(stage);
            } else {
                int openingCostForStage = Math.addExact(Math.addExact(calcOpeningCost(stage - 1), calcOpeningCost(stage - 2)), (4 - stage) * baseOpeningCosts[facility]);
                openingCosts.add(openingCostForStage);
                return openingCostForStage;
            }
        }

        private int calcOpeningCostsForNextStage(){
            return calcOpeningCost(stage + 1) - calcOpeningCost(stage);
        }
    }
}
