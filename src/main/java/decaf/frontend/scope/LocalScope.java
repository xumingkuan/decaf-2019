package decaf.frontend.scope;

import decaf.frontend.tree.Tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Local scope: stores locally-defined variables.
 */
public class LocalScope extends Scope {

    public LocalScope(Scope parent) {
        super(Kind.LOCAL);
        assert parent.isFormalOrLocalOrLambdaScope();
        if (parent.isFormalScope()) {
            ((FormalScope) parent).setNested(this);
        } else if (parent.isLocalScope()) {
            ((LocalScope) parent).nested.add(this);
        } else {
            ((LambdaScope) parent).setNested(this);
        }
        this.parent = parent;
    }

    @Override
    public boolean isLocalScope() {
        return true;
    }

    /**
     * Collect all local scopes and lambda scopes defined inside this scope.
     *
     * @return local scopes and lambda scopes
     */
    public List<Scope> nestedLocalOrLambdaScopes() {
        return nested;
    }

    private List<Scope> nested = new ArrayList<>();

    public final Scope parent;
}
