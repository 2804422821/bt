package bt.bencoding;

import java.util.ArrayList;
import java.util.List;

class BEListBuilder extends BEPrefixedTypeBuilder<List<Object>> {

    private final List<Object> objects;
    private BEObjectBuilder<?> builder;

    BEListBuilder() {
        objects = new ArrayList<>();
    }

    @Override
    protected boolean doAccept(int b) {

        if (builder == null) {
            BEType type = BEParser.getTypeForPrefix((char) b);
            builder = BEParser.builderForType(type);
        }
        if (!builder.accept(b)) {
            objects.add(builder.build());
            builder = null;
            return accept(b);
        }
        return true;
    }

    @Override
    public boolean acceptEOF() {
        return builder == null;
    }

    @Override
    protected List<Object> doBuild() {
        return objects;
    }

    @Override
    public BEType getType() {
        return BEType.LIST;
    }
}
