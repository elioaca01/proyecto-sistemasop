package avilaos.ui;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
public final class UiTheme {
    public static final Color BACKGROUND=new Color(0xF4F7F9),PAPER=Color.WHITE,INK=new Color(0x183648),TEAL=new Color(0x087F8C),BORDER=new Color(0xD5E1E7),ERROR=new Color(0xA34529);
    private UiTheme(){}
    public static void apply(){
        FontUIResource font=new FontUIResource(Font.DIALOG,Font.PLAIN,13);
        for(String key:new String[]{"Label.font","Button.font","Table.font","TableHeader.font","TextField.font","ComboBox.font","TabbedPane.font","TitledBorder.font","TextArea.font"})UIManager.put(key,font);
        UIManager.put("Panel.background",PAPER);UIManager.put("Label.foreground",INK);UIManager.put("Table.selectionBackground",new Color(0xDCEFF0));UIManager.put("Table.selectionForeground",INK);UIManager.put("Table.gridColor",BORDER);UIManager.put("ProgressBar.foreground",TEAL);UIManager.put("TabbedPane.selected",PAPER);
    }
}
