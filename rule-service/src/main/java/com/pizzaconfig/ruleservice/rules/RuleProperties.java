package com.pizzaconfig.ruleservice.rules;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "rule")
public class RuleProperties {

    private int cheeseCap = 2;
    private List<String> glutenFreeSizes = List.of("S", "M");
    private int ovenThermalLimit = 10;

    public int getCheeseCap() {
        return cheeseCap;
    }

    public void setCheeseCap(int cheeseCap) {
        this.cheeseCap = cheeseCap;
    }

    public List<String> getGlutenFreeSizes() {
        return glutenFreeSizes;
    }

    public void setGlutenFreeSizes(List<String> glutenFreeSizes) {
        this.glutenFreeSizes = glutenFreeSizes;
    }

    public int getOvenThermalLimit() {
        return ovenThermalLimit;
    }

    public void setOvenThermalLimit(int ovenThermalLimit) {
        this.ovenThermalLimit = ovenThermalLimit;
    }
}
