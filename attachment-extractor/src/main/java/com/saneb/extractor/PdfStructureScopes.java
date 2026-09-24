package com.saneb.extractor;

import java.util.*;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;

/** 구조 태그와 ParentTree 양쪽에서 같은 페이지/MCID를 가리키는 문단·표 행만 허용한다. */
final class PdfStructureScopes {
    record Scope(String locator, boolean tableRow) { }
    record Binding(Scope scope, COSDictionary owner) { }
    private static final Set<String> CONTAINERS=Set.of("Document","Part","Sect","Div","L","LI","LBody","Table","THead","TBody","TFoot");
    private static final Set<String> PARAGRAPHS=Set.of("P","H","H1","H2","H3","H4","H5","H6");
    private final Map<COSDictionary,Integer> pages=new IdentityHashMap<>();
    private final Map<COSDictionary,LinkedHashMap<Integer,Binding>> bindings=new IdentityHashMap<>();
    private final Set<COSBase> visited=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Scope,COSDictionary> scopePages=new HashMap<>();
    private int nodes, scopeNumber, lastPage;

    static Map<Integer,Binding> selectPage(PdfStructureScopes scopes,COSDictionary page) {
        return scopes==null?Map.of():scopes.bindings.getOrDefault(page,new LinkedHashMap<>());
    }
    static PdfStructureScopes select(PDDocument document) {
        try {
            var result=new PdfStructureScopes();int pageNumber=0;
            for(var page:document.getPages())result.pages.put(page.getCOSObject(),++pageNumber);
            var catalog=document.getDocumentCatalog();var marked=catalog.getMarkInfo();var root=catalog.getStructureTreeRoot();
            if(marked==null || !marked.isMarked() || marked.isSuspect() || root==null)throw invalid();
            var dictionary=root.getCOSObject();
            if(dictionary.containsKey(COSName.getPDFName("RoleMap")) || dictionary.containsKey(COSName.getPDFName("ClassMap")))throw invalid();
            result.walk(dictionary.getDictionaryObject(COSName.K),dictionary,null,null,"StructTreeRoot",0);
            var parents=new HashMap<Integer,COSArray>();
            result.readParents(dictionary.getDictionaryObject(COSName.PARENT_TREE),parents,0);
            var usedParents=new HashSet<Integer>();
            for(var entry:result.bindings.entrySet()) {
                var key=entry.getKey().getDictionaryObject(COSName.STRUCT_PARENTS);
                if(!(key instanceof COSInteger integer) || integer.longValue()<0 || integer.longValue()>Integer.MAX_VALUE
                        || !usedParents.add(integer.intValue()))throw invalid();
                var values=parents.get(integer.intValue());if(values==null)throw invalid();
                for(var binding:entry.getValue().entrySet())
                    if(binding.getKey()>=values.size() || values.getObject(binding.getKey())!=binding.getValue().owner())throw invalid();
                for(int i=0;i<values.size();i++)
                    if(values.getObject(i)!=null && values.getObject(i)!=COSNull.NULL && !entry.getValue().containsKey(i))throw invalid();
            }
            return result;
        } catch(InvalidStructure | IllegalArgumentException exception) {return null;}
    }
    private void walk(COSBase value,COSDictionary parent,COSDictionary page,Scope scope,String parentType,int depth) {
        if(value==null || ++nodes>20000 || depth>64)throw invalid();
        if(value instanceof COSArray array) {
            if(!visited.add(array))throw invalid();
            for(int i=0;i<array.size();i++)walk(array.getObject(i),parent,page,scope,parentType,depth+1);
            return;
        }
        if(value instanceof COSInteger id) {bind(id,page,scope,parent);return;}
        if(!(value instanceof COSDictionary node) || !visited.add(node))throw invalid();
        var ownPage=node.getDictionaryObject(COSName.PG);
        if(ownPage!=null) {if(!(ownPage instanceof COSDictionary p) || !pages.containsKey(p))throw invalid();page=p;}
        if("MCR".equals(node.getNameAsString(COSName.TYPE))) {
            if(node.containsKey(COSName.getPDFName("Stm")) || !(node.getDictionaryObject(COSName.MCID) instanceof COSInteger id))throw invalid();
            bind(id,page,scope,parent);return;
        }
        if(!"StructElem".equals(node.getNameAsString(COSName.TYPE)) || node.getDictionaryObject(COSName.P)!=parent
                || node.containsKey(COSName.ACTUAL_TEXT) || node.containsKey(COSName.ALT) || node.containsKey(COSName.E))throw invalid();
        String type=node.getNameAsString(COSName.S);
        if(type==null)throw invalid();
        if("TR".equals(parentType) && !Set.of("TD","TH").contains(type)
                || "Table".equals(parentType) && !Set.of("TR","THead","TBody","TFoot").contains(type)
                || Set.of("THead","TBody","TFoot").contains(parentType) && !"TR".equals(type))throw invalid();
        if("TR".equals(type)) {
            if(!Set.of("Table","THead","TBody","TFoot").contains(parentType) || scope!=null)throw invalid();
            scope=new Scope("row:"+(++scopeNumber),true);
        } else if(Set.of("TD","TH").contains(type)) {
            if(!"TR".equals(parentType) || scope==null || !scope.tableRow())throw invalid();
            validateCellAttributes(node.getDictionaryObject(COSName.A));
        } else if(PARAGRAPHS.contains(type)) {
            if(scope!=null && !scope.tableRow())throw invalid();
            if(scope==null)scope=new Scope("paragraph:"+(++scopeNumber),false);
        } else if("Span".equals(type)) {
            if(scope==null)throw invalid();
        } else if(!CONTAINERS.contains(type) || scope!=null)throw invalid();
        walk(node.getDictionaryObject(COSName.K),node,page,scope,type,depth+1);
    }
    private void bind(COSInteger id,COSDictionary page,Scope scope,COSDictionary owner) {
        if(page==null || scope==null || id.longValue()<0 || id.longValue()>20000 || !pages.containsKey(page))throw invalid();
        if(pages.get(page)<lastPage)throw invalid();lastPage=pages.get(page);
        var original=scopePages.putIfAbsent(scope,page);if(original!=null && original!=page)throw invalid();
        var map=bindings.computeIfAbsent(page,p->new LinkedHashMap<>());
        if(map.putIfAbsent(id.intValue(),new Binding(new Scope("page:"+pages.get(page)+":"+scope.locator(),scope.tableRow()),owner))!=null)throw invalid();
    }
    private void validateCellAttributes(COSBase value) {
        if(value==null)return;
        if(!visited.add(value) || ++nodes>20000)throw invalid();
        if(value instanceof COSArray array) {if(array.size()>32)throw invalid();for(int i=0;i<array.size();i++) {
            var attribute=array.getObject(i);if(attribute instanceof COSArray)throw invalid();validateCellAttributes(attribute);
        }return;}
        if(!(value instanceof COSDictionary dictionary))throw invalid();
        for(String name:List.of("RowSpan","ColSpan")) {
            var span=dictionary.getDictionaryObject(COSName.getPDFName(name));
            if(span!=null && (!(span instanceof COSInteger integer) || integer.longValue()!=1))throw invalid();
        }
    }
    private void readParents(COSBase value,Map<Integer,COSArray> result,int depth) {
        if(!(value instanceof COSDictionary node) || !visited.add(node) || ++nodes>20000 || depth>64)throw invalid();
        var nums=node.getDictionaryObject(COSName.NUMS);var kids=node.getDictionaryObject(COSName.KIDS);
        if((nums==null)==(kids==null))throw invalid();
        if(nums!=null) {
            if(!(nums instanceof COSArray array) || array.size()%2!=0 || array.size()>40000)throw invalid();
            for(int i=0;i<array.size();i+=2) {
                if(!(array.getObject(i) instanceof COSInteger key) || key.longValue()<0 || key.longValue()>Integer.MAX_VALUE
                        || !(array.getObject(i+1) instanceof COSArray parents) || parents.size()>20001
                        || result.putIfAbsent(key.intValue(),parents)!=null)throw invalid();
            }
        } else {
            if(!(kids instanceof COSArray array) || array.size()>20000)throw invalid();
            for(int i=0;i<array.size();i++)readParents(array.getObject(i),result,depth+1);
        }
    }
    private static InvalidStructure invalid(){return new InvalidStructure();}
    private static final class InvalidStructure extends RuntimeException { }
}
