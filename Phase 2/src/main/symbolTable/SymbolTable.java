package main.symbolTable;


import main.symbolTable.exceptions.ItemAlreadyExistsException;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.SymbolTableItem;
import main.symbolTable.utils.Stack;

import java.util.*;



public class SymbolTable {

    //Start of static members

    public static SymbolTable top;
    public static SymbolTable root;
    private static Stack<SymbolTable> stack = new Stack<>();

    public static void push(SymbolTable symbolTable) {
        if (top != null)
            stack.push(top);
        top = symbolTable;
    }

    public static void pop() {
        top = stack.pop();
    }

    //End of static members

    public SymbolTable pre;
    public Set<SymbolTable> post = new HashSet<>();
    private Map<String, SymbolTableItem> items;

    public SymbolTable() {
        this(null);
    }

    public SymbolTable(SymbolTable pre) {
        this.pre = pre;
        this.items = new HashMap<>();
    }

    public void put(SymbolTableItem item) throws ItemAlreadyExistsException {
        if (items.containsKey(item.getKey()))
            throw new ItemAlreadyExistsException();
        items.put(item.getKey(), item);
    }

    public SymbolTableItem getItem(String key, Boolean searchCurrent) throws ItemNotFoundException {
        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
        SymbolTable currentSymbolTable = this;
        if(!searchCurrent) {
            visitedSymbolTables.add(this);
            currentSymbolTable = this.pre;
        }
        while((currentSymbolTable != null) && (!visitedSymbolTables.contains(currentSymbolTable))) {
            visitedSymbolTables.add( currentSymbolTable );
            SymbolTableItem symbolTableItem = currentSymbolTable.items.get(key);
            if( symbolTableItem != null )
                return symbolTableItem;
            currentSymbolTable = currentSymbolTable.pre;
        }
        throw new ItemNotFoundException();
    }

    public Set<SymbolTableItem> getAllItems(String key) {
        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
        Set<SymbolTableItem> foundItems = new HashSet<>();
        visitedSymbolTables.add(this);
        SymbolTable currentSymbolTable = this.pre;
        while((currentSymbolTable != null) && (!visitedSymbolTables.contains(currentSymbolTable))) {
            SymbolTableItem symbolTableItem = currentSymbolTable.items.get(key);
            if( symbolTableItem != null )
                foundItems.add(symbolTableItem);
            visitedSymbolTables.add(currentSymbolTable);
            currentSymbolTable = currentSymbolTable.pre;
        }
        return foundItems;
    }

    private boolean hasConflict(String key, SymbolTable current, Set<SymbolTable> visited) {
        if (visited.contains(current)) {
            return false;
        }

        if (current.items.get(key) != null)
            return true;

        visited.add(current);

        for (SymbolTable i: current.post) {
            if(hasConflict(key, i, visited))
                return true;
        }

        return false;
    }

    public boolean hasConflictWithChildren(String key) {
        Set<SymbolTable> visitedSymbolTables = new HashSet<>();
        return hasConflict(key, this, visitedSymbolTables);
    }

}
