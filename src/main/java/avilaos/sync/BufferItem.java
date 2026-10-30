package avilaos.sync;
public record BufferItem(long id, long producerId, int producerComputerId) {
    public BufferItem { if (id <= 0 || producerId <= 0 || producerComputerId <= 0) { throw new IllegalArgumentException("Token inválido"); } }
}
