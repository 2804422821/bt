package bt.protocol;

public class Port implements Message {

    private int port;

    public Port(int port) throws InvalidMessageException {

        if (port < 0 || port > 65535) {
            throw new InvalidMessageException("Invalid argument: port (" + port + ")");
        }

        this.port = port;
    }

    @Override
    public MessageType getType() {
        return MessageType.PORT;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "[" + getType().name() + "] port {" + port + "}";
    }
}
