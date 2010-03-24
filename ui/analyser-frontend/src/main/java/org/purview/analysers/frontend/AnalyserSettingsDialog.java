package org.purview.analysers.frontend;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.NbBundle;
import org.purview.core.analysis.Analyser;
import org.purview.core.analysis.Metadata;
import org.purview.core.analysis.Settings;
import org.purview.core.analysis.settings.FloatRangeSetting;
import org.purview.core.analysis.settings.IntRangeSetting;
import org.purview.core.analysis.settings.Setting;
import org.purview.core.data.ImageMatrix;
import scala.collection.Iterator;
import scala.collection.Seq;

public class AnalyserSettingsDialog extends JDialog implements ActionListener, ItemListener {

    private final Map<Analyser<ImageMatrix>, Boolean> analysers;
    private final Map<JCheckBox, Analyser<ImageMatrix>> callbacks;

    /** Creates new form AnalyserSettingsDialog */
    public AnalyserSettingsDialog(final Map<Analyser<ImageMatrix>, Boolean> analysers,
            final Frame parent, final boolean modal) {
        super(parent, modal);

        this.analysers = analysers;

        initComponents();

        okButton.addActionListener(this);

        activeAnalysersTab.setLayout(new BoxLayout(activeAnalysersTab, BoxLayout.PAGE_AXIS));
        this.callbacks = new HashMap<JCheckBox, Analyser<ImageMatrix>>();
        int unknownIndex = 0;
        for (Analyser<ImageMatrix> analyser : analysers.keySet()) {
            JCheckBox box = new JCheckBox();
            box.setSelected(analysers.get(analyser));
            callbacks.put(box, analyser);
            box.addItemListener(this);
            if (analyser instanceof Metadata) {
                box.setText(((Metadata) analyser).name());
            } else {
                box.setText(NbBundle.getMessage(AnalyserSettingsDialog.class,
                        "LBL_UnknownAnalyser", ++unknownIndex));
            }
            activeAnalysersTab.add(box);
        }
        updateTabs();
    }

    private void updateTabs() {
        analyserTabs.removeAll();
        analyserTabs.add(org.openide.util.NbBundle.getMessage(AnalyserSettingsDialog.class, "LBL_ActiveAnalysers"), activeAnalysersTab);
        for (final Analyser<ImageMatrix> analyser : analysers.keySet()) {
            if (analysers.get(analyser) && analyser instanceof Settings) {
                final Settings settingsForAnalyser = (Settings) analyser;
                final SettingsPanel panel = new SettingsPanel(settingsForAnalyser);
                String tabName = (analyser instanceof Metadata)
                        ? NbBundle.getMessage(AnalyserSettingsDialog.class, "LBL_SettingsFor", ((Metadata) analyser).name())
                        : NbBundle.getMessage(AnalyserSettingsDialog.class, "LBL_SettingsFor", "?");
                analyserTabs.add(tabName, panel);
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        analyserTabs = new javax.swing.JTabbedPane();
        activeAnalysersTab = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okButton.setText(org.openide.util.NbBundle.getMessage(AnalyserSettingsDialog.class, "LBL_OK")); // NOI18N
        buttonPanel.add(okButton);

        getContentPane().add(buttonPanel, java.awt.BorderLayout.PAGE_END);

        javax.swing.GroupLayout activeAnalysersTabLayout = new javax.swing.GroupLayout(activeAnalysersTab);
        activeAnalysersTab.setLayout(activeAnalysersTabLayout);
        activeAnalysersTabLayout.setHorizontalGroup(
            activeAnalysersTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 395, Short.MAX_VALUE)
        );
        activeAnalysersTabLayout.setVerticalGroup(
            activeAnalysersTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 148, Short.MAX_VALUE)
        );

        analyserTabs.addTab(org.openide.util.NbBundle.getMessage(AnalyserSettingsDialog.class, "LBL_ActiveAnalysers"), activeAnalysersTab); // NOI18N

        getContentPane().add(analyserTabs, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel activeAnalysersTab;
    private javax.swing.JTabbedPane analyserTabs;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == okButton) {
            this.setVisible(false);
        }
    }

    public void itemStateChanged(final ItemEvent e) {
        if (e.getSource() instanceof JCheckBox && callbacks.containsKey((JCheckBox) e.getSource())) {
            final JCheckBox box = (JCheckBox) e.getSource();
            analysers.put(callbacks.get(box), box.isSelected());
            updateTabs();
        }
    }
}

class SettingsPanel extends JPanel implements ChangeListener {

    private final Map<Object, Setting> settingCallbacks;

    public SettingsPanel(final Settings set) {
        settingCallbacks = new HashMap<Object, Setting>();

        /*
         * Scala:
         * for(s <- set.settings) {
         *   settingsGrid.add(new JLabel(s.name));
         *   s match {
         *     case setting: IntRangeSetting =>
         *       val slider = new JSlider
         *       slider.setMinimum(setting.min)
         *       slider.setMaximum(setting.max)
         *       slider.setValue(setting.defaultVal getOrElse setting.min)
         *       //...
         *     case setting: FloatRangeSetting =>
         *       //...
         *   }
         * }
         */

        final Seq<Setting<?>> settingFields = set.settings();
        final Iterator<Setting<?>> settingFieldIter = settingFields.iterator();
        this.setLayout(new SpringLayout());

        while (settingFieldIter.hasNext()) {
            Setting<?> s = settingFieldIter.next();
            JLabel l = new JLabel(s.name(), JLabel.TRAILING);
            this.add(l);
            if (s instanceof IntRangeSetting) {
                IntRangeSetting setting = (IntRangeSetting) s;
                JSpinner spinner = new JSpinner();
                spinner.setModel(new SpinnerNumberModel(setting.value(),
                        setting.min(), setting.max(), 1));

                l.setLabelFor(spinner);
                this.add(spinner);
                spinner.addChangeListener(this);
                settingCallbacks.put(spinner, setting);
            } else if (s instanceof FloatRangeSetting) {
                FloatRangeSetting setting = (FloatRangeSetting) s;
                JSpinner spinner = new JSpinner();
                spinner.setModel(new SpinnerNumberModel(Float.valueOf(setting.value()),
                        Float.valueOf(setting.min()), Float.valueOf(setting.max()),
                        Float.valueOf(1f / setting.granularity())));

                l.setLabelFor(spinner);
                this.add(spinner);
                spinner.addChangeListener(this);
                settingCallbacks.put(spinner, setting);
            } else {
                this.add(new JLabel("(" + NbBundle.getMessage(SettingsPanel.class, "LBL_SettingNotSupported") + ")"));
            }
        }
        SpringUtilities.makeCompactGrid(this, settingFields.length(), 2, 5, 5, 5, 5);
    }

    public void stateChanged(ChangeEvent e) {
        if (settingCallbacks.containsKey(e.getSource())) {
            Setting s = settingCallbacks.get(e.getSource());

            if (s instanceof IntRangeSetting) {
                JSpinner spinner = (JSpinner) e.getSource();
                ((IntRangeSetting) s).value_$eq(((Integer) spinner.getValue()).intValue());
            } else if (s instanceof FloatRangeSetting) {
                JSpinner spinner = (JSpinner) e.getSource();
                ((FloatRangeSetting) s).value_$eq(((Float) spinner.getValue()).floatValue());
            }
        }
    }
}
