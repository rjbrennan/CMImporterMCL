import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;

import fileChooser.CustomFileChooser;
import mapstructs.Map;

public class Importer {
	
	private static File output;
	private static File input;

	public static void main(String[] args) throws IOException, InvocationTargetException, InterruptedException {
		
		//Prompts user to choose a folder of .cxl files they would like to combine
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				JFileChooser folder = new JFileChooser();
				folder.setDialogTitle("Select a folder of concept maps");
				folder.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				folder.setAcceptAllFileFilterUsed(false);
				System.out.println("Trying to keep the code running");
				folder.showOpenDialog(null);
				input = folder.getSelectedFile();
			}
		});
		
		
		Map map = new Map(input);
		
		//Prompts user to enter the topic of the maps
		String topic = JOptionPane.showInputDialog("Enter the topic");
		while(!map.inMap(topic)) {	
			topic = JOptionPane.showInputDialog("That topic did not match any concepts, please try again");
		}
		double pGamma = 0;
		while(pGamma<=0) {
			JFrame frame = new JFrame();
			JSlider slider = createSlider(10, 40);
			JPanel sliderPanel = createSliderPanel(slider, "myMessage");
			String title = "myTitle";
			int dialogResponse = JOptionPane.showOptionDialog
		            (frame,                  // I'm within a JFrame here
		             sliderPanel,
		             title,
		             JOptionPane.OK_CANCEL_OPTION,
		             JOptionPane.QUESTION_MESSAGE,
		             null, null, null
		            );
			if (JOptionPane.OK_OPTION == dialogResponse) {
				pGamma = slider.getValue();
			}
			else
				pGamma = 0;
			
			frame.setVisible(false);
			frame.dispose();
			
		}
		
		pGamma = pGamma/10;
		
		// FIXME if you write the name of another file, it just does filename..cxl
		System.out.println("Got this far");
		EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
            	CustomFileChooser j = new CustomFileChooser(".cxl");
        		j.setAcceptAllFileFilterUsed(false);
                j.showSaveDialog(null);
                output = j.getSelectedFile();
            }
        });

		map.execute(output, pGamma);

	}
	
	private static JSlider createSlider(int min, int max) {
		JSlider slider = new JSlider(min, max);
		slider.setMajorTickSpacing(10);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setValue(min);
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put(min, new JLabel("A Couple"));
		labelTable.put(max, new JLabel("A Lot"));
		slider.setLabelTable(labelTable);
		
		return slider;
	}
	
	public static JPanel createSliderPanel(final JSlider slider, String label) {
        final JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new TitledBorder(label));
        p.setPreferredSize(new Dimension(300, 60));
        p.add(slider);
        return p;
    }

}
