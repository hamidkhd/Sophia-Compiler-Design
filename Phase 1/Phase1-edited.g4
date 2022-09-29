grammar sophia;

globalScope
    : classDec* EOF
    ;


classDec
    : CLASS class_name=IDENTIFIER (EXTENDS parent=IDENTIFIER)?
    {
        System.out.print("ClassDec:"+$class_name.text);
        if($parent.text != null) System.out.print("," + $parent.text);
        System.out.println("");
    } 
        classScope
    ;

classScope
    : LBRACE classProperty* constructor? classProperty* RBRACE
    ;

functionScope
    : LBRACE varDecStatement* statement* RBRACE
    ;

scope
    : LBRACE statement* RBRACE
    ;

constructor
    : DEF cons_name=IDENTIFIER 
        {System.out.println("ConstructorDec:" + $cons_name.text);} 
        LPAR argumentNamedVars? RPAR functionScope
    ;

classProperty
    : varDecStatement
    | method
    ;

method
    : methodDec functionScope
    ;

methodDec
    : DEF (type | VOID) method_name=IDENTIFIER
        {System.out.println("MethodDec:" + $method_name.text);} 
        LPAR argumentNamedVars? RPAR
    ;

argumentNamedVars
    : varDec COMMA argumentNamedVars
    | varDec
    ;

statement
    : scope
    | printStatement SEMICOLON
    | assignStatement SEMICOLON
    | functionCall SEMICOLON 
    | ifStatement
    | forStatement
    | forEachStatement
    | breakStatement SEMICOLON
    | continueStatement SEMICOLON
    | returnStatement SEMICOLON
    | SEMICOLON
    ;

expression
    : assignStatement
    | expressionList
    | exp1OR 
    ;

exp1OR
    : exp2AND nlr_exp1OR
    ;

nlr_exp1OR
    : (logicalOr exp2AND {System.out.println("Operator:" + $logicalOr.text);} nlr_exp1OR)?
    ;

exp2AND
    : exp3eq nlr_exp2AND
    ;

nlr_exp2AND
    : (logicalAnd exp3eq {System.out.println("Operator:" + $logicalAnd.text);} nlr_exp2AND)?
    ;

exp3eq
    : exp4rel nlr_exp3eq 
    ;

nlr_exp3eq
    : (eqOP exp4rel {System.out.println("Operator:" + $eqOP.text);} nlr_exp3eq)?
    ;

exp4rel
    : exp5sum nlr_exp4rel 
    ;

nlr_exp4rel
    : (relOP exp5sum {System.out.println("Operator:" + $relOP.text);} nlr_exp4rel)?
    ;

exp5sum
    : exp6mul nlr_exp5sum 
    ;

nlr_exp5sum
    : (arithBinOP1 exp6mul {System.out.println("Operator:" + $arithBinOP1.text);} nlr_exp5sum)?
    ;

exp6mul
    : exp7pre nlr_exp6mul 
    ;

nlr_exp6mul
    : (arithBinOP0 exp7pre {System.out.println("Operator:" + $arithBinOP0.text);} nlr_exp6mul)?
    ;

exp7pre
    : preUnaryOP exp7pre {System.out.println("Operator:" + $preUnaryOP.text);}
    | exp8post 
    ;

exp8post
    : exp9 nlr_exp8post
    ;

nlr_exp8post
    : (postUnaryOP {System.out.println("Operator:" + $postUnaryOP.text);} nlr_exp8post)?
    ;

exp9
    : exp12Par nlr_exp9
    ;

nlr_exp9
    : ((callArgs | dotOpID | bracketExpression) nlr_exp9)?
    ;

exp12Par
    : LPAR expression RPAR
    | exp13Terminals
    ;

exp13Terminals
    : classInst
    | THIS
    | NUM
    | STR
    | boolValues
    | IDENTIFIER
    ;

breakStatement
    : BREAK
    {System.out.println("Control:break");}
    ; 

continueStatement
    : CONTINUE
    {System.out.println("Control:continue");}
    ; 

classInst
    : NEW IDENTIFIER callArgs
    ;

printStatement
    : {System.out.println("Built-in:print");} 
    PRINT LPAR expression RPAR
    ;

functionCall
    : {System.out.println("MethodCall");} 
    exp9 callArgs
    ;

commaSeperatedExps
    : (expression (COMMA expression)*)?
    ;

callArgs
    : LPAR commaSeperatedExps RPAR
    ;

dotOpID
    : dotOP IDENTIFIER
    ;

bracketExpression
    : LBRACKET expression RBRACKET
    ;

expressionList
    : LBRACKET expression (COMMA expression)* RBRACKET
    ;

assignStatement
    : exp1OR assignOP expression {System.out.println("Operator:" + $assignOP.text);}
    ;

ifStatement
    :   {System.out.println("Conditional:if");} 
    IF LPAR expression RPAR statement elseStatement?
    ;

elseStatement
    : {System.out.println("Conditional:else");}  
    ELSE LBRACE statement* RBRACE
    |{System.out.println("Conditional:else");} ELSE statement // 1-line else
    ;

forStatement
    : {System.out.println("Loop:for");}  
    FOR LPAR assignStatement? SEMICOLON expression? SEMICOLON assignStatement? RPAR statement
    ;

forEachStatement
    : {System.out.println("Loop:foreach");}  
    FOREACH LPAR IDENTIFIER IN expression RPAR statement
    ;

returnStatement
    : {System.out.println("Return");} 
    RETURN expression?
    ;

varDecStatement
    : IDENTIFIER COLON type SEMICOLON
    {System.out.println("VarDec:" + $IDENTIFIER.getText());} 
    ;

varDec
    : IDENTIFIER COLON type
    ;

type
    : INT
    | STRING_K 
    | BOOL
    | list 
    | funcPtr
    | IDENTIFIER // classes
    ;

list 
    : LIST LPAR listDecTypes RPAR
    | LIST LPAR NUM HASH type RPAR
    ;


listDecTypes
    : (type | varDec) COMMA listDecTypes
    | type | varDec
    ;

funcPtr
    : FUNC LT (VOID | typeList) MINUS GT (type | VOID) GT
    ;

typeList
    : type COMMA typeList
    | type
    ;

boolValues
    : TRUE
    | FALSE
    ;

dotOP
    : DOT
    ;

postUnaryOP
    : PLUS PLUS
    | MINUS MINUS
    ;

preUnaryOP
    : MINUS MINUS
    | PLUS PLUS
    | EXCLAIMATION
    | MINUS
    ;

arithBinOP0
    : PERCENT
    | DIVIDE
    | AESTRIK
    ;

arithBinOP1
    : MINUS
    | PLUS
    ;

relOP
    : LT
    | GT
    ;

eqOP
    : EQUAL EQUAL
    | EXCLAIMATION EQUAL
    ;

logicalAnd
    : AND AND
    ;

logicalOr
    : OR OR
    ;

assignOP
    : EQUAL
    ;


 



CLASS: 'class';
EXTENDS: 'extends';
THIS: 'this';
DEF: 'def';
FUNC: 'func';
RETURN: 'return';
IF: 'if';
ELSE: 'else';
FOR: 'for';
FOREACH: 'foreach';
CONTINUE: 'continue';
BREAK: 'break';
FALSE: 'false';
TRUE: 'true';
INT: 'int';
BOOL: 'bool';
STRING_K: 'string';
VOID: 'void';
LIST: 'list';
IN: 'in';
NULL: 'null';
NEW: 'new';
PRINT: 'print';

LBRACE: '{';
RBRACE: '}';
LPAR: '(';
RPAR: ')';
LBRACKET: '[';
RBRACKET: ']';

SEMICOLON: ';';
COLON: ':';
DOT: '.';
COMMA: ',';

HASH: '#';

GT: '>';
LT: '<';
EQUAL: '=';

AESTRIK: '*';
DIVIDE: '/';
PLUS: '+';
MINUS: '-';
PERCENT: '%';

AND: '&';
OR: '|';
EXCLAIMATION: '!';

IDENTIFIER
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;

NUM
    : '0' | [1-9][0-9]*
    ;

COMMENT
    : '//' ~[\n\r]* -> skip
    ;

STR
    : '"' ~('"')* '"'
    ;


WS
    : [ \t\n\r] -> skip
    ;