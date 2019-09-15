package decaf.frontend.tacgen;

import decaf.driver.Config;
import decaf.driver.Phase;
import decaf.lowlevel.tac.*;
import decaf.frontend.tree.Tree;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class TacGen extends Phase<Tree.TopLevel, TAC.Prog> implements TacEmitter {

    public TacGen(Config config) {
        super("tacgen", config);
    }

    @Override
    public TAC.Prog transform(Tree.TopLevel tree) {
        // Create class info.
        var info = new ArrayList<ClassInfo>();
        for (var clazz : tree.classes) {
            info.add(clazz.symbol.getInfo());
        }
        var pw = new ProgramWriter(info);

        // Step 1: create virtual tables.
        pw.visitVTables();

        // Step 2: emit tac instructions for every method.
        for (var clazz : tree.classes) {
            for (var method : clazz.methods()) {
                MethodVisitor mv;
                if (method.symbol.isMain()) {
                    mv = pw.visitMainMethod();
                } else {
                    // Remember calling convention: pass `this` (if non-static) as an extra argument, via reversed temps.
                    var numArgs = method.params.size();
                    var i = 0;
                    if (!method.isStatic()) {
                        numArgs++;
                        i++;
                    }

                    mv = pw.visitMethod(clazz.name, method.name, numArgs);
                    for (var param : method.params) {
                        param.symbol.temp = mv.getArgTemp(i);
                        i++;
                    }
                }

                method.body.accept(this, mv);
                mv.visitEnd();
            }
        }

        return pw.visitEnd();
    }

    @Override
    public void onSucceed(TAC.Prog program) {
        if (config.target.equals(Config.Target.PA3)) {
            // First dump the tac program to file,
            var path = config.dstPath.resolve(config.getSourceBaseName() + ".tac");
            try {
                var printer = new PrintWriter(path.toFile());
                program.printTo(printer);
                printer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // and then execute it using our simulator.
            var simulator = new Simulator(System.in, config.output);
            simulator.execute(program);
        }
    }
}
