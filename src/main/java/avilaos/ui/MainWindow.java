package avilaos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Ventana inicial. Las decisiones de la simulación pertenecerán al núcleo. */
public final class MainWindow extends JFrame {
    public MainWindow() {
        super("ÁvilaOS");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(new Color(244, 247, 249));
        content.setBorder(BorderFactory.createEmptyBorder(40, 32, 40, 32));
        JLabel title = new JLabel("ÁvilaOS", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 40f));
        title.setForeground(new Color(24, 54, 72));
        JLabel description = new JLabel("Simulador de sistemas operativos distribuidos", SwingConstants.CENTER);
        description.setFont(description.getFont().deriveFont(16f));
        description.setForeground(new Color(46, 73, 89));
        JLabel course = new JLabel("Proyecto 1 · Sistemas Operativos", SwingConstants.CENTER);
        course.setForeground(new Color(85, 108, 120));
        content.add(title, BorderLayout.NORTH);
        content.add(description, BorderLayout.CENTER);
        content.add(course, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(640, 320);
        setMinimumSize(new java.awt.Dimension(520, 280));
        setLocationRelativeTo(null);
    }
}
