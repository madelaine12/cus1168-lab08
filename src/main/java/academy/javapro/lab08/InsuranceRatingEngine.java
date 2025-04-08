package academy.javapro.lab08;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class InsuranceRatingEngine {

    private Map<String, Object> knowledgeBase;
    private List<Rule> rules;

    public InsuranceRatingEngine() {
        this.knowledgeBase = new HashMap<>();
        this.rules = new ArrayList<>();
        initializeKnowledgeBase();
        initializeRules();
    }

    private void initializeKnowledgeBase() {
        knowledgeBase.put("baseRate.sedan", 1000.0);
        knowledgeBase.put("baseRate.suv", 1200.0);
        knowledgeBase.put("baseRate.luxury", 1500.0);
        knowledgeBase.put("baseRate.sports", 1800.0);

        knowledgeBase.put("ageFactor.16-19", 2.0);
        knowledgeBase.put("ageFactor.20-24", 1.5);
        knowledgeBase.put("ageFactor.25-65", 1.0);
        knowledgeBase.put("ageFactor.66+", 1.3);

        knowledgeBase.put("accidentSurcharge.0", 0.0);
        knowledgeBase.put("accidentSurcharge.1", 300.0);
        knowledgeBase.put("accidentSurcharge.2+", 600.0);
    }

    private void initializeRules() {
        rules.add(new Rule("base rate", profile -> true, (profile, premium) -> {
            String vehicleCategory = determineVehicleCategory(profile);
            double baseRate = (double) knowledgeBase.get("baseRate." + vehicleCategory);
            premium.setBaseRate(baseRate);
        }));

        rules.add(new Rule("age factor", profile -> true, (profile, premium) -> {
            int age = profile.getAge();
            double factor = 1.0;
            String explanation = "";

            if (age < 20) {
                factor = (double) knowledgeBase.get("ageFactor.16-19");
                explanation = "Drivers under 20 have higher statistical risk";
            } else if (age < 25) {
                factor = (double) knowledgeBase.get("ageFactor.20-24");
                explanation = "Drivers 20-24 have moderately higher risk";
            } else if (age < 66) {
                factor = (double) knowledgeBase.get("ageFactor.25-65");
                explanation = "Standard rate for drivers 25-65";
            } else {
                factor = (double) knowledgeBase.get("ageFactor.66+");
                explanation = "Slight increase for senior drivers";
            }

            double adjustment = premium.getBaseRate() * (factor - 1.0);
            premium.addAdjustment("Age factor", adjustment, explanation);
        }));

        rules.add(new Rule("accident history", profile -> profile.getAccidentsInLastFiveYears() > 0,
                (profile, premium) -> {
                    int accidents = profile.getAccidentsInLastFiveYears();
                    double surcharge = 0.0;
                    String explanation = "";

                    if (accidents == 1) {
                        surcharge = (double) knowledgeBase.get("accidentSurcharge.1");
                        explanation = "Surcharge for 1 accident in past 5 years";
                    } else if (accidents >= 2) {
                        surcharge = (double) knowledgeBase.get("accidentSurcharge.2+");
                        explanation = "Major surcharge for 2+ accidents in past 5 years";
                    }

                    premium.addAdjustment("Accident history", surcharge, explanation);
                }));
    }

    private String determineVehicleCategory(DriverProfile profile) {
        String make = profile.getVehicleMake();
        String model = profile.getVehicleModel();
    
        if (make.equalsIgnoreCase("ford") && model.equalsIgnoreCase("mustang") ||
            make.equalsIgnoreCase("ferrari") || make.equalsIgnoreCase("porsche") ||
            make.equalsIgnoreCase("corvette")) {
            return "sports"; 
        } 
        else if (make.equalsIgnoreCase("bmw") || make.equalsIgnoreCase("mercedes") ||
                make.equalsIgnoreCase("lexus") || make.equalsIgnoreCase("audi")) {
            return "luxury";
        } 
        else if (model.equalsIgnoreCase("suv") || model.equalsIgnoreCase("explorer") ||
                model.equalsIgnoreCase("tahoe") || model.equalsIgnoreCase("highlander")) {
            return "suv";
        } 
        else {
            return "sedan";
        }
    }

    public Premium calculatePremium(DriverProfile profile) {
        Premium premium = new Premium();

        for (Rule rule : rules) {
            if (rule.matches(profile)) {
                rule.apply(profile, premium);
            }
        }

        return premium;
    }

    static class Rule {

        private String name;
        private Predicate<DriverProfile> condition;
        private BiConsumer<DriverProfile, Premium> action;

        public Rule(String name, Predicate<DriverProfile> condition, BiConsumer<DriverProfile, Premium> action) {
            this.name = name;
            this.condition = condition;
            this.action = action;
        }

        public boolean matches(DriverProfile profile) {
            return condition.test(profile);
        }

        public void apply(DriverProfile profile, Premium premium) {
            action.accept(profile, premium);
        }

        public String getName() {
            return name;
        }
    }
}

