package net.tianyang928.littleant.entity.ai.brain;

import net.tianyang928.littleant.entity.AntEntity;
import java.util.HashMap;
import java.util.Map;

public final class AntScriptContext {
    private final AntEntity ant;
    private final Map<String, Double> variables = new HashMap<>();
    public AntScriptContext(AntEntity ant) { this.ant = ant; }
    public AntEntity ant() { return ant; }
    public Map<String, Double> variables() { return variables; }
}
