package avilaos.sync;
import avilaos.hardware.Ram;
import avilaos.structures.LinkedList;

/** Registro propio; su propietario BufferService protege las mutaciones compartidas. */
public final class BufferRegistry {
    private record Host(int id, Ram ram) { }
    private record Entry(BoundedBuffer buffer, Ram.Reservation reservation) { }
    private final LinkedList<Host> hosts = new LinkedList<>();
    private final LinkedList<Entry> buffers = new LinkedList<>();
    public void registerRam(int hostId, Ram ram) {
        if (hostId <= 0 || ram == null) { throw new IllegalArgumentException("Anfitrión/RAM inválido"); }
        for (int i = 0; i < hosts.size(); i++) { if (hosts.get(i).id() == hostId) { throw new IllegalArgumentException("Anfitrión duplicado"); } }
        hosts.addLast(new Host(hostId, ram));
    }
    public BoundedBuffer create(BufferDefinition definition) {
        if (definition == null) { throw new IllegalArgumentException("Definición requerida"); }
        for (int i = 0; i < buffers.size(); i++) {
            if (buffers.get(i).buffer().definition().id() == definition.id()) { throw new IllegalArgumentException("ID de buffer duplicado"); }
        }
        Ram ram = null;
        for (int i = 0; i < hosts.size(); i++) { if (hosts.get(i).id() == definition.hostId()) { ram = hosts.get(i).ram(); } }
        if (ram == null) { throw new IllegalArgumentException("Anfitrión inexistente"); }
        // Rechazar antes de crear el arreglo físico; una capacidad imposible no debe agotar la JVM.
        if (definition.capacity() > ram.free()) { throw new IllegalArgumentException("RAM insuficiente para el buffer"); }
        BoundedBuffer buffer = new BoundedBuffer(definition);
        Ram.Reservation reservation = ram.reserve(definition.capacity());
        if (reservation == null) { throw new IllegalArgumentException("RAM insuficiente para el buffer"); }
        buffers.addLast(new Entry(buffer, reservation)); return buffer;
    }
    public BoundedBuffer get(int id) {
        for (int i = 0; i < buffers.size(); i++) { if (getAt(i).definition().id() == id) { return getAt(i); } }
        throw new IllegalArgumentException("Buffer inexistente: " + id);
    }
    public int size() { return buffers.size(); }
    public BoundedBuffer getAt(int index) { return buffers.get(index).buffer(); }
}
