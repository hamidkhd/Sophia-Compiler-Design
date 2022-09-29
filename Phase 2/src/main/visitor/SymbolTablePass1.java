package main.visitor;

import main.SophiaErrors;
import main.ast.nodes.Program;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.values.ListValue;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.expression.values.primitive.StringValue;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExistsException;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.ClassSymbolTableItem;
import main.symbolTable.items.FieldSymbolTableItem;
import main.symbolTable.items.LocalVariableSymbolTableItem;
import main.symbolTable.items.MethodSymbolTableItem;


public class SymbolTablePass1 extends Visitor<Void> {


    @Override
    public Void visit(Program program) {
        SymbolTable root = new SymbolTable();
        SymbolTable.root = root;
        SymbolTable.push(root);
        for (ClassDeclaration i: program.getClasses()){
            ClassSymbolTableItem c = new ClassSymbolTableItem(i);

            try {
                root.put(c);
            }
            catch (ItemAlreadyExistsException e) {
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of class "+i.getClassName().getName());
                i.setClassName(new Identifier("#"+i.getClassName().getName()));
                c = new ClassSymbolTableItem(i);
                boolean endLoop = false;
                while (!endLoop){
                    try {
                        root.put(c);
                        endLoop = true;
                    } catch (ItemAlreadyExistsException e1) {
                        i.setClassName(new Identifier("#"+i.getClassName().getName()));
                        c = new ClassSymbolTableItem(i);
                    }
                }
            }

            SymbolTable cst = new SymbolTable(root);
            c.setClassSymbolTable(cst);
            SymbolTable.push(cst);
            i.accept(this);
            SymbolTable.pop();
        }

        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration) {
        SymbolTable symbolTable = SymbolTable.top;

        for (FieldDeclaration i: classDeclaration.getFields()){
            FieldSymbolTableItem fst = new FieldSymbolTableItem(i);

            try {
                symbolTable.put(fst);
            }
            catch (ItemAlreadyExistsException e) {
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of field "+i.getVarDeclaration().getVarName().getName());
            }
        }

        if (classDeclaration.getConstructor() != null) {
            MethodDeclaration i = classDeclaration.getConstructor();
            MethodSymbolTableItem msti = new MethodSymbolTableItem(i);

            try {
                symbolTable.put(msti);
            }
            catch (ItemAlreadyExistsException e) {
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of method "+i.getMethodName().getName());
            }

            try{
                symbolTable.getItem(FieldSymbolTableItem.START_KEY + msti.getName(), true);
                SophiaErrors.addError("Line:"+i.getLine()+":Name of method "+i.getMethodName().getName()+" conflicts with a field's name");
            }
            catch (ItemNotFoundException e) {}

            SymbolTable mst = new SymbolTable(symbolTable);
            msti.setMethodSymbolTable(mst);
            SymbolTable.push(mst);
            i.accept(this);
            SymbolTable.pop();
        }

        for (MethodDeclaration i: classDeclaration.getMethods()){
            MethodSymbolTableItem msti = new MethodSymbolTableItem(i);

            try {
                symbolTable.put(msti);
            }
            catch (ItemAlreadyExistsException e) {
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of method "+i.getMethodName().getName());
            }

            try{
                symbolTable.getItem(FieldSymbolTableItem.START_KEY + msti.getName(), true);
                SophiaErrors.addError("Line:"+i.getLine()+":Name of method "+i.getMethodName().getName()+" conflicts with a field's name");
            }
            catch (ItemNotFoundException e) {}

            SymbolTable mst = new SymbolTable(symbolTable);
            msti.setMethodSymbolTable(mst);
            SymbolTable.push(mst);
            i.accept(this);
            SymbolTable.pop();
        }
        return null;
    }

    @Override
    public Void visit(ConstructorDeclaration constructorDeclaration) {
        SymbolTable symbolTable = SymbolTable.top;

        for (VarDeclaration i: constructorDeclaration.getArgs()){
            LocalVariableSymbolTableItem fst = new LocalVariableSymbolTableItem(i);

            try {
                symbolTable.put(fst);
            }
            catch (ItemAlreadyExistsException e) {
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of local variable "+i.getVarName().getName());
            }
        }
        for (VarDeclaration i: constructorDeclaration.getLocalVars()){
            LocalVariableSymbolTableItem fst = new LocalVariableSymbolTableItem(i);

            try {
                symbolTable.put(fst);
            }
            catch (ItemAlreadyExistsException e) {
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of local variable "+i.getVarName().getName());
            }
        }

        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) {
        SymbolTable symbolTable = SymbolTable.top;

        for (VarDeclaration i: methodDeclaration.getArgs()){
            LocalVariableSymbolTableItem fst = new LocalVariableSymbolTableItem(i);

            try {
                symbolTable.put(fst);
            }
            catch (ItemAlreadyExistsException e) {
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of local variable "+i.getVarName().getName());
            }
        }
        for (VarDeclaration i: methodDeclaration.getLocalVars()){
            LocalVariableSymbolTableItem fst = new LocalVariableSymbolTableItem(i);

            try {
                symbolTable.put(fst);
            }
            catch (ItemAlreadyExistsException e) {
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of local variable "+i.getVarName().getName());
            }
        }
        return null;
    }

}
