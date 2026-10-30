package avilaos.sync;
public record BufferDefinition(int id, int hostId, int capacity) {
    public BufferDefinition { if (id <= 0 || hostId <= 0 || capacity <= 0) { throw new IllegalArgumentException("Buffer inválido"); } }
}
