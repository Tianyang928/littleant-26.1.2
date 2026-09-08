package net.tianyang928.littleant.entity.ai.brain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compiler for the deliberately small, Python-like LittleAnt DSL. It is not a Python interpreter. */
public final class AntDslConverter {
    private static final Pattern CALL = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*)\\)");
    private final LinkedHashMap<UUID, BrainBlock> blocks = new LinkedHashMap<>();
    private final Set<String> functionNames = new HashSet<>();
    private final Set<String> listVariables = new HashSet<>();
    private List<Line> lines;
    private int index;

    public Map<UUID, BrainBlock> convert(String source) {
        if (source == null || source.length() > 65536)
            throw new IllegalArgumentException("[AntDslConverter]DSL is empty or too long");
        blocks.clear();
        functionNames.clear();
        listVariables.clear();
        lines = new ArrayList<>();
        String lastLine = "";
        for (String raw : source.replace("\r", "").split("\n")) {
            String t = raw.stripTrailing();
            if (t.isBlank() || t.stripLeading().startsWith("#")) continue;
            int n = raw.length() - raw.stripLeading().length();
            lines.add(new Line(n, t.strip()));
            if (t.strip().startsWith("def ") && !lastLine.strip().startsWith("@")) {
                String definition = t.strip().substring(4);
                int end = definition.indexOf('(');
                functionNames.add((end >= 0 ? definition.substring(0, end) : definition.replace(":", "")).trim());
            }
            lastLine = t;
        }
        index = 0;
        while (index < lines.size()) parseTop();
        if (blocks.size() > 256) throw new IllegalArgumentException("[AntDslConverter]Max module count is: 256");
        return blocks;
    }

    private void parseTop() {
        Line l = lines.get(index);
        if (l.text.startsWith("@")) {
            String op = l.text.substring(1).trim();
            index++;
            if (index < lines.size() && lines.get(index).text.startsWith("def ")) {
                if (!ModuleRegistry.contains(op))
                    throw new IllegalArgumentException("[AntDslConverter]Unknown decorator: " + op);
                String definition = lines.get(index).text.substring(4);
                int end = definition.indexOf('(');
                String name = (end >= 0 ? definition.substring(0, end) : definition.replace(":", "")).trim();
                Line d = lines.get(index++);
                List<UUID> body = parseSuite(d.indent);
                List<String> eventArgs = ModuleRegistry.get(op).inputs().isEmpty()
                        ? List.of() : List.of(quote(name));
                addChain(op, eventArgs, body, null);
            } else {
                List<UUID> body = parseSuite(l.indent);
                addChain(op, List.of(), body, null);
            }
            return;
        }
        if (l.text.startsWith("def ")) {
            index++;
            int open = l.text.indexOf('('), close = l.text.lastIndexOf(')');
            String name = open > 4 ? l.text.substring(4, open).trim() : l.text.substring(4).replace(":", "").trim();
            List<UUID> body = parseSuite(l.indent);
            addChain("function_start", List.of(quote(name)), body, null);
            return;
        }
        index++;
    }

    private List<UUID> parseSuite(int parentIndent) {
        List<UUID> out = new ArrayList<>();
        while (index < lines.size() && lines.get(index).indent > parentIndent) {
            Line l = lines.get(index);
            String s = l.text;
            if (s.startsWith("def ")) {
                index++;
                continue;
            }
            if (s.startsWith("if ") && s.endsWith(":")) {
                out.add(parseIf(l));
                continue;
            }
            if (s.startsWith("if ") && s.contains(": ")) {
                index++;
                int colon = s.indexOf(':');
                UUID body = parseInline(s.substring(colon + 1).trim());
                if (body == null) throw new IllegalArgumentException("[AntDslConverter]Inline if body must be a module call: " + s);
                out.add(addStatement("if", List.of(s.substring(3, colon).trim(), "body:" + body)));
                continue;
            }
            if (s.startsWith("repeat(") && s.endsWith(":")) {
                index++;
                int p = s.indexOf('('), q = s.lastIndexOf(')');
                List<UUID> b = parseSuite(l.indent);
                if (b.isEmpty())
                    throw new IllegalArgumentException("[AntDslConverter]repeat body cannot be empty: " + s);
                out.add(addStatement("repeat", List.of(s.substring(p + 1, q), "body:" + first(b))));
                continue;
            }
            // Python-style for variable in range(stop) / range(start, stop).
            if (s.startsWith("for ") && s.endsWith(":")) {
                index++;
                Matcher range = Pattern.compile("for\\s+[A-Za-z_]\\w*\\s+in\\s+range\\s*\\((.*)\\)\\s*:").matcher(s);
                if (!range.matches()) throw new IllegalArgumentException("[AntDslConverter] Invalid for syntax: " + s);
                List<String> args = splitArgs(range.group(1));
                if (args.size() < 1 || args.size() > 2 || args.stream().anyMatch(String::isBlank))
                    throw new IllegalArgumentException("[AntDslConverter] range() requires stop or start, stop: " + s);
                String variable = s.substring(4, s.indexOf(" in ")).trim();
                listVariables.remove(variable);
                String start = args.size() == 2 ? args.get(0) : "0";
                String count = args.size() == 1 ? args.get(0) : "subtract(" + args.get(1) + "," + start + ")";
                UUID initialize = addStatement("set_variable", List.of(quote(variable), "subtract(" + start + ",1)"));
                UUID increment = addStatement("set_variable", List.of(quote(variable), "add(" + variable + ",1)"));
                List<UUID> b = parseSuite(l.indent);
                if (b.isEmpty()) throw new IllegalArgumentException("[AntDslConverter]for body cannot be empty: " + s);
                UUID bodyStart = b.getFirst();
                blocks.compute(increment, (k, incrementBlock) -> new BrainBlock(incrementBlock.opcode(), incrementBlock.x(), incrementBlock.y(),
                        incrementBlock.id(), incrementBlock.inputs(), bodyStart, incrementBlock.parent()));
                out.add(initialize);
                out.add(addStatement("repeat", List.of(count, "body:" + increment)));
                continue;
            }
            if (s.startsWith("while ") && s.endsWith(":")) {
                index++;
                List<UUID> b = parseSuite(l.indent);
                if (b.isEmpty())
                    throw new IllegalArgumentException("[AntDslConverter]while body cannot be empty: " + s);
                out.add(addStatement("while", List.of(s.substring(6, s.length() - 1).trim(), "body:" + first(b))));
                continue;
            }
            if (s.startsWith("while ") && s.contains(": ")) {
                index++;
                int colon = s.indexOf(':');
                UUID body = parseInline(s.substring(colon + 1).trim());
                if (body == null) throw new IllegalArgumentException("[AntDslConverter]Inline while body must be a module call: " + s);
                out.add(addStatement("while", List.of(s.substring(6, colon).trim(), "body:" + body)));
                continue;
            }
            if (s.equals("else:") || s.startsWith("elif "))
                throw new IllegalArgumentException("[AntDslConverter]Unexpected else/elif without if: " + s);
            index++;
            Matcher m = CALL.matcher(s);
            if (m.matches()) out.add(addStatement(m.group(1), splitArgs(m.group(2))));
            else if (s.contains("=") && !s.contains("==")) {
                int p = s.indexOf('=');
                String variable = s.substring(0, p).trim();
                String value = s.substring(p + 1).trim();
                if (value.equals("[]")) {
                    listVariables.add(variable);
                    out.add(addStatement("new_list", List.of(quote(variable))));
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    listVariables.add(variable);
                    out.add(addStatement("set_list_list", List.of(quote(variable), runtimeListLiteral(value))));
                } else if (expressionType(value) == ValueType.LIST) {
                    listVariables.add(variable);
                    out.add(addStatement("set_list_list", List.of(quote(variable), value)));
                } else {
                    listVariables.remove(variable);
                    out.add(addStatement("set_variable", List.of(quote(variable), value)));
                }
            }
        }
        link(out);
        return out;
    }

    /** Parse an if/elif/else chain into nested if_else modules. */
    private UUID parseIf(Line line) {
        int keywordLength = line.text.startsWith("elif ") ? 5 : 3;
        String condition = line.text.substring(keywordLength, line.text.length() - 1).trim();
        index++;
        List<UUID> trueBody = parseSuite(line.indent);
        if (trueBody.isEmpty())
            throw new IllegalArgumentException("[AntDslConverter]if body cannot be empty: " + line.text);
        UUID falseBody = null;
        if (index < lines.size() && lines.get(index).indent == line.indent) {
            String next = lines.get(index).text;
            if (next.startsWith("elif ") && next.endsWith(":")) {
                Line elif = lines.get(index);
                falseBody = parseIf(elif);
            } else if (next.equals("else:")) {
                index++;
                List<UUID> elseBody = parseSuite(line.indent);
                falseBody = first(elseBody).isEmpty() ? null : addStatement("if", List.of("true()", "body:" + first(elseBody)));
            }
        }
        if (falseBody == null)
            return addStatement("if", List.of(condition, "body:" + first(trueBody)));
        return addStatement("if_else", List.of(condition, "body_if:" + first(trueBody), "body_else:" + falseBody));
    }

    private static String first(List<UUID> ids) {
        return ids.isEmpty() ? "" : ids.getFirst().toString();
    }

    private void link(List<UUID> ids) {
        for (int i = 0; i + 1 < ids.size(); i++) {
            BrainBlock b = blocks.get(ids.get(i));
            blocks.put(b.id(), new BrainBlock(b.opcode(), b.x(), b.y(), b.id(), b.inputs(), ids.get(i + 1), b.parent()));
        }
    }

    private UUID addStatement(String name, List<String> args) {
        if (!ModuleRegistry.contains(name) && functionNames.contains(name)) {
            String functionName = name;
            if (!args.isEmpty())
                throw new IllegalArgumentException("[AntDslConverter]Function calls cannot have parameters: " + functionName);
            name = "call_function";
            args = List.of(quote(functionName));
        }
        if (!ModuleRegistry.contains(name))
            throw new IllegalArgumentException("[AntDslConverter]Unknown module name: " + name);
        BlockDefinition d = ModuleRegistry.get(name);
        List<InputSlot> in = new ArrayList<>();
        Map<String, String> named = new LinkedHashMap<>();
        List<String> positional = new ArrayList<>();
        for (String arg : args) {
            int eq = findNamedEquals(arg);
            if (eq > 0) named.put(arg.substring(0, eq).trim(), arg.substring(eq + 1).trim());
            else positional.add(arg);
        }
        for (String key : named.keySet()) {
            if (d.inputs().stream().noneMatch(input -> input.name().equals(key)))
                throw new IllegalArgumentException("[AntDslConverter]Unknown input '" + key + "' for module: " + name);
        }
        if (positional.size() > d.inputs().size())
            throw new IllegalArgumentException("[AntDslConverter]Too many arguments for module: " + name);
        int positionalIndex = 0;
        for (int i = 0; i < d.inputs().size(); i++) {
            InputDefinition def = d.inputs().get(i);
            String a;
            if (named.containsKey(def.name())) {
                a = named.get(def.name());
            } else if (positionalIndex < positional.size()) {
                a = positional.get(positionalIndex++);
            } else {
                a = def.defaultValue();
            }
            // Control-flow bodies are encoded internally as <input-name>:<UUID>.
            // They are not DSL expressions: parsing the UUID as an expression
            // would treat its '-' characters as subtraction operators (notably
            // for if_else's body_if/body_else inputs).
            String blockPrefix = def.name() + ":";
            if (def.type() == ValueType.BLOCK && a != null && a.startsWith(blockPrefix)) {
                UUID id = UUID.fromString(a.substring(blockPrefix.length()).trim());
                in.add(new InputSlot(def.name(), def.type(), "", id));
            } else {
                UUID child = expression(a, def.type());
                in.add(child == null ? InputSlot.literal(def.name(), def.type(), unquote(a)) : InputSlot.block(def.name(), def.type(), child));
            }
        }
        UUID id = UUID.randomUUID();
        // Parent links are required so nested bodies are not treated as independent roots.
        for (InputSlot slot : in)
            if (slot.blockId() != null && blocks.containsKey(slot.blockId())) {
                BrainBlock child = blocks.get(slot.blockId());
                blocks.put(child.id(), new BrainBlock(child.opcode(), child.x(), child.y(), child.id(), child.inputs(), child.next(), id));
                setParentChain(child.next(), id, new HashSet<>());
            }
        blocks.put(id, new BrainBlock(name, 20 + blocks.size() * 12, 20, id, in, null, null));
        return id;
    }

    private void addChain(String name, List<String> args, List<UUID> body, UUID ignored) {
        UUID id = addStatement(name, args);
        if (!body.isEmpty()) {
            blocks.computeIfPresent(id, (k, b) -> new BrainBlock(b.opcode(), b.x(), b.y(), b.id(), b.inputs(), body.getFirst(), b.parent()));
            setParentChain(body.getFirst(), id, new HashSet<>());
        }
    }

    private UUID parseInline(String statement) {
        Matcher m = CALL.matcher(statement);
        if (m.matches()) return addStatement(m.group(1), splitArgs(m.group(2)));
        return null;
    }

    private void setParentChain(UUID id, UUID parent, Set<UUID> seen) {
        while (id != null && seen.add(id)) {
            BrainBlock b = blocks.get(id);
            if (b == null) return;
            blocks.put(id, new BrainBlock(b.opcode(), b.x(), b.y(), b.id(), b.inputs(), b.next(), parent));
            id = b.next();
        }
    }

    private UUID expression(String s, ValueType expectedType) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("(") && isParenthesesWrapped(s)) {
            return expression(s.substring(1, s.length() - 1), expectedType);
        }
        int or = topLevelWord(s, "or");
        if (or > 0) return addStatement("or", List.of(s.substring(0, or).trim(), s.substring(or + 2).trim()));
        int and = topLevelWord(s, "and");
        if (and > 0) return addStatement("and", List.of(s.substring(0, and).trim(), s.substring(and + 3).trim()));
        if (s.startsWith("not ")) return addStatement("not", List.of(s.substring(4).trim()));
        if (s.startsWith("!")) {
            if (s.startsWith("!=", 0)) {
                int p = findTopLevelOperator(s, "!=");
                if (p > 0) return addStatement("not", List.of("equal(" + s.substring(0, p).trim() + "," + s.substring(p + 2).trim() + ")"));
            }
            return addStatement("not", List.of(s.substring(1).trim()));
        }
        for (String op : new String[]{"!=", "==", ">=", "<=", ">", "<"}) {
            int p = findTopLevelOperator(s, op);
            if (p > 0) {
                String mapped = switch (op) {
                    case "!=" -> "not_equal";
                    case "==" -> "equal";
                    case ">" -> "greater_than";
                    case ">=" -> "greater_than_or_equal";
                    case "<" -> "less_than";
                    case "<=" -> "less_than_or_equal";
                    default -> "equal";
                };
                if (op.equals("!=")) return addStatement("not", List.of("equal(" + s.substring(0, p).trim() + "," + s.substring(p + op.length()).trim() + ")"));
                return addStatement(mapped, List.of(s.substring(0, p).trim(), s.substring(p + op.length()).trim()));
            }
        }
        int addOrSubtract = findLastTopLevelArithmetic(s, "+-");
        if (addOrSubtract > 0) {
            String opcode = s.charAt(addOrSubtract) == '+' ? "add" : "subtract";
            return addStatement(opcode, List.of(s.substring(0, addOrSubtract).trim(), s.substring(addOrSubtract + 1).trim()));
        }
        int multiplyOrDivide = findLastTopLevelArithmetic(s, "*/%");
        if (multiplyOrDivide > 0) {
            String opcode = switch (s.charAt(multiplyOrDivide)) {
                case '*' -> "multiply";
                case '/' -> "divide";
                default -> "mod";
            };
            return addStatement(opcode, List.of(s.substring(0, multiplyOrDivide).trim(), s.substring(multiplyOrDivide + 1).trim()));
        }
        Matcher m = CALL.matcher(s);
        if (m.matches()) return addStatement(m.group(1), splitArgs(m.group(2)));
        if (s.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            String getter = listVariables.contains(s) || expectedType == ValueType.LIST ? "get_list" : "get_variable";
            return addStatement(getter, List.of(quote(s)));
        }
        return null;
    }

    /** Determines whether an assignment expression produces a named-list value. */
    private ValueType expressionType(String expression) {
        String s = expression == null ? "" : expression.trim();
        while (s.startsWith("(") && isParenthesesWrapped(s)) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return listVariables.contains(s) ? ValueType.LIST : ValueType.ANY;
        }
        Matcher call = CALL.matcher(s);
        if (call.matches() && ModuleRegistry.contains(call.group(1))) {
            return ModuleRegistry.outputType(call.group(1));
        }
        return ValueType.ANY;
    }

    private static List<String> splitArgs(String s) {
        List<String> r = new ArrayList<>();
        if (s.isBlank()) return r;
        int d = 0;
        int brackets = 0;
        char quote = 0;
        boolean escaped = false;
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (escaped) {
                escaped = false;
                b.append(c);
                continue;
            }
            if (quote != 0 && c == '\\') {
                escaped = true;
                b.append(c);
                continue;
            }
            if (c == '\"' || c == '\'') {
                if (quote == 0) quote = c;
                else if (quote == c) quote = 0;
            }
            if (c == ',' && quote == 0 && d == 0 && brackets == 0) {
                r.add(b.toString().trim());
                b.setLength(0);
            } else {
                if (c == '(' && quote == 0) d++;
                if (c == ')' && quote == 0) d--;
                if (c == '[' && quote == 0) brackets++;
                if (c == ']' && quote == 0) brackets--;
                b.append(c);
            }
        }
        if (quote != 0 || d != 0 || brackets != 0)
            throw new IllegalArgumentException("[AntDslConverter]Unbalanced quote, parentheses, or list brackets in arguments: " + s);
        r.add(b.toString().trim());
        return r;
    }

    /** Converts Python-style quoted list literals to the runtime's unquoted string-list protocol. */
    private static String runtimeListLiteral(String value) {
        return AntBlackboard.parseList(value).stream().collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static int topLevelWord(String s, String word) {
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i + word.length() <= s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (quote != 0 && c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '\"' || c == '\'') {
                if (quote == 0) quote = c;
                else if (quote == c) quote = 0;
                continue;
            }
            if (quote != 0) continue;
            if (c == '(') depth++; else if (c == ')') depth--;
            if (depth == 0 && s.startsWith(word, i) && (i == 0 || Character.isWhitespace(s.charAt(i - 1))) && (i + word.length() == s.length() || Character.isWhitespace(s.charAt(i + word.length())))) return i;
        }
        return -1;
    }

    private static int findTopLevelOperator(String s, String operator) {
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i + operator.length() <= s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '"') quoted = !quoted;
            if (quoted) continue;
            if (c == '(') depth++; else if (c == ')') depth--;
            if (depth == 0 && s.startsWith(operator, i)) return i;
        }
        return -1;
    }

    /** Finds the rightmost binary arithmetic operator so equal-precedence operators remain left-associative. */
    private static int findLastTopLevelArithmetic(String s, String operators) {
        int depth = 0;
        boolean quoted = false;
        int result = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '"') quoted = !quoted;
            if (quoted) continue;
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && operators.indexOf(c) >= 0) {
                if ((c == '-' || c == '+') && i == 0) continue;
                if ((c == '-' || c == '+') && i > 0) {
                    int previousIndex = i - 1;
                    while (previousIndex >= 0 && Character.isWhitespace(s.charAt(previousIndex))) previousIndex--;
                    if (previousIndex < 0 || "(,=<>!+-*/%".indexOf(s.charAt(previousIndex)) >= 0) continue;
                }
                result = i;
            }
        }
        return result;
    }

    private static boolean isParenthesesWrapped(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth == 0 && i < s.length() - 1) return false;
        }
        return true;
    }

    private static int findNamedEquals(String s) {
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (quote != 0 && c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '\'' || c == '"') {
                if (quote == 0) quote = c;
                else if (quote == c) quote = 0;
                continue;
            }
            if (quote != 0) continue;
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == '=' && depth == 0) {
                // A named argument has an identifier on the left. Requiring
                // that shape prevents comparison operators (<=, >=, !=, ==)
                // in positional expressions from being mistaken for name=value.
                String left = s.substring(0, i).trim();
                if (left.matches("[A-Za-z_][A-Za-z0-9_]*")
                        && (i + 1 >= s.length() || s.charAt(i + 1) != '=')) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) ? s.substring(1, s.length() - 1) : s;
    }

    private record Line(int indent, String text) {
    }
}
