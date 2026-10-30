package avilaos.tests;
import avilaos.ui.*;
import avilaos.config.SimulationConfig;
import avilaos.process.*;
import avilaos.sync.*;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;
public final class UiFormTest {
    private static void text(JPanel p,String name,String value){SwingTestSupport.find(p,name,JTextField.class).setText(value);}
    public static void run(){SwingTestSupport.edt(()->{
        for(String value:new String[]{"","1.5","-1","2147483648","abc"})throwsType(IllegalArgumentException.class,()->InputParser.integer(value,"RAM",1));
        equal(25,InputParser.integer(" 25 ","RAM",1));equal(0,InputParser.integer("0","prioridad",0));
        throwsType(IllegalArgumentException.class,()->InputParser.longInteger("9223372036854775808","Deadline",1));
        ProcessFormPanel p=new ProcessFormPanel();p.setChoices(SimulationTest.configs(),new BufferDefinition[]{new BufferDefinition(1,1,1)});
        text(p,"process.name","  prueba  ");text(p,"process.instructions","6");text(p,"process.memory","2");text(p,"process.deadline","2147483648");
        JComboBox<?> type=SwingTestSupport.find(p,"process.type",JComboBox.class);
        type.setSelectedItem(ProcessType.PRODUCER);ProcessDefinition producer=p.readDefinition();equal(1,producer.bufferId());equal(0,producer.cpuBurst());equal("prueba",producer.name());
        type.setSelectedItem(ProcessType.CONSUMER);equal(ProcessType.CONSUMER,p.readDefinition().type());
        text(p,"process.interval","invalid");type.setSelectedItem(ProcessType.CPU_BOUND);ProcessDefinition cpu=p.readDefinition();equal(0,cpu.operationInterval());equal(0,cpu.bufferId());equal(2147483648L,cpu.deadlineCycles());
        type.setSelectedItem(ProcessType.IO_BOUND);text(p,"process.cpuBurst","2");text(p,"process.ioCycles","3");equal(2,p.readDefinition().cpuBurst());
        text(p,"process.priority","-1");throwsType(IllegalArgumentException.class,p::readDefinition);text(p,"process.priority","0");
        type.setSelectedItem(ProcessType.PRODUCER);text(p,"process.interval","1");text(p,"process.instructions","5");throwsType(IllegalArgumentException.class,p::readDefinition);
        text(p,"process.instructions","6");p.setChoices(SimulationTest.configs(),new BufferDefinition[0]);throwsType(IllegalArgumentException.class,p::readDefinition);
        BufferFormPanel b=new BufferFormPanel();b.setComputers(SimulationTest.configs());text(b,"buffer.id","4");text(b,"buffer.capacity","1");equal(4,b.readDefinition().id());text(b,"buffer.capacity","1.5");throwsType(IllegalArgumentException.class,b::readDefinition);
        ConfigurationPanel config=new ConfigurationPanel();SimulationConfig original=SimulationConfigTest.empty();config.load(original);text(config,"config.cycleMillis"," 25 ");text(config,"config.latency","0");equal(25L,config.readConfiguration().cycleMillis());equal(500L,original.cycleMillis());
        text(config,"config.computerCount","1");throwsType(IllegalArgumentException.class,config::readConfiguration);text(config,"config.computerCount","3");equal(3,config.readConfiguration().computerCount());
        config.setEditable(false);isTrue(!SwingTestSupport.find(config,"config.apply",JButton.class).isEnabled());
    });}
}
