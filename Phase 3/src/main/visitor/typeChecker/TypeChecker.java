package main.visitor.typeChecker;

import main.ast.nodes.Node;
import main.ast.nodes.Program;
import main.ast.nodes.declaration.Declaration;
import main.ast.nodes.declaration.classDec.ClassDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.ConstructorDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.FieldDeclaration;
import main.ast.nodes.declaration.classDec.classMembersDec.MethodDeclaration;
import main.ast.nodes.declaration.variableDec.VarDeclaration;
import main.ast.nodes.expression.BinaryExpression;
import main.ast.nodes.expression.Expression;
import main.ast.nodes.expression.Identifier;
import main.ast.nodes.expression.ListAccessByIndex;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.values.ListValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.statement.*;
import main.ast.nodes.statement.loop.BreakStmt;
import main.ast.nodes.statement.loop.ContinueStmt;
import main.ast.nodes.statement.loop.ForStmt;
import main.ast.nodes.statement.loop.ForeachStmt;
import main.ast.types.NoType;
import main.ast.types.NullType;
import main.ast.types.Type;
import main.ast.types.functionPointer.FptrType;
import main.ast.types.list.ListNameType;
import main.ast.types.list.ListType;
import main.ast.types.single.BoolType;
import main.ast.types.single.ClassType;
import main.ast.types.single.IntType;
import main.ast.types.single.StringType;
import main.compileErrorException.CompileErrorException;
import main.compileErrorException.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.*;
import main.symbolTable.utils.graph.Graph;
import main.symbolTable.utils.graph.exceptions.GraphDoesNotContainNodeException;
import main.visitor.Visitor;

import java.util.HashSet;
import java.util.Set;

public class TypeChecker extends Visitor<Void> 
{
    private final Graph<String> classHierarchy;
    private final ExpressionTypeChecker expressionTypeChecker;

    static String currentClass = null;
    static boolean inMethodCallSt = false;

    private boolean hasMain = false;
    private int loopCount = 0;
    private boolean isFiledDec = false;
    private boolean seenRet = false;
    private MethodDeclaration current_method;

    public TypeChecker(Graph<String> classHierarchy)
    {
        this.classHierarchy = classHierarchy;
        this.expressionTypeChecker = new ExpressionTypeChecker(classHierarchy);
    }

    @Override
    public Void visit(Program program) 
    {
        for (ClassDeclaration classDeclaration : program.getClasses()) 
        {
            currentClass = classDeclaration.getClassName().getName();

            if (currentClass.equals("Main"))
                hasMain = true;

            classDeclaration.accept(this);
        }

        if (!hasMain)
            program.addError(new NoMainClass());

        return null;
    }

    @Override
    public Void visit(ClassDeclaration classDeclaration)
    {
        SymbolTable symbolTable;
        try {
            ClassSymbolTableItem classSymbolTableItem = (ClassSymbolTableItem)
                    SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + this.currentClass, true);
            symbolTable = classSymbolTableItem.getClassSymbolTable();
        } catch (ItemNotFoundException ignored) {
            return null;
        }
        SymbolTable.push(symbolTable);
        if (currentClass.equals("Main")) 
        {
            try 
            {
                if (!classHierarchy.getParentsOfNode("Main").isEmpty()) 
                    classDeclaration.addError(new MainClassCantExtend(classDeclaration.getLine()));
            } catch (GraphDoesNotContainNodeException e) 
            {
                return null;
            }
        } else 
        {
            if (classHierarchy.isSecondNodeAncestorOf(classDeclaration.getClassName().getName(), "Main"))
                classDeclaration.addError(new CannotExtendFromMainClass(classDeclaration.getLine()));
        }

        if (classDeclaration.getParentClassName() != null) 
        {
            try 
            {
                SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + classDeclaration.getParentClassName().getName(), true);
            } catch (ItemNotFoundException e) 
            {
                classDeclaration.addError(new ClassNotDeclared(classDeclaration.getParentClassName().getLine(),
                        classDeclaration.getParentClassName().getName()));
            }
        }

        for (FieldDeclaration fieldDeclaration : classDeclaration.getFields()) 
            fieldDeclaration.accept(this);

        if (classDeclaration.getConstructor() != null) 
            classDeclaration.getConstructor().accept(this);
    
        else if (currentClass.equals("Main")) 
            classDeclaration.addError(new NoConstructorInMainClass(classDeclaration));

        for (MethodDeclaration methodDeclaration : classDeclaration.getMethods()) 
            methodDeclaration.accept(this);

        return null;
    }

    @Override
    public Void visit(ConstructorDeclaration constructorDeclaration) 
    {
        if (!constructorDeclaration.getMethodName().getName().equals(currentClass))
            constructorDeclaration.addError(new ConstructorNotSameNameAsClass(constructorDeclaration.getLine()));

        if (currentClass.equals("Main")) 
        {
            if (!constructorDeclaration.getArgs().isEmpty())
                constructorDeclaration.addError(new MainConstructorCantHaveArgs(constructorDeclaration.getLine()));
        }

        this.visit((MethodDeclaration) constructorDeclaration);
        return null;
    }

    @Override
    public Void visit(MethodDeclaration methodDeclaration) 
    {
        SymbolTable symbolTable;
        MethodSymbolTableItem methodSymbolTableItem = null;
        try {
            methodSymbolTableItem = (MethodSymbolTableItem)
                    SymbolTable.top.getItem(MethodSymbolTableItem.START_KEY + methodDeclaration.getMethodName().getName(), true);
            symbolTable = methodSymbolTableItem.getMethodSymbolTable();
        } catch (ItemNotFoundException ignored) {
            return null;
        }
        SymbolTable.push(symbolTable);
        current_method = methodDeclaration;

        VarDeclaration temp = new VarDeclaration(new Identifier("!"), methodSymbolTableItem.getReturnType());
        temp.setLine(methodDeclaration.getLine());
        temp.accept(this);
        boolean hasError = false;
        for (CompileErrorException e : temp.flushErrors()) {
            hasError = true;
            methodDeclaration.addError(e);
        }
        if (hasError)
            methodSymbolTableItem.setReturnType(new NoType());
        
        for (VarDeclaration varDeclaration : methodDeclaration.getArgs()) 
            varDeclaration.accept(this);

        for (VarDeclaration varDeclaration : methodDeclaration.getLocalVars()) 
            varDeclaration.accept(this);

        seenRet = false;
        for (Statement statement : methodDeclaration.getBody()) 
            statement.accept(this);

        if (!(methodSymbolTableItem.getReturnType() instanceof NoType
                || methodSymbolTableItem.getReturnType() instanceof NullType)) {
            if (!seenRet)
                methodDeclaration.addError(new MissingReturnStatement(methodDeclaration));
        }

        return null;
    }

    @Override
    public Void visit(FieldDeclaration fieldDeclaration) 
    {
        isFiledDec = true;
        fieldDeclaration.getVarDeclaration().accept(this);
        isFiledDec = false;
        return null;
    }

    @Override
    public Void visit(VarDeclaration varDeclaration)
    {
        Type varType = varDeclaration.getType();
        if (varType instanceof ClassType) {
            ClassType classType = (ClassType) varType;
            try {
                SymbolTable.root.getItem(
                        ClassSymbolTableItem.START_KEY + classType.getClassName().getName(), true);
            } catch (ItemNotFoundException e) {
                varDeclaration.addError(
                        new ClassNotDeclared(varDeclaration.getLine(), classType.getClassName().getName()));
                varDeclaration.setType(new NoType());

                try {
                    if (isFiledDec) {
                        SymbolTableItem tmp = SymbolTable.top.getItem(
                                FieldSymbolTableItem.START_KEY + varDeclaration.getVarName().getName(),
                                true);
                        ((FieldSymbolTableItem) tmp).setType(new NoType());
                    } else {
                        SymbolTableItem tmp = SymbolTable.top.getItem(
                                LocalVariableSymbolTableItem.START_KEY + varDeclaration.getVarName().getName(),
                                true);
                        ((LocalVariableSymbolTableItem) tmp).setType(new NoType());
                    }
                } catch (ItemNotFoundException ee) {}
            }
        }
        else if (varType instanceof ListType) {
            ListType listType = (ListType) varType;
            if (listType.getElementsTypes().isEmpty()) {
                varDeclaration.addError(new CannotHaveEmptyList(varDeclaration.getLine()));
                varDeclaration.setType(new NoType());
                try {
                    if (isFiledDec) {
                        SymbolTableItem tmp = SymbolTable.top.getItem(
                                FieldSymbolTableItem.START_KEY + varDeclaration.getVarName().getName(),
                                true);
                        ((FieldSymbolTableItem) tmp).setType(new NoType());
                    } else {
                        SymbolTableItem tmp = SymbolTable.top.getItem(
                                LocalVariableSymbolTableItem.START_KEY + varDeclaration.getVarName().getName(),
                                true);
                        ((LocalVariableSymbolTableItem) tmp).setType(new NoType());
                    }
                } catch (ItemNotFoundException ee) {}
            }
            else {
                Set<String> names = new HashSet<>();
                boolean hasDupError = false;
                boolean membersHasError = false;
                for (ListNameType member : listType.getElementsTypes()) {
                    String name = member.getName().getName();
                    if (!name.equals("")) {
                        if (names.contains(name) && !hasDupError) {
                            varDeclaration.addError(new DuplicateListId(varDeclaration.getLine()));
                            hasDupError = true;
                        }
                        else
                            names.add(name);
                    }
                    VarDeclaration temp = new VarDeclaration(new Identifier("!"), member.getType());
                    temp.setLine(varDeclaration.getLine());
                    temp.accept(this);
                    for (CompileErrorException e : temp.flushErrors())
                        varDeclaration.addError(e);
                    if (temp.getType() instanceof NoType)
                        membersHasError = true;
                }
                if (hasDupError || membersHasError) {
                    varDeclaration.setType(new NoType());
                    try {
                        if (isFiledDec) {
                            SymbolTableItem tmp = SymbolTable.top.getItem(
                                    FieldSymbolTableItem.START_KEY + varDeclaration.getVarName().getName(),
                                    true);
                            ((FieldSymbolTableItem) tmp).setType(new NoType());
                        } else {
                            SymbolTableItem tmp = SymbolTable.top.getItem(
                                    LocalVariableSymbolTableItem.START_KEY + varDeclaration.getVarName().getName(),
                                    true);
                            ((LocalVariableSymbolTableItem) tmp).setType(new NoType());
                        }
                    } catch (ItemNotFoundException ee) {}
                }
            }
        }
        else if (varType instanceof FptrType) {
            FptrType fptr = (FptrType) varType;
            boolean membersHasError = false;
            for (Type argType : fptr.getArgumentsTypes()) {
                VarDeclaration temp = new VarDeclaration(new Identifier("!"), argType);
                temp.setLine(varDeclaration.getLine());
                temp.accept(this);
                for (CompileErrorException e : temp.flushErrors())
                    varDeclaration.addError(e);
                if (temp.getType() instanceof NoType)
                    membersHasError = true;
            }

            VarDeclaration temp = new VarDeclaration(new Identifier("!"), fptr.getReturnType());
            temp.setLine(varDeclaration.getLine());
            temp.accept(this);
            for (CompileErrorException e : temp.flushErrors())
                varDeclaration.addError(e);
            if (temp.getType() instanceof NoType)
                membersHasError = true;

            if (membersHasError) {
                varDeclaration.setType(new NoType());
                try {
                    if (isFiledDec) {
                        SymbolTableItem tmp = SymbolTable.top.getItem(
                                FieldSymbolTableItem.START_KEY + varDeclaration.getVarName().getName(),
                                true);
                        ((FieldSymbolTableItem) tmp).setType(new NoType());
                    } else {
                        SymbolTableItem tmp = SymbolTable.top.getItem(
                                LocalVariableSymbolTableItem.START_KEY + varDeclaration.getVarName().getName(),
                                true);
                        ((LocalVariableSymbolTableItem) tmp).setType(new NoType());
                    }
                } catch (ItemNotFoundException ee) {}
            }
        }
        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        Type rValType = assignmentStmt.getrValue().accept(expressionTypeChecker);
        expressionTypeChecker.isLVal = true;
        Type lValType = assignmentStmt.getlValue().accept(expressionTypeChecker);

        if (!expressionTypeChecker.isLVal)
            assignmentStmt.addError(new LeftSideNotLvalue(assignmentStmt.getLine()));

        if (lValType instanceof NoType)
            return null;

        if (!expressionTypeChecker.isSubType(rValType, lValType))
            assignmentStmt.addError(new UnsupportedOperandType(assignmentStmt.getLine(),
                    BinaryOperator.assign.toString()));

        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) 
    {
        for (Statement statement : blockStmt.getStatements()) 
            statement.accept(this);
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) 
    {
        Type condition_type = conditionalStmt.getCondition().accept(expressionTypeChecker);

        if (!(condition_type instanceof NoType || condition_type instanceof BoolType))
            conditionalStmt.addError(new ConditionNotBool(conditionalStmt.getLine()));

        conditionalStmt.getThenBody().accept(this);

        if (conditionalStmt.getElseBody() != null)
            conditionalStmt.getElseBody().accept(this);

        return null;
    }

    @Override
    public Void visit(MethodCallStmt methodCallStmt) 
    {
        inMethodCallSt = true;
        methodCallStmt.getMethodCall().accept(expressionTypeChecker);
        inMethodCallSt = false;
        return null;
    }

    @Override
    public Void visit(PrintStmt print) {
        Type argType = print.getArg().accept(expressionTypeChecker);
        if (!(argType instanceof IntType || argType instanceof StringType || argType instanceof BoolType
            || argType instanceof NoType))
            {
                print.addError(new UnsupportedTypeForPrint(print.getLine()));
            }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        seenRet = true;
        Type return_type_value = returnStmt.getReturnedExpr().accept(expressionTypeChecker);
        Type method_return_type = current_method.getReturnType();

        if (!(expressionTypeChecker.isSubType(return_type_value, method_return_type)))
            returnStmt.addError(new ReturnValueNotMatchMethodReturnType(returnStmt));
        return null;
    }

    @Override
    public Void visit(BreakStmt breakStmt) {
        if (loopCount <= 0)
            breakStmt.addError(new ContinueBreakNotInLoop(breakStmt.getLine(), 0));
        return null;
    }

    @Override
    public Void visit(ContinueStmt continueStmt) {
        if (loopCount <= 0)
            continueStmt.addError(new ContinueBreakNotInLoop(continueStmt.getLine(), 1));
        return null;
    }

    @Override
    public Void visit(ForeachStmt foreachStmt) {
        loopCount++;
        Type variable = foreachStmt.getVariable().accept(expressionTypeChecker);
        Type type = foreachStmt.getList().accept(expressionTypeChecker);

        if (type instanceof NoType) {
            foreachStmt.getBody().accept(this);
            return null;
        }

        else if (!(type instanceof ListType)) {
            foreachStmt.addError(new ForeachCantIterateNoneList(foreachStmt.getLine()));
            foreachStmt.getBody().accept(this);
            return null;
        }

        ListType listType = (ListType) type;
        Type prev = null;
        Type curr = null;
        boolean isMuliType = false;
        for (ListNameType typeIterator: listType.getElementsTypes()) {
            curr = typeIterator.getType();

            if (curr instanceof NoType)
                continue;

            if (prev == null) {
                prev = curr;
                continue;
            }

            if (!(expressionTypeChecker.isSubType(prev, curr) && expressionTypeChecker.isSubType(curr, prev))) { // aren't the same
                isMuliType = true;
                break;
            }
        }

        boolean varNoType = false;
        if (variable instanceof NoType)
            varNoType = true;

        if (isMuliType) {
            foreachStmt.addError(new ForeachListElementsNotSameType(foreachStmt.getLine()));
            if (!varNoType && !(expressionTypeChecker.isSubType(variable, listType.getElementsTypes().get(0).getType())
                    && expressionTypeChecker.isSubType(listType.getElementsTypes().get(0).getType(), variable))) {
                foreachStmt.addError(new ForeachVarNotMatchList(foreachStmt));
            }
        }
        else if (!varNoType && prev != null
                && !(expressionTypeChecker.isSubType(variable, prev)
                && expressionTypeChecker.isSubType(prev, variable))) {
                foreachStmt.addError(new ForeachVarNotMatchList(foreachStmt));
            }


        foreachStmt.getBody().accept(this);

        loopCount--;
        return null;
    }

    @Override
    public Void visit(ForStmt forStmt) {
        loopCount++;

        if (forStmt.getInitialize() != null)
            forStmt.getInitialize().accept(this);

        Type loopCondition = new NoType();
        if (forStmt.getCondition() != null)
            loopCondition = forStmt.getCondition().accept(expressionTypeChecker);

        if (!(loopCondition instanceof NoType || loopCondition instanceof BoolType))
            forStmt.addError(new ConditionNotBool(forStmt.getLine()));

        if (forStmt.getUpdate() != null)
            forStmt.getUpdate().accept(this);


        forStmt.getBody().accept(this);

        loopCount--;
        return null;
    }

}
