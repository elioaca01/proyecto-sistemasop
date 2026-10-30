package avilaos.tests;

import avilaos.process.ProcessDefinition;
import avilaos.process.ProcessType;

public final class ProcessDefinitionTest {
    private ProcessDefinitionTest() { }

    public static void run() {
        acceptsAndNormalizesValidDefinitions();
        rejectsInvalidCommonFields();
        rejectsFieldsIncompatibleWithType();
        enforcesBufferInstructionBudgetWithoutOverflow();
    }

    private static ProcessDefinition cpu(String name, int computer, int instructions,
                                         int memory, int priority, long deadline) {
        return new ProcessDefinition(name, computer, ProcessType.CPU_BOUND,
                instructions, memory, priority, deadline, 0, 0, 0, 0, 0);
    }

    private static ProcessDefinition buffer(ProcessType type, int instructions,
                                            int id, int interval, int target) {
        return new ProcessDefinition("P1", 1, type, instructions, 8, 0, 100,
                0, 0, id, interval, target);
    }

    private static void acceptsAndNormalizesValidDefinitions() {
        TestSupport.equal("P1", cpu("  P1  ", 1, 2, 8, 0, 10).name());
        ProcessDefinition io = new ProcessDefinition("IO", 2, ProcessType.IO_BOUND,
                20, 8, 1, 100, 3, 2, 0, 0, 0);
        TestSupport.equal(ProcessType.IO_BOUND, io.type());
        TestSupport.equal("P1", buffer(ProcessType.PRODUCER, 21, 1, 2, 3).name());
        TestSupport.equal(ProcessType.CONSUMER,
                buffer(ProcessType.CONSUMER, 22, 1, 2, 3).type());
    }

    private static void rejectsInvalidCommonFields() {
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu(null, 1, 2, 8, 0, 10));
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu(" \t ", 1, 2, 8, 0, 10));
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu("P", 0, 2, 8, 0, 10));
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu("P", 1, 0, 8, 0, 10));
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu("P", 1, 2, -1, 0, 10));
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu("P", 1, 2, 8, -1, 10));
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu("P", 1, 2, 8, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () ->
                new ProcessDefinition("P", 1, null, 2, 8, 0, 10, 0, 0, 0, 0, 0));
    }

    private static void rejectsFieldsIncompatibleWithType() {
        TestSupport.throwsType(IllegalArgumentException.class, () ->
                new ProcessDefinition("P", 1, ProcessType.CPU_BOUND, 20, 8, 0, 100, 1, 0, 0, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () ->
                new ProcessDefinition("P", 1, ProcessType.CPU_BOUND, 20, 8, 0, 100, 0, 0, 1, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () ->
                new ProcessDefinition("P", 1, ProcessType.IO_BOUND, 20, 8, 0, 100, 0, 2, 0, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () ->
                new ProcessDefinition("P", 1, ProcessType.IO_BOUND, 20, 8, 0, 100, 2, 0, 0, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () ->
                new ProcessDefinition("P", 1, ProcessType.IO_BOUND, 20, 8, 0, 100, 2, 2, 1, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () ->
                new ProcessDefinition("P", 1, ProcessType.PRODUCER, 20, 8, 0, 100, 1, 0, 1, 1, 1));
        for (ProcessType type : new ProcessType[]{ProcessType.PRODUCER, ProcessType.CONSUMER}) {
            TestSupport.throwsType(IllegalArgumentException.class, () -> buffer(type, 21, 0, 2, 3));
            TestSupport.throwsType(IllegalArgumentException.class, () -> buffer(type, 21, 1, 0, 3));
            TestSupport.throwsType(IllegalArgumentException.class, () -> buffer(type, 21, 1, 2, 0));
        }
    }

    private static void enforcesBufferInstructionBudgetWithoutOverflow() {
        for (ProcessType type : new ProcessType[]{ProcessType.PRODUCER, ProcessType.CONSUMER}) {
            TestSupport.equal(21, buffer(type, 21, 1, 2, 3).instructionCount());
            TestSupport.throwsType(IllegalArgumentException.class, () -> buffer(type, 20, 1, 2, 3));
            TestSupport.throwsType(IllegalArgumentException.class, () ->
                    buffer(type, Integer.MAX_VALUE, 1, Integer.MAX_VALUE, Integer.MAX_VALUE));
            TestSupport.throwsType(IllegalArgumentException.class, () ->
                    buffer(type, Integer.MAX_VALUE, 1, Integer.MAX_VALUE, 1));
        }
    }
}
