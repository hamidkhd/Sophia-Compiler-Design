grammar Sophia;

@header{
    import main.ast.types.*;
    import main.ast.types.functionPointer.*;
    import main.ast.types.list.*;
    import main.ast.types.single.*;
    import main.ast.nodes.*;
    import main.ast.nodes.declaration.*;
    import main.ast.nodes.declaration.classDec.*;
    import main.ast.nodes.declaration.classDec.classMembersDec.*;
    import main.ast.nodes.declaration.variableDec.*;
    import main.ast.nodes.expression.*;
    import main.ast.nodes.expression.operators.*;
    import main.ast.nodes.expression.values.*;
    import main.ast.nodes.expression.values.primitive.*;
    import main.ast.nodes.statement.*;
    import main.ast.nodes.statement.loop.*;
}

sophia returns[Program sophiaProgram]:
    p=program
    {$sophiaProgram = $p.programRet;}
    EOF;

program returns[Program programRet]:
    {
        $programRet = new Program();
        $programRet.setLine(1);
    }
    (
    c=sophiaClass
    {$programRet.addClass($c.sophiaClassRet);}
    )*;

sophiaClass returns[ClassDeclaration sophiaClassRet] locals[FieldDeclaration tempFieldDec]:
    CLASS name=identifier
    {
        $sophiaClassRet = new ClassDeclaration($name.idRet);
        $sophiaClassRet.setLine($CLASS.getLine());
    }
    (EXTENDS parent_name=identifier {$sophiaClassRet.setParentClassName($parent_name.idRet);})?
    LBRACE ((
    (v1=varDeclaration {$tempFieldDec = new FieldDeclaration($v1.varDecRet); $tempFieldDec.setLine($v1.varDecRet.getLine()); $sophiaClassRet.addField($tempFieldDec);}
        | m1=method {$sophiaClassRet.addMethod($m1.methodRet);})*
    constructor {$sophiaClassRet.setConstructor($constructor.constructorRet);}
    (v2=varDeclaration {$tempFieldDec = new FieldDeclaration($v2.varDecRet); $tempFieldDec.setLine($v2.varDecRet.getLine()); $sophiaClassRet.addField($tempFieldDec);}
        | m2=method {$sophiaClassRet.addMethod($m2.methodRet);})*)
    | ((v3=varDeclaration {$tempFieldDec = new FieldDeclaration($v3.varDecRet); $tempFieldDec.setLine($v3.varDecRet.getLine()); $sophiaClassRet.addField($tempFieldDec);}
        | m3=method {$sophiaClassRet.addMethod($m3.methodRet);})*))
    RBRACE;

varDeclaration returns[VarDeclaration varDecRet]:
    identifier COLON type SEMICOLLON
    {
        $varDecRet = new VarDeclaration($identifier.idRet, $type.typeRet);
        $varDecRet.setLine($identifier.idRet.getLine());
    }
    ;

method returns[MethodDeclaration methodRet] locals[Type retType]:
    DEF (type {$retType = $type.typeRet;}| VOID {$retType = new NullType();}) name=identifier
    {
        $methodRet = new MethodDeclaration($name.idRet, $retType);
        $methodRet.setLine($DEF.getLine());
    }
    LPAR methodArguments RPAR {$methodRet.setArgs($methodArguments.methodArgsRet);}
    LBRACE methodBody RBRACE
    {
        $methodRet.setLocalVars($methodBody.localVars);
        $methodRet.setBody($methodBody.statements);
    }
    ;

constructor returns[ConstructorDeclaration constructorRet]:
    DEF name=identifier
    {
        $constructorRet = new ConstructorDeclaration($name.idRet);
        $constructorRet.setLine($DEF.getLine());
    }
    LPAR methodArguments RPAR {$constructorRet.setArgs($methodArguments.methodArgsRet);}
    LBRACE methodBody RBRACE
    {
            $constructorRet.setLocalVars($methodBody.localVars);
            $constructorRet.setBody($methodBody.statements);
        }
    ;

methodArguments returns[ArrayList<VarDeclaration> methodArgsRet]:
    {$methodArgsRet = new ArrayList<>();}
    (a=variableWithType {$methodArgsRet.add($a.varDecRet);}
    (COMMA b=variableWithType {$methodArgsRet.add($b.varDecRet);})*)?;

variableWithType returns[VarDeclaration varDecRet]:
    identifier COLON type
    {
        $varDecRet = new VarDeclaration($identifier.idRet, $type.typeRet);
        $varDecRet.setLine($identifier.idRet.getLine());
    }
    ;

type returns[Type typeRet]:
    primitiveDataType {$typeRet = $primitiveDataType.primTypeRet;}
    | listType {$typeRet = $listType.listTypeRet;}
    | functionPointerType {$typeRet = $functionPointerType.fptrRet;}
    | classType {$typeRet = $classType.classTypeRet;}
    ;
classType returns[ClassType classTypeRet]:
    identifier {$classTypeRet = new ClassType($identifier.idRet);}
    ;

listType returns[ListType listTypeRet]:
    LIST LPAR ((size=INT_VALUE SHARP type) {$listTypeRet = new ListType($size.int, new ListNameType($type.typeRet));}
    | (listItemsTypes {$listTypeRet = new ListType($listItemsTypes.listItmesTypesRet);})) RPAR
    ;

listItemsTypes returns[ArrayList<ListNameType> listItmesTypesRet]:
    {$listItmesTypesRet = new ArrayList<>();}
    a=listItemType {$listItmesTypesRet.add($a.listItemTypeRet);}
    (COMMA b=listItemType {$listItmesTypesRet.add($b.listItemTypeRet);})*
    ;

listItemType returns[ListNameType listItemTypeRet]:
    variableWithType {$listItemTypeRet = new ListNameType($variableWithType.varDecRet);}
    | type {$listItemTypeRet = new ListNameType($type.typeRet);}
    ;

functionPointerType returns[FptrType fptrRet] locals[Type retType, ArrayList<Type> args]:
    FUNC LESS_THAN (VOID {$args = new ArrayList<>();} | typesWithComma {$args = $typesWithComma.typesWithCommaRet;})
    ARROW (VOID {$retType = new NullType();} | type {$retType = $type.typeRet;})
    GREATER_THAN
    {$fptrRet = new FptrType($args, $retType);}
    ;

typesWithComma returns[ArrayList<Type> typesWithCommaRet]:
    {$typesWithCommaRet = new ArrayList<>();}
    a=type {$typesWithCommaRet.add($a.typeRet);}
    (COMMA b=type {$typesWithCommaRet.add($b.typeRet);})*
    ;

primitiveDataType returns[Type primTypeRet]:
    INT {$primTypeRet = new IntType();}
    | STRING {$primTypeRet = new StringType();}
    | BOOLEAN {$primTypeRet = new BoolType();};

methodBody returns[ArrayList<VarDeclaration> localVars, ArrayList<Statement> statements]:
    {
        $localVars = new ArrayList<>();
        $statements = new ArrayList<>();
    }
    (varDeclaration {$localVars.add($varDeclaration.varDecRet);})*
    (statement {$statements.add($statement.statementRet);})*
    ;

statement returns[Statement statementRet]:
    forStatement {$statementRet = $forStatement.forStmtRet;}
    | foreachStatement {$statementRet = $foreachStatement.foreachRet;}
    | ifStatement {$statementRet = $ifStatement.ifRet;}
    | assignmentStatement {$statementRet = $assignmentStatement.assignStmtRet;}
    | printStatement {$statementRet = $printStatement.printRet;}
    | continueBreakStatement {$statementRet = $continueBreakStatement.cbRet;}
    | methodCallStatement {$statementRet = $methodCallStatement.methodCallStmtRet;}
    | returnStatement {$statementRet = $returnStatement.returnRet;}
    | block {$statementRet = $block.blockRet;}
    ;

block returns[BlockStmt blockRet]:
    LBRACE {$blockRet = new BlockStmt(); $blockRet.setLine($LBRACE.getLine());}
    (statement {$blockRet.addStatement($statement.statementRet);})*
    RBRACE
    ;

assignmentStatement returns[AssignmentStmt assignStmtRet]:
    assignment {$assignStmtRet = $assignment.assignmentRet;} SEMICOLLON;

assignment returns[AssignmentStmt assignmentRet]:
    lvalue=orExpression ASSIGN rvalue=expression
    {
        $assignmentRet = new AssignmentStmt($lvalue.orRet, $rvalue.expressionRet);
        $assignmentRet.setLine($ASSIGN.getLine());
    }
    ;

printStatement returns[PrintStmt printRet]:
    PRINT LPAR expression RPAR SEMICOLLON
    {
        $printRet = new PrintStmt($expression.expressionRet);
        $printRet.setLine($PRINT.getLine());
    }
    ;

returnStatement returns[ReturnStmt returnRet] locals[Expression exp]:
    RETURN
        {
            $exp = new NullValue();
            $exp.setLine($RETURN.getLine());
            $returnRet = new ReturnStmt();
            $returnRet.setLine($RETURN.getLine());
        }
    (expression {$exp = $expression.expressionRet;})?
    SEMICOLLON {$returnRet.setReturnedExpr($exp);}
    ;

methodCallStatement returns[MethodCallStmt methodCallStmtRet]:
    methodCall SEMICOLLON
    {
        $methodCallStmtRet = new MethodCallStmt($methodCall.methodCallRet);
        $methodCallStmtRet.setLine($methodCall.methodCallRet.getLine());
    }
    ;

methodCall returns[MethodCall methodCallRet] locals[Expression instance]:
    e1=otherExpression {$instance = $e1.otherRet;}
    (
        (
            lpar1=LPAR args1=methodCallArguments RPAR
            {
                $instance = new MethodCall($instance, $args1.methodCallArgsRet);
                $instance.setLine($lpar1.getLine());
            }
        )
    | (
        DOT identifier
        {
            $instance = new ObjectOrListMemberAccess($instance, $identifier.idRet);
            $instance.setLine($identifier.idRet.getLine());
        }
      )
    | (
        LBRACK e2=expression RBRACK
        {
            $instance = new ListAccessByIndex($instance, $e2.expressionRet);
            $instance.setLine($LBRACK.getLine());
        }
      )
    )*
    (
        lpar2=LPAR args2=methodCallArguments RPAR
        {
            $methodCallRet = new MethodCall($instance, $args2.methodCallArgsRet);
            $methodCallRet.setLine($lpar2.getLine());
        }
    )
    ;

methodCallArguments returns[ArrayList<Expression> methodCallArgsRet]:
    {$methodCallArgsRet = new ArrayList<>();}
    (
    e1=expression {$methodCallArgsRet.add($e1.expressionRet);}
    (COMMA e2=expression {$methodCallArgsRet.add($e2.expressionRet);})*
    )?;

continueBreakStatement returns[Statement cbRet]:
    (
        BREAK {$cbRet = new BreakStmt(); $cbRet.setLine($BREAK.getLine());}
        | CONTINUE {$cbRet = new ContinueStmt(); $cbRet.setLine($CONTINUE.getLine());}
    ) SEMICOLLON;

forStatement returns[ForStmt forStmtRet]:
    FOR LPAR {$forStmtRet = new ForStmt(); $forStmtRet.setLine($FOR.getLine());}
    (init=assignment {$forStmtRet.setInitialize($init.assignmentRet);})? SEMICOLLON
    (condition=expression {$forStmtRet.setCondition($condition.expressionRet);})? SEMICOLLON
    (update=assignment {$forStmtRet.setUpdate($update.assignmentRet);})? RPAR
    statement {$forStmtRet.setBody($statement.statementRet);};

foreachStatement returns[ForeachStmt foreachRet]:
    FOREACH LPAR identifier IN expression RPAR
    {
        $foreachRet = new ForeachStmt($identifier.idRet, $expression.expressionRet);
        $foreachRet.setLine($FOREACH.getLine());
    }
    statement {$foreachRet.setBody($statement.statementRet);}
    ;

ifStatement returns[ConditionalStmt ifRet]:
    IF LPAR exp=expression RPAR then=statement
    {
        $ifRet = new ConditionalStmt($exp.expressionRet, $then.statementRet);
        $ifRet.setLine($IF.getLine());
    }
    (ELSE elseSt=statement {$ifRet.setElseBody($elseSt.statementRet);})?;

expression returns[Expression expressionRet]:
    first=orExpression {$expressionRet = $first.orRet;}
    ( ASSIGN
        second=expression {$expressionRet = new BinaryExpression($expressionRet, $second.expressionRet, BinaryOperator.assign);} {$expressionRet.setLine($ASSIGN.getLine());}
    )?
    ;

orExpression returns[Expression orRet]:
    first=andExpression {$orRet = $first.andRet;}
    ( OR
        second=andExpression  {$orRet = new BinaryExpression($orRet, $second.andRet, BinaryOperator.or);} {$orRet.setLine($OR.getLine());}
    )*
    ;

andExpression returns[Expression andRet]:
    first=equalityExpression {$andRet = $first.equalRet;}
    ( AND
        second=equalityExpression {$andRet = new BinaryExpression($andRet, $second.equalRet, BinaryOperator.and);} {$andRet.setLine($AND.getLine());}
    )*
    ;

equalityExpression returns[Expression equalRet] locals[BinaryOperator op, int line]:
    first=relationalExpression {$equalRet = $first.relationalRet;}
    (
        (
            EQUAL {$op = BinaryOperator.eq;} {$line = $EQUAL.getLine();}
            | NOT_EQUAL {$op = BinaryOperator.neq;} {$line = $NOT_EQUAL.getLine();}
        )
        second=relationalExpression {$equalRet = new BinaryExpression($equalRet, $second.relationalRet, $op);} {$equalRet.setLine($line);}
    )*
    ;

relationalExpression returns[Expression relationalRet] locals[BinaryOperator op, int line]:
    first=additiveExpression {$relationalRet = $first.additiveRet;}
    (
        (
            GREATER_THAN {$op = BinaryOperator.gt;} {$line = $GREATER_THAN.getLine();}
            | LESS_THAN  {$op = BinaryOperator.lt;} {$line = $LESS_THAN.getLine();}
        )
        second=additiveExpression {$relationalRet = new BinaryExpression($relationalRet, $second.additiveRet, $op);} {$relationalRet.setLine($line);}
    )*
    ;

additiveExpression returns[Expression additiveRet] locals[BinaryOperator op, int line]:
    first=multiplicativeExpression {$additiveRet = $first.multiplicativeRet;}
    (
        (
            PLUS {$op = BinaryOperator.add;} {$line = $PLUS.getLine();}
            | MINUS {$op = BinaryOperator.sub;} {$line = $MINUS.getLine();}
        )
        second=multiplicativeExpression {$additiveRet = new BinaryExpression($additiveRet, $second.multiplicativeRet, $op);} {$additiveRet.setLine($line);}
    )*
    ;

multiplicativeExpression returns[Expression multiplicativeRet] locals[BinaryOperator op, int line]:
    first=preUnaryExpression {$multiplicativeRet = $first.preUnaryRet;}
    (
        (
            MULT {$op = BinaryOperator.mult;} {$line = $MULT.getLine();}
            | DIVIDE {$op = BinaryOperator.div;} {$line = $DIVIDE.getLine();}
            | MOD {$op = BinaryOperator.mod;} {$line = $MOD.getLine();}
        )
        second=preUnaryExpression {$multiplicativeRet = new BinaryExpression($multiplicativeRet, $second.preUnaryRet, $op);} {$multiplicativeRet.setLine($line);}
    )*
    ;

preUnaryExpression returns[Expression preUnaryRet] locals[UnaryOperator op, int line]:
    (
        (
            NOT {$op = UnaryOperator.not;} {$line = $NOT.getLine();}
            | MINUS {$op = UnaryOperator.minus;} {$line = $MINUS.getLine();}
            | INCREMENT {$op = UnaryOperator.preinc;} {$line = $INCREMENT.getLine();}
            | DECREMENT {$op = UnaryOperator.predec;} {$line = $DECREMENT.getLine();}
        )
        preUnaryExpression {$preUnaryRet = new UnaryExpression($preUnaryExpression.preUnaryRet, $op);} {$preUnaryRet.setLine($line);}
    )
    | postUnaryExpression {$preUnaryRet = $postUnaryExpression.postUnaryRet;}
    ;

postUnaryExpression returns[Expression postUnaryRet]:
    accessExpression {$postUnaryRet = $accessExpression.accessRet;}
    (
        INCREMENT {$postUnaryRet = new UnaryExpression($postUnaryRet, UnaryOperator.postinc);}  {$postUnaryRet.setLine($INCREMENT.getLine());}
        | DECREMENT {$postUnaryRet = new UnaryExpression($postUnaryRet, UnaryOperator.postdec);}  {$postUnaryRet.setLine($DECREMENT.getLine());}
    )?
    ;

accessExpression returns[Expression accessRet]:
    otherExpression {$accessRet = $otherExpression.otherRet;}
    (
        (
            LPAR methodCallArguments RPAR
            {
                $accessRet = new MethodCall($accessRet, $methodCallArguments.methodCallArgsRet);
                $accessRet.setLine($LPAR.getLine());
            }
        )
        | (
            DOT identifier
            {
                $accessRet = new ObjectOrListMemberAccess($accessRet, $identifier.idRet);
                $accessRet.setLine($identifier.idRet.getLine());
            }
        )
        | (
            LBRACK expression RBRACK
            {
                $accessRet = new ListAccessByIndex($accessRet, $expression.expressionRet);
                $accessRet.setLine($LBRACK.getLine());
            }
        )
    )*
    ;

otherExpression returns[Expression otherRet]:
    THIS {$otherRet = new ThisClass();} {$otherRet.setLine($THIS.getLine());}
    | newExpression {$otherRet = $newExpression.newRet;}
    | values {$otherRet = $values.valuesRet;}
    | identifier {$otherRet = $identifier.idRet;}
    | LPAR (expression {$otherRet = $expression.expressionRet;}) RPAR
    ;

newExpression returns[Expression newRet]:
    NEW classType LPAR methodCallArguments RPAR
    {$newRet = new NewClassInstance($classType.classTypeRet, $methodCallArguments.methodCallArgsRet);}
    {$newRet.setLine($NEW.getLine());}
    ;

values returns[Value valuesRet]:
    boolValue {$valuesRet = $boolValue.boolValueRet;}
    | STRING_VALUE {$valuesRet = new StringValue($STRING_VALUE.text.replace("\"", ""));} {$valuesRet.setLine($STRING_VALUE.getLine());}
    | INT_VALUE {$valuesRet = new IntValue($INT_VALUE.int);}  {$valuesRet.setLine($INT_VALUE.getLine());}
    | NULL {$valuesRet = new NullValue();} {$valuesRet.setLine($NULL.getLine());}
    | listValue {$valuesRet = $listValue.listValueRet;}
    ;

boolValue returns[BoolValue boolValueRet]:
    TRUE {$boolValueRet = new BoolValue(true);} {$boolValueRet.setLine($TRUE.getLine());}
    | FALSE {$boolValueRet = new BoolValue(false);} {$boolValueRet.setLine($FALSE.getLine());}
    ;

listValue returns[ListValue listValueRet]:
    LBRACK methodCallArguments RBRACK {$listValueRet = new ListValue($methodCallArguments.methodCallArgsRet);}
    {$listValueRet.setLine($LBRACK.getLine());}
    ;

identifier returns[Identifier idRet]:
    id=IDENTIFIER
    {
        $idRet = new Identifier($id.getText());
        $idRet.setLine($id.getLine());
    };


DEF: 'def';
EXTENDS: 'extends';
CLASS: 'class';

PRINT: 'print';
FUNC: 'func';

NEW: 'new';

CONTINUE: 'continue';
BREAK: 'break';
RETURN: 'return';

FOREACH: 'foreach';
IN: 'in';
FOR: 'for';
IF: 'if';
ELSE: 'else';

BOOLEAN: 'bool';
STRING: 'string';
INT: 'int';
VOID: 'void';
NULL: 'null';
LIST: 'list';

TRUE: 'true';
FALSE: 'false';

THIS: 'this';

ARROW: '->';
GREATER_THAN: '>';
LESS_THAN: '<';
NOT_EQUAL: '!=';
EQUAL: '==';

MULT: '*';
DIVIDE: '/';
MOD: '%';
PLUS: '+';
MINUS: '-';
AND: '&&';
OR: '||';
NOT: '!';
QUESTION_MARK: '?';

ASSIGN: '=';

INCREMENT: '++';
DECREMENT: '--';

LPAR: '(';
RPAR: ')';
LBRACK: '[';
RBRACK: ']';
LBRACE: '{';
RBRACE: '}';

SHARP: '#';
COMMA: ',';
DOT: '.';
COLON: ':';
SEMICOLLON: ';';

INT_VALUE: '0' | [1-9][0-9]*;
IDENTIFIER: [a-zA-Z_][A-Za-z0-9_]*;
STRING_VALUE: '"'~["]*'"';
COMMENT: ('//' ~( '\r' | '\n')*) -> skip;
WS: ([ \t\n\r]) -> skip;