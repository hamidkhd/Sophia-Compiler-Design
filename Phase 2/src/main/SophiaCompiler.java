package main;

import main.ast.nodes.Program;
import main.symbolTable.SymbolTable;
import main.visitor.ASTTreePrinter;
import main.visitor.SymbolTablePass1;
import main.visitor.SymbolTablePass2;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import parsers.SophiaLexer;
import parsers.SophiaParser;

public class SophiaCompiler {
    public void compile(CharStream textStream) {
        SophiaLexer sophiaLexer = new SophiaLexer(textStream);
        CommonTokenStream tokenStream = new CommonTokenStream(sophiaLexer);
        SophiaParser sophiaParser = new SophiaParser(tokenStream);
        Program program = sophiaParser.sophia().sophiaProgram;

        SophiaErrors errors = new SophiaErrors();
        program.accept(new SymbolTablePass1());
        program.accept(new SymbolTablePass2());

        if (!SophiaErrors.hasErrors())
            program.accept(new ASTTreePrinter());
        else
            errors.printErrors();

    }

}
