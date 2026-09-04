package net.tianyang928.littleant.entity.ai.brain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compiler for the deliberately small, Python-like LittleAnt DSL. It is not a Python interpreter. */
public final class LittleAntDslConverter {
    private static final Pattern CALL = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*\\((.*)\\)");
    private final LinkedHashMap<UUID, BrainBlock> blocks = new LinkedHashMap<>();
    private List<Line> lines; private int index;

    public Map<UUID, BrainBlock> convert(String source) {
        if (source == null || source.length() > 65536) throw new IllegalArgumentException("DSL 为空或过长");
        blocks.clear();
        lines = new ArrayList<>();
        for (String raw : source.replace("\r", "").split("\n")) {
            String t = raw.stripTrailing(); if (t.isBlank() || t.stripLeading().startsWith("#")) continue;
            int n = raw.length() - raw.stripLeading().length(); lines.add(new Line(n, t.strip()));
        }
        index = 0; while (index < lines.size()) parseTop();
        if (blocks.size() > 256) throw new IllegalArgumentException("模块数量不能超过 256");
        return blocks;
    }
    private void parseTop() {
        Line l=lines.get(index);
        if (l.text.startsWith("@")) { String op=l.text.substring(1).trim(); index++; if(index<lines.size() && lines.get(index).text.startsWith("def ")) { Line d=lines.get(index++); List<UUID> body=parseSuite(d.indent); addChain(op, List.of(), body, null); } else { List<UUID> body=parseSuite(l.indent); addChain(op, List.of(), body, null); } return; }
        if (l.text.startsWith("def ")) { index++; parseSuite(l.indent); return; }
        index++;
    }
    private List<UUID> parseSuite(int parentIndent) {
        List<UUID> out=new ArrayList<>();
        while(index<lines.size() && lines.get(index).indent>parentIndent) {
            Line l=lines.get(index); String s=l.text;
            if (s.startsWith("def ")) { index++; continue; }
            if (s.startsWith("if ") && s.endsWith(":")) { index++; List<UUID> b=parseSuite(l.indent); UUID c=addStatement("if", List.of(s.substring(3,s.length()-1).trim(), "body:"+first(b))); out.add(c); continue; }
            if (s.startsWith("repeat(") && s.endsWith(":") ) { index++; int p=s.indexOf('('), q=s.lastIndexOf(')'); List<UUID> b=parseSuite(l.indent); out.add(addStatement("repeat", List.of(s.substring(p+1,q), "body:"+first(b)))); continue; }
            if (s.equals("else:") || s.startsWith("elif ")) { index++; continue; }
            index++; Matcher m=CALL.matcher(s); if(m.matches()) out.add(addStatement(m.group(1), splitArgs(m.group(2))));
            else if (s.contains("=") && !s.contains("==")) { int p=s.indexOf('='); out.add(addStatement("set_variable", List.of(quote(s.substring(0,p).trim()), s.substring(p+1).trim()))); }
        }
        link(out); return out;
    }
    private static String first(List<UUID> ids){ return ids.isEmpty()?"":ids.get(0).toString(); }
    private void link(List<UUID> ids){ for(int i=0;i+1<ids.size();i++){ BrainBlock b=blocks.get(ids.get(i)); blocks.put(b.id(),new BrainBlock(b.opcode(),b.x(),b.y(),b.id(),b.inputs(),ids.get(i+1),b.parent())); }}
    private UUID addStatement(String name,List<String> args){
        if (!ModuleRegistry.contains(name)) throw new IllegalArgumentException("未知模块: "+name);
        BlockDefinition d=ModuleRegistry.get(name); List<InputSlot> in=new ArrayList<>();
        for(int i=0;i<d.inputs().size();i++){ InputDefinition def=d.inputs().get(i); String a=i<args.size()?args.get(i):def.defaultValue();
            if(a!=null && a.startsWith("body:")){ UUID id=UUID.fromString(a.substring(5)); in.add(new InputSlot(def.name(),def.type(),"",id)); }
            else { UUID child=expression(a); in.add(child==null?InputSlot.literal(def.name(),def.type(),unquote(a)):InputSlot.block(def.name(),def.type(),child)); }
        }
        UUID id=UUID.randomUUID();
        // Parent links are required so nested bodies are not treated as independent roots.
        for (InputSlot slot : in) if (slot.blockId()!=null && blocks.containsKey(slot.blockId())) {
            BrainBlock child=blocks.get(slot.blockId());
            blocks.put(child.id(), new BrainBlock(child.opcode(),child.x(),child.y(),child.id(),child.inputs(),child.next(),id));
        }
        blocks.put(id,new BrainBlock(name,20+blocks.size()*12,20,id,in,null,null)); return id;
    }
    private void addChain(String name,List<String> args,List<UUID> body,UUID ignored){ UUID id=addStatement(name,args); if(!body.isEmpty()){ BrainBlock b=blocks.get(id); blocks.put(id,new BrainBlock(b.opcode(),b.x(),b.y(),b.id(),b.inputs(),body.get(0),b.parent())); } }
    private UUID expression(String s){
        if(s==null)return null; s=s.trim();
        for (String op : new String[]{"==", ">=", "<=", ">", "<"}) {
            int p=s.indexOf(op); if (p>0) {
                String mapped = switch(op){case "=="->"equal"; case ">", ">="->"greater_than"; case "<", "<="->"less_than"; default->"equal";};
                if (op.equals(">=")||op.equals("<=")) mapped=op.equals(">=")?"greater_than":"less_than";
                return addStatement(mapped,List.of(s.substring(0,p).trim(),s.substring(p+op.length()).trim()));
            }
        }
        Matcher m=CALL.matcher(s); if(!m.matches()) return null; return addStatement(m.group(1),splitArgs(m.group(2)));
    }
    private static List<String> splitArgs(String s){ List<String> r=new ArrayList<>(); if(s.isBlank())return r; int d=0; boolean q=false; StringBuilder b=new StringBuilder(); for(char c:s.toCharArray()){ if(c=='\"')q=!q; if(c==','&&!q&&d==0){r.add(b.toString().trim());b.setLength(0);} else {if(c=='('&&!q)d++; if(c==')'&&!q)d--; b.append(c);} } r.add(b.toString().trim()); return r; }
    private static String quote(String s){ return s; } private static String unquote(String s){ if(s==null)return ""; s=s.trim(); return s.length()>=2&&((s.startsWith("\"")&&s.endsWith("\""))||(s.startsWith("'")&&s.endsWith("'")))?s.substring(1,s.length()-1):s; }
    private record Line(int indent,String text){}
}
