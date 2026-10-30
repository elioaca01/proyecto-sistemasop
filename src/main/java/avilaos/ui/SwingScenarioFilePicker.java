package avilaos.ui;

import java.awt.Component;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class SwingScenarioFilePicker implements ScenarioFilePicker {
    private JFileChooser chooser(String title){
        JFileChooser chooser=new JFileChooser(System.getProperty("user.dir"));chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Escenarios JSON (*.json)","json"));chooser.setAcceptAllFileFilterUsed(false);return chooser;
    }
    public Path chooseOpen(Component parent){
        JFileChooser chooser=chooser("Cargar escenario · vuelve al ciclo 0");
        return chooser.showOpenDialog(parent)==JFileChooser.APPROVE_OPTION?chooser.getSelectedFile().toPath():null;
    }
    public Path chooseSave(Component parent){
        JFileChooser chooser=chooser("Guardar escenario inicial aplicado");
        while(chooser.showSaveDialog(parent)==JFileChooser.APPROVE_OPTION){
            Path path=chooser.getSelectedFile().toPath();String name=path.getFileName().toString();
            if(!name.regionMatches(true,name.length()-5,".json",0,5))path=path.resolveSibling(name+".json");
            if(Files.exists(path)){
                int answer=JOptionPane.showConfirmDialog(parent,"¿Reemplazar el archivo existente?\n"+path,"Guardar escenario",JOptionPane.YES_NO_CANCEL_OPTION);
                if(answer==JOptionPane.CANCEL_OPTION||answer==JOptionPane.CLOSED_OPTION)return null;
                if(answer!=JOptionPane.YES_OPTION)continue;
            }
            return path;
        }
        return null;
    }
}
