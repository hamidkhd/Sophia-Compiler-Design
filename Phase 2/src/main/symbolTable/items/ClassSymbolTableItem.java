package main.symbolTable.items;


import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.symbolTable.SymbolTable;

import java.util.HashSet;
import java.util.Set;

public class ClassSymbolTableItem extends SymbolTableItem {
    public static final String START_KEY = "Class_";
    private SymbolTable classSymbolTable;
    private ClassDeclaration classDeclaration;

    public ClassSymbolTableItem(ClassDeclaration classDeclaration) {
        this.classDeclaration = classDeclaration;
        this.name = classDeclaration.getClassName().getName();
    }

    public SymbolTable getClassSymbolTable() {
        return classSymbolTable;
    }

    public void setClassSymbolTable(SymbolTable classSymbolTable) {
        this.classSymbolTable = classSymbolTable;
    }

    public ClassDeclaration getClassDeclaration() {
        return classDeclaration;
    }

    public void setClassDeclaration(ClassDeclaration classDeclaration) {
        this.classDeclaration = classDeclaration;
    }

    public boolean hasCycle() {
        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
        SymbolTable currentSymbolTable = this.classSymbolTable.pre;
        while(currentSymbolTable != null && !visitedSymbolTables.contains(currentSymbolTable)) {
            visitedSymbolTables.add(currentSymbolTable);
            if (visitedSymbolTables.contains(this.classSymbolTable))
                return true;
            currentSymbolTable = currentSymbolTable.pre;
        }
        return false;
    }

    @Override
    public String getKey() {
        return START_KEY + this.name;
    }
}
