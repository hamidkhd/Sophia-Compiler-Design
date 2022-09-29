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
import main.symbolTable.items.*;

import java.util.HashSet;
import java.util.Set;


public class SymbolTablePass2 extends Visitor<Void> {


    @Override
    public Void visit(Program program) {
        SymbolTable root = SymbolTable.root;
        for (ClassDeclaration i: program.getClasses()) {
            if (i.getParentClassName() == null)
                continue;

            SymbolTableItem curr_class = null;
            SymbolTableItem curr_parent;


            try {
                curr_class = root.getItem(ClassSymbolTableItem.START_KEY + i.getClassName().getName(), true);
            } catch (ItemNotFoundException e) {}
            try{
                curr_parent = root.getItem(ClassSymbolTableItem.START_KEY + i.getParentClassName().getName(), true);
                ((ClassSymbolTableItem) curr_class).getClassSymbolTable().pre = ((ClassSymbolTableItem) curr_parent).getClassSymbolTable();

                ((ClassSymbolTableItem) curr_parent).getClassSymbolTable().post.add(((ClassSymbolTableItem) curr_class).getClassSymbolTable());
            }
            catch (ItemNotFoundException e) {}
        }

        for (ClassDeclaration i: program.getClasses()){
            SymbolTableItem curr_class = null;
            try {
                curr_class = root.getItem(ClassSymbolTableItem.START_KEY + i.getClassName().getName(), true);
            } catch (ItemNotFoundException e) {}
            ClassSymbolTableItem c = (ClassSymbolTableItem) curr_class;

            if (c.hasCycle()) {
                System.out.println("Line:"+i.getLine()+":Class "+i.getClassName().getName()+" is in an inheritance cycle");
            }

            SymbolTable cst = c.getClassSymbolTable();
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
            try {
                symbolTable.getItem(FieldSymbolTableItem.START_KEY + i.getVarDeclaration().getVarName().getName(), false);
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of field "+i.getVarDeclaration().getVarName().getName());
            } catch (ItemNotFoundException e) {}

        }

        if (classDeclaration.getConstructor() != null) {
            MethodDeclaration i = classDeclaration.getConstructor();
            try {
                symbolTable.getItem(MethodSymbolTableItem.START_KEY + i.getMethodName().getName(), false);
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of method "+i.getMethodName().getName());
            } catch (ItemNotFoundException e) {}

            try{
                symbolTable.getItem(FieldSymbolTableItem.START_KEY + i.getMethodName().getName(), false);
                SophiaErrors.addError("Line:"+i.getLine()+":Name of method "+i.getMethodName().getName()+" conflicts with a field's name");
            }
            catch (ItemNotFoundException e) {}


            if (symbolTable.hasConflictWithChildren(FieldSymbolTableItem.START_KEY + i.getMethodName().getName())) {
                SophiaErrors.addError("Line:"+i.getLine()+":Name of method "+i.getMethodName().getName()+" conflicts with a field's name");
            }

        }

        for (MethodDeclaration i: classDeclaration.getMethods()){
            try {
                symbolTable.getItem(MethodSymbolTableItem.START_KEY + i.getMethodName().getName(), false);
                SophiaErrors.addError("Line:"+i.getLine()+":Redefinition of method "+i.getMethodName().getName());
            } catch (ItemNotFoundException e) {}

            try{
                symbolTable.getItem(FieldSymbolTableItem.START_KEY + i.getMethodName().getName(), false);
                SophiaErrors.addError("Line:"+i.getLine()+":Name of method "+i.getMethodName().getName()+" conflicts with a field's name");
            }
            catch (ItemNotFoundException e) {}


            if (symbolTable.hasConflictWithChildren(FieldSymbolTableItem.START_KEY + i.getMethodName().getName())) {
                SophiaErrors.addError("Line:"+i.getLine()+":Name of method "+i.getMethodName().getName()+" conflicts with a field's name");
            }
        }
        return null;
    }
}
