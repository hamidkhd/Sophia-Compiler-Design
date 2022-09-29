package main.visitor.typeChecker;

import com.sun.jdi.event.StepEvent;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.values.ListValue;
import main.ast.nodes.expression.values.NullValue;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.expression.values.primitive.StringValue;
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
import main.compileErrorException.typeErrors.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.*;
import main.symbolTable.utils.graph.Graph;
import main.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;


public class ExpressionTypeChecker extends Visitor<Type> {
    private final Graph<String> classHierarchy;
    public boolean isLVal = true;

    public ExpressionTypeChecker(Graph<String> classHierarchy) {
        this.classHierarchy = classHierarchy;
    }

    public boolean isSubType(Type first, Type second) {
        if (first instanceof NoType)
            return true;
        if (second instanceof NoType)
            return false;

        if (first instanceof BoolType && second instanceof BoolType)
            return true;

        if (first instanceof IntType && second instanceof IntType)
            return true;

        if (first instanceof StringType && second instanceof StringType)
            return true;

        if (first instanceof NullType && second instanceof NullType)
            return true;

        if (first instanceof NullType && (second instanceof ClassType || second instanceof FptrType))
            return true;

        if (first instanceof ClassType && second instanceof ClassType) {
            String name1 = ((ClassType) first).getClassName().getName();
            String name2 = ((ClassType) second).getClassName().getName();
            if (name1.equals(name2))
                return true;
            return classHierarchy.isSecondNodeAncestorOf(name1, name2);
        }

        if (first instanceof FptrType && second instanceof FptrType) {
            FptrType f1 = (FptrType) first;
            FptrType f2 = (FptrType) second;
            ArrayList<Type> t1 = f1.getArgumentsTypes();
            ArrayList<Type> t2 = f2.getArgumentsTypes();

            if (!isSubType(f1.getReturnType(), f2.getReturnType()))
                return false;
            if (t1.size() != t2.size())
                return false;
            for (int i = 0; i < t1.size(); i++) {
                if (!isSubType(t2.get(i), t1.get(i)))
                    return false;
            }
            return true;
        }

        if (first instanceof ListType && second instanceof ListType) {
            ListType l1 = (ListType) first;
            ListType l2 = (ListType) second;
            ArrayList<ListNameType> t1 = l1.getElementsTypes();
            ArrayList<ListNameType> t2 = l2.getElementsTypes();

            if (t1.size() != t2.size())
                return false;
            for (int i = 0; i < t1.size(); i++) {
                if (!isSubType(t1.get(i).getType(), t2.get(i).getType()))
                    return false;
            }
            return true;
        }

        return false;
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        this.isLVal = true; // for checking left operand
        Type first = binaryExpression.getFirstOperand().accept(this);
        boolean isFirstLVal = this.isLVal;
        this.isLVal = false;
        Type second = binaryExpression.getSecondOperand().accept(this);
        switch (binaryExpression.getBinaryOperator()) {
            case add:
            case sub:
            case mult:
            case div:
            case mod:
                if (first instanceof IntType && second instanceof IntType)
                    return new IntType();

                else if ((first instanceof NoType || first instanceof IntType)
                        && (second instanceof NoType || second instanceof IntType)) {
                    return new NoType();
                }
                else {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),
                            binaryExpression.getBinaryOperator().toString()));
                    return new NoType();
                }
            case lt:
            case gt:
                if (first instanceof IntType && second instanceof IntType)
                    return new BoolType();

                else if ((first instanceof NoType || first instanceof IntType)
                        && (second instanceof NoType || second instanceof IntType)) {
                    return new NoType();
                }
                else {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),
                            binaryExpression.getBinaryOperator().toString()));
                    return new NoType();
                }
            case or:
            case and:
                if (first instanceof BoolType && second instanceof BoolType)
                    return new BoolType();

                else if ((first instanceof NoType || first instanceof BoolType)
                        && (second instanceof NoType || second instanceof BoolType)) {
                    return new NoType();
                } else {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),
                            binaryExpression.getBinaryOperator().toString()));
                    return new NoType();
                }
            case eq:
            case neq:
                if (first instanceof ListType || second instanceof ListType) { // Lists don't have eq and neq
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),
                            binaryExpression.getBinaryOperator().toString()));
                    return new NoType();
                }
                else if (first instanceof NoType || second instanceof NoType)
                    return new NoType();

                else if (isSubType(first, second) && isSubType(second, first))
                    return new BoolType();
                else if(((first instanceof ClassType || first instanceof FptrType) && second instanceof NullType)
                    || ((second instanceof ClassType || second instanceof FptrType) && first instanceof NullType)) {
                    return new BoolType();
                }
                else {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),
                            binaryExpression.getBinaryOperator().toString()));
                    return new NoType();
                }
            case assign:
                boolean hasError = false;
                if (!isFirstLVal) {
                    binaryExpression.addError(new LeftSideNotLvalue(binaryExpression.getLine()));
                    hasError = true;
                }
                if (first instanceof NoType || second instanceof NoType)
                    return new NoType();
                else if (!isSubType(second, first)) {
                    binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),
                            BinaryOperator.assign.toString()));
                    return new NoType();
                }
                if (hasError)
                    return new NoType();
                else
                    return second;

        }
        return null;
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        this.isLVal = true; // for checking lvalue
        Type expType = unaryExpression.getOperand().accept(this);
        boolean isFirstLVal = this.isLVal;
        this.isLVal = false;
        switch (unaryExpression.getOperator()) {
            case predec:
            case postdec:
            case postinc:
            case preinc:
                if (!isFirstLVal) {
                    unaryExpression.addError(new IncDecOperandNotLvalue(unaryExpression.getLine(),
                            unaryExpression.getOperator().toString()));
                }
                if (expType instanceof NoType)
                    return new NoType();

                else if (expType instanceof IntType) {
                    if (isFirstLVal)
                        return new IntType();
                    else
                        return new NoType();
                }
                else {
                    unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(),
                            unaryExpression.getOperator().toString()));
                    return new NoType();
                }

            case minus:
                if (expType instanceof NoType)
                    return new NoType();

                else if (expType instanceof IntType)
                    return new IntType();
                else {
                    unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(),
                            unaryExpression.getOperator().toString()));
                    return new NoType();
                }
            case not:
                if (expType instanceof NoType)
                    return new NoType();
                else if (expType instanceof BoolType)
                    return new BoolType();
                else {
                    unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(),
                            unaryExpression.getOperator().toString()));
                    return new NoType();
                }
        }
        return null;
    }

    @Override
    public Type visit(ObjectOrListMemberAccess objectOrListMemberAccess) {
        boolean wasLVal = this.isLVal;
        Type instanceType = objectOrListMemberAccess.getInstance().accept(this);
        if (wasLVal && objectOrListMemberAccess.getInstance() instanceof  ThisClass)
            this.isLVal = true;
        Identifier id = objectOrListMemberAccess.getMemberName();
        if (instanceType instanceof NoType)
            return new NoType();

        else if (instanceType instanceof ClassType) {
            ClassType classType = (ClassType) instanceType;
            ClassSymbolTableItem ctb = null;
            try {
                ctb = (ClassSymbolTableItem) SymbolTable.top.getItem(
                        ClassSymbolTableItem.START_KEY + classType.getClassName().getName(),
                        true);
            } catch (ItemNotFoundException e) {
                objectOrListMemberAccess.addError(new ClassNotDeclared(objectOrListMemberAccess.getLine(),
                        classType.getClassName().getName()));
                return new NoType();
            }

            try {
                SymbolTableItem tmp = ctb.getClassSymbolTable().getItem(FieldSymbolTableItem.START_KEY + id.getName(), true);
                return ((FieldSymbolTableItem)tmp).getType();
            } catch (ItemNotFoundException e) {
                try {
                    SymbolTableItem tmp = ctb.getClassSymbolTable().getItem(MethodSymbolTableItem.START_KEY + id.getName(), true);
                    MethodSymbolTableItem mtb = (MethodSymbolTableItem)tmp;
                    this.isLVal = false;
                    return new FptrType(mtb.getArgTypes(), mtb.getReturnType());
                } catch (ItemNotFoundException notFoundException) {
                    if (id.getName().equals(classType.getClassName().getName())) { // constructor when no constructor
                        return new FptrType(new ArrayList<>(), new NullType());
                    }
                    objectOrListMemberAccess.addError(new MemberNotAvailableInClass(
                            objectOrListMemberAccess.getLine(), id.getName(),
                            ((ClassType) instanceType).getClassName().getName()));
                    return new NoType();
                }
            }
        }

        else if (instanceType instanceof ListType) {
            ListType listType = (ListType) instanceType;
            ArrayList<ListNameType> listTypes = listType.getElementsTypes();
            for (ListNameType type : listTypes) {
                if (type.getName().getName().equals(id.getName()))
                    return type.getType();
            }
            objectOrListMemberAccess.addError(new ListMemberNotFound(objectOrListMemberAccess.getLine(), id.getName()));
            return new NoType();
        }


        else {
            objectOrListMemberAccess.addError(new MemberAccessOnNoneObjOrListType(objectOrListMemberAccess.getLine()));
            return new NoType();
        }
    }

    @Override
    public Type visit(Identifier identifier) {
        String name = identifier.getName();
        SymbolTableItem tmp;
        try {
            tmp = SymbolTable.top.getItem(LocalVariableSymbolTableItem.START_KEY + name, true);
            return ((LocalVariableSymbolTableItem)tmp).getType();
        } catch (ItemNotFoundException itemNotFoundException) {
            identifier.addError(new VarNotDeclared(identifier.getLine(), identifier.getName()));
            return new NoType();
        }
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        Type instanceType = listAccessByIndex.getInstance().accept(this);
        boolean tempLVal = this.isLVal;
        Type indexType = listAccessByIndex.getIndex().accept(this);
        this.isLVal = tempLVal;
        boolean shouldReturnNoType = false;

        if (!(indexType instanceof IntType || indexType instanceof NoType)) {
            listAccessByIndex.addError(new ListIndexNotInt(listAccessByIndex.getLine()));
            shouldReturnNoType = true;
        }

        if (instanceType instanceof NoType)
            return new NoType();

        if (!(instanceType instanceof ListType)) {
            listAccessByIndex.addError(new ListAccessByIndexOnNoneList(listAccessByIndex.getLine()));
            return new NoType();
        }

        ListType listType = (ListType) instanceType;
        ArrayList<ListNameType> types = listType.getElementsTypes();
        Type prev = null;
        Type curr = null;
        boolean isMuliType = false;
        for (ListNameType type: types) {
            curr = type.getType();

            if (curr instanceof NoType)
                continue;

            if (prev == null) {
                prev = curr;
                continue;
            }

            if (!(isSubType(prev, curr) && isSubType(curr, prev))) { // aren't the same
                isMuliType = true;
                break;
            }
        }
        if (isMuliType && !(listAccessByIndex.getIndex() instanceof IntValue)) {
            listAccessByIndex.addError(new CantUseExprAsIndexOfMultiTypeList(listAccessByIndex.getLine()));
            return new NoType();
        }
        else if (shouldReturnNoType)
            return new NoType();
        else if (listAccessByIndex.getIndex() instanceof IntValue) {
            int index = ((IntValue) listAccessByIndex.getIndex()).getConstant();
            if (index > types.size())
                return types.get(0).getType();
            else
                return types.get(index).getType();
        }
        else
            return types.get(0).getType();

    }

    @Override
    public Type visit(MethodCall methodCall) {
        this.isLVal = false;
        Type instanceType = methodCall.getInstance().accept(this);
        boolean hasError = false;
        if (instanceType instanceof FptrType) {
            FptrType fptr = (FptrType) instanceType;
            if (fptr.getReturnType() instanceof NullType && !TypeChecker.inMethodCallSt) {
                methodCall.addError(new CantUseValueOfVoidMethod(methodCall.getLine()));
                hasError = true;
            }
            ArrayList<Type> fptrArgs = fptr.getArgumentsTypes();
            ArrayList<Expression> callArgs = methodCall.getArgs();
            ArrayList<Type> callTypes = new ArrayList<>();

            for (Expression exp : callArgs)
                callTypes.add(exp.accept(this));

            if (fptrArgs.size() != callArgs.size()) {
                methodCall.addError(new MethodCallNotMatchDefinition(methodCall.getLine()));
                return new NoType();
            }
            for (int i = 0; i < fptrArgs.size(); i++) {
                if (!isSubType(callTypes.get(i), fptrArgs.get(i))) {
                    methodCall.addError(new MethodCallNotMatchDefinition(methodCall.getLine()));
                    return new NoType();
                }
            }

            if (hasError)
                return new NoType();
            return fptr.getReturnType();
        }
        else if (instanceType instanceof NoType)
            return new NoType();
        else {
            methodCall.addError(new CallOnNoneFptrType(methodCall.getLine()));
            return new NoType();
        }
    }

    @Override
    public Type visit(NewClassInstance newClassInstance) {
        this.isLVal = false;
        String className = newClassInstance.getClassType().getClassName().getName();
        ClassSymbolTableItem ctb = null;
        try {
            SymbolTableItem tmp = SymbolTable.root.getItem(ClassSymbolTableItem.START_KEY + className, true);
            ctb = (ClassSymbolTableItem) tmp;
        } catch (ItemNotFoundException foundException) {
            newClassInstance.addError(new ClassNotDeclared(newClassInstance.getLine(), className));
            return new NoType();
        }

        try {
            SymbolTableItem tmp = ctb.getClassSymbolTable().getItem(
                    MethodSymbolTableItem.START_KEY + className, true);
            MethodSymbolTableItem mtb = (MethodSymbolTableItem)tmp;
            ArrayList<Type> conArgs = mtb.getArgTypes();
            ArrayList<Expression> callArgs = newClassInstance.getArgs();
            ArrayList<Type> callTypes = new ArrayList<>();

            for (Expression exp : callArgs)
                callTypes.add(exp.accept(this));

            if (conArgs.size() != callArgs.size()) {
                newClassInstance.addError(new ConstructorArgsNotMatchDefinition(newClassInstance));
                return new NoType();
            }
            for (int i = 0; i < conArgs.size(); i++) {
                if (!isSubType(callTypes.get(i), conArgs.get(i))) {
                    newClassInstance.addError(new ConstructorArgsNotMatchDefinition(newClassInstance));
                    return new NoType();
                }
            }

        } catch (ItemNotFoundException e) {
            ArrayList<Expression> callArgs = newClassInstance.getArgs();
            for (Expression exp : callArgs)
                exp.accept(this);
            if (!newClassInstance.getArgs().isEmpty()) {
                newClassInstance.addError(new ConstructorArgsNotMatchDefinition(newClassInstance));
                return new NoType();
            }
        }

        return newClassInstance.getClassType();
    }

    @Override
    public Type visit(ThisClass thisClass) {
        this.isLVal = false;
        return new ClassType(new Identifier(TypeChecker.currentClass));
    }

    @Override
    public Type visit(ListValue listValue) {
        this.isLVal = false;
        ArrayList<ListNameType> members = new ArrayList<>();
        for (Expression exp : listValue.getElements()) {
            Type expType = exp.accept(this);
            members.add(new ListNameType(expType));
        }
        return new ListType(members);
    }

    @Override
    public Type visit(NullValue nullValue) {
        this.isLVal = false;
        return new NullType();
    }

    @Override
    public Type visit(IntValue intValue) {
        this.isLVal = false;
        return new IntType();
    }

    @Override
    public Type visit(BoolValue boolValue) {
        this.isLVal = false;
        return new BoolType();
    }

    @Override
    public Type visit(StringValue stringValue) {
        this.isLVal = false;
        return new StringType();
    }
}
